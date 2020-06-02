// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.alt.plugins.bitcoin

import io.ktor.http.ContentType
import org.bouncycastle.util.Arrays
import org.veriblock.alt.plugins.HttpSecurityInheritingChain
import org.veriblock.alt.plugins.createHttpClient
import org.veriblock.alt.plugins.rpcRequest
import org.veriblock.alt.plugins.util.RpcException
import org.veriblock.core.altchain.AltchainPoPEndorsement
import org.veriblock.core.contracts.BlockEndorsement
import org.veriblock.core.crypto.Crypto
import org.veriblock.core.utilities.createLogger
import org.veriblock.core.utilities.extensions.asHexBytes
import org.veriblock.core.utilities.extensions.flip
import org.veriblock.core.utilities.extensions.isHex
import org.veriblock.core.utilities.extensions.toHex
import org.veriblock.sdk.alt.ApmInstruction
import org.veriblock.sdk.alt.model.SecurityInheritingBlock
import org.veriblock.sdk.alt.model.SecurityInheritingTransaction
import org.veriblock.sdk.alt.model.SecurityInheritingTransactionVout
import org.veriblock.sdk.alt.plugin.PluginConfig
import org.veriblock.sdk.alt.plugin.PluginSpec
import org.veriblock.sdk.models.AltPublication
import org.veriblock.sdk.models.PublicationData
import org.veriblock.sdk.models.SyncStatus
import org.veriblock.sdk.models.VeriBlockPublication
import org.veriblock.sdk.services.SerializeDeserializeService
import java.io.File
import java.nio.ByteBuffer
import kotlin.math.abs
import kotlin.math.roundToLong

private val logger = createLogger {}

@PluginSpec(name = "BitcoinFamily", key = "btc")
class BitcoinFamilyChain(
    override val key: String,
    configuration: PluginConfig
) : HttpSecurityInheritingChain {

    override val config = BitcoinConfig(configuration)

    override val id: Long = configuration.id
        ?: error("Failed to load altchain plugin $key: please configure the chain 'id'!")

    override val name: String = configuration.name
        ?: error("Failed to load altchain plugin $key: please configure the chain 'name'!")

    private val payoutAddressScript: ByteArray

    override val httpClient = createHttpClient(
        authConfig = config.auth,
        contentTypes = listOf(ContentType.Application.Json, ContentType.Text.Any),
        connectionTimeout = config.daemonConnectionTimeout
    )

    override val requestLogsPath: String?
        get() = config.requestLogsPath

    init {
        config.checkValidity()
        checkNotNull(config.payoutAddress) {
            "$name's payoutAddress ($key.payoutAddress) must be configured!"
        }
        payoutAddressScript = if (config.payoutAddress.isHex()) {
            config.payoutAddress.asHexBytes()
        } else {
            try {
                SegwitAddressUtility.generatePayoutScriptFromSegwitAddress(config.payoutAddress)
            } catch (e: Exception) {
                error("Invalid segwit address: ${e.message}")
            }
        }

        if (requestLogsPath != null) {
            File(requestLogsPath).mkdirs()
        }
    }

    override suspend fun getBestBlockHeight(): Int {
        logger.debug { "Retrieving best block height..." }
        return rpcRequest("getblockcount")
    }

    override suspend fun getBlock(hash: String): SecurityInheritingBlock? {
        logger.debug { "Retrieving block $hash..." }
        val btcBlock: BtcBlock = try {
            rpcRequest("getblock", listOf(hash, 1))
        } catch (e: RpcException) {
            if (e.errorCode == -5) {
                // Block not found
                return null
            } else {
                throw e
            }
        }
        return SecurityInheritingBlock(
            btcBlock.hash,
            btcBlock.height,
            btcBlock.previousblockhash ?: "0000000000000000000000000000000000000000000000000000000000000000",
            btcBlock.confirmations,
            btcBlock.version,
            btcBlock.nonce,
            btcBlock.merkleroot,
            btcBlock.difficulty,
            btcBlock.tx[0],
            btcBlock.tx.drop(1)
        )
    }

    private suspend fun getBlockHash(height: Int): String? {
        logger.debug { "Retrieving block hash @$height..." }
        return try {
            rpcRequest("getblockhash", listOf(height))
        } catch (e: RpcException) {
            if (e.errorCode == -8) {
                // Block height out of range
                return null
            } else {
                throw e
            }
        }
    }

    override suspend fun getBlock(height: Int): SecurityInheritingBlock? {
        logger.debug { "Retrieving block @$height..." }
        val blockHash = getBlockHash(height)
            ?: return null
        return getBlock(blockHash)
    }

    override suspend fun checkBlockIsOnMainChain(height: Int, blockHeaderToCheck: ByteArray): Boolean {
        logger.debug { "Checking block @$height has header ${blockHeaderToCheck.toHex()}..." }
        val blockHash = getBlockHash(height)
            ?: return false
        // Get raw block
        val rawBlock: String = try {
            rpcRequest("getblock", listOf(blockHash, 0))
        } catch (e: RpcException) {
            if (e.errorCode == -5) {
                // Block not found
                return false
            } else {
                throw e
            }
        }
        // Extract header
        val header: ByteArray = Arrays.copyOf(rawBlock.asHexBytes(), blockHeaderToCheck.size)
        // Check header
        return header.contentEquals(blockHeaderToCheck)
    }

    override suspend fun getTransaction(txId: String): SecurityInheritingTransaction? {
        logger.debug { "Retrieving transaction $txId..." }
        val btcTransaction: BtcTransaction = try {
            rpcRequest("getrawtransaction", listOf(txId, 1))
        } catch (e: RpcException) {
            if (e.errorCode == -5) {
                // Transaction not found
                return null
            } else {
                throw e
            }
        }
        return SecurityInheritingTransaction(
            btcTransaction.txid,
            btcTransaction.confirmations,
            btcTransaction.vout.map {
                SecurityInheritingTransactionVout(
                    (it.value * 100000000).roundToLong(),
                    it.scriptPubKey.hex
                )
            }
        )
    }

    override fun getPayoutInterval(): Int {
        return 501
    }

    override suspend fun getMiningInstruction(blockHeight: Int?): ApmInstruction {
        val actualBlockHeight = blockHeight
        // Retrieve top block height from API if not supplied
            ?: getBestBlockHeight()

        logger.info { "Retrieving mining instruction at height $actualBlockHeight from $name daemon at ${config.host}..." }
        val response: BtcPublicationData = rpcRequest("getpopdata", listOf(actualBlockHeight))

        val publicationData = PublicationData(
            id,
            response.block_header.asHexBytes(),
            payoutAddressScript,
            response.raw_contextinfocontainer.asHexBytes()
        )
        if (response.last_known_veriblock_blocks.isEmpty()) {
            error("Publication data's context (last known VeriBlock blocks) must not be empty!")
        }
        return ApmInstruction(
            actualBlockHeight,
            publicationData,
            response.last_known_veriblock_blocks.map { it.asHexBytes() },
            response.last_known_bitcoin_blocks.map { it.asHexBytes() }
        )
    }

    override suspend fun submit(proofOfProof: AltPublication, veriBlockPublications: List<VeriBlockPublication>): String {
        logger.info { "Submitting PoP and VeriBlock publications to $name daemon at ${config.host}..." }
        return rpcRequest("submitpop", listOf(
            SerializeDeserializeService.serialize(proofOfProof).toHex(),
            veriBlockPublications.map { SerializeDeserializeService.serialize(it).toHex() }
        ))
    }

    override fun extractAddressDisplay(addressData: ByteArray): String {
        TODO("Not yet implemented")
    }

    private val crypto = Crypto()

    override fun extractBlockEndorsement(altchainPopEndorsement: AltchainPoPEndorsement): BlockEndorsement {
        val contextBuffer = ByteBuffer.wrap(altchainPopEndorsement.getContextInfo())
        val height = contextBuffer.getInt()
        val hash = crypto.SHA256D(altchainPopEndorsement.getHeader()).flip()
        val previousHash = altchainPopEndorsement.getHeader().copyOfRange(4, 36).flip()
        val previousKeystone = contextBuffer.getBytes(32).flip()
        val secondPreviousKeystone = contextBuffer.getBytes(32).flip()

        return BlockEndorsement(height, hash, previousHash, previousKeystone, secondPreviousKeystone)
    }

    override suspend fun isConnected(): Boolean {
        return try {
            rpcRequest<String>("getblockcount")
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun getSynchronizedStatus(): SyncStatus {
        return try {
            val response: BtcSyncStatus = rpcRequest("getblockchaininfo")
            val blockDifference = abs(response.headers - response.blocks)
            SyncStatus(
                response.headers,
                response.blocks,
                blockDifference,
                response.headers > 0 && blockDifference < 4
            )
        } catch (e: Exception) {
            logger.warn { "Unable to perform the getblockchaininfo rpc call to ${config.host} (is it reachable?)" }
            SyncStatus(0, 0, 0, false)
        }
    }
}

fun ByteBuffer.getBytes(count: Int): ByteArray {
    val result = ByteArray(count)
    get(result)
    return result
}

private data class BtcPublicationData(
    val block_header: String,
    val raw_contextinfocontainer: String,
    val last_known_veriblock_blocks: List<String>,
    val last_known_bitcoin_blocks: List<String>,
    val first_address: String? = null
)

private data class BtcBlock(
    val hash: String,
    val height: Int,
    val confirmations: Int,
    val version: Short,
    val nonce: Int,
    val merkleroot: String,
    val difficulty: Double,
    val tx: List<String>,
    val previousblockhash: String?
)

private data class BtcTransaction(
    val txid: String,
    val confirmations: Int,
    val vout: List<BtcTransactionVout>
)

private data class BtcTransactionVout(
    val value: Double,
    val scriptPubKey: BtcScriptPubKey
)

private data class BtcScriptPubKey(
    val asm: String,
    val hex: String,
    val reqSigs: Int,
    val type: String
)

private data class BtcSyncStatus(
    val blocks: Int,
    val headers: Int
)
