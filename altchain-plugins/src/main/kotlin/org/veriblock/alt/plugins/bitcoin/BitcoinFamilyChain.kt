// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.alt.plugins.bitcoin

import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.extensions.authentication
import com.github.kittinunf.fuel.httpPost
import org.bouncycastle.util.Arrays
import org.veriblock.alt.plugins.util.JsonRpcRequestBody
import org.veriblock.alt.plugins.util.RpcException
import org.veriblock.alt.plugins.util.rpcResponse
import org.veriblock.alt.plugins.util.toJson
import org.veriblock.core.altchain.AltchainPoPEndorsement
import org.veriblock.core.contracts.BlockEndorsement
import org.veriblock.core.crypto.Crypto
import org.veriblock.core.utilities.createLogger
import org.veriblock.core.utilities.extensions.asHexBytes
import org.veriblock.core.utilities.extensions.isHex
import org.veriblock.core.utilities.extensions.toHex
import org.veriblock.sdk.alt.MiningInstruction
import org.veriblock.sdk.alt.SecurityInheritingChain
import org.veriblock.sdk.alt.model.SecurityInheritingBlock
import org.veriblock.sdk.alt.model.SecurityInheritingTransaction
import org.veriblock.sdk.alt.model.SecurityInheritingTransactionVout
import org.veriblock.sdk.alt.plugin.PluginConfig
import org.veriblock.sdk.alt.plugin.PluginSpec
import org.veriblock.sdk.models.AltPublication
import org.veriblock.sdk.models.PublicationData
import org.veriblock.sdk.models.VeriBlockPublication
import org.veriblock.sdk.services.SerializeDeserializeService
import java.nio.ByteBuffer

private val logger = createLogger {}

@PluginSpec(name = "BitcoinFamily", key = "btc")
class BitcoinFamilyChain(
    override val key: String,
    configuration: PluginConfig
) : SecurityInheritingChain {

    override val config = BitcoinConfig(configuration)

    override val id: Long = configuration.id
        ?: error("Failed to load altchain plugin $key: please configure the chain 'id'!")

    override val name: String = configuration.name
        ?: error("Failed to load altchain plugin $key: please configure the chain 'name'!")


    private val payoutAddressScript: ByteArray

    init {
        config.checkValidity()
        checkNotNull(config.payoutAddress) {
            "$key's payoutAddress must be configured!"
        }
        payoutAddressScript = when {
            config.payoutAddress.isHex() ->
                config.payoutAddress.asHexBytes()
            else -> {
                try {
                    SegwitAddressUtility.generatePayoutScriptFromSegwitAddress(config.payoutAddress)
                } catch (e: Exception) {
                    error("Invalid segwit address: ${e.message}")
                }
            }
        }
    }

    private fun Request.authenticate() = if (config.username != null && config.password != null) {
        authentication().basic(config.username, config.password)
    } else {
        this
    }

    override fun getBestBlockHeight(): Int {
        logger.debug { "Retrieving best block height..." }
        val jsonBody = JsonRpcRequestBody("getblockcount").toJson()
        return config.host.httpPost()
            .authenticate()
            .body(jsonBody)
            .rpcResponse()
    }

    override fun getBlock(hash: String): SecurityInheritingBlock? {
        logger.debug { "Retrieving block $hash..." }
        val jsonBody = JsonRpcRequestBody("getblock", listOf(hash, 1)).toJson()
        val btcBlock: BtcBlock = try {
            config.host.httpPost()
                .authenticate()
                .body(jsonBody)
                .rpcResponse()
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

    private fun getBlockHash(height: Int): String? {
        logger.debug { "Retrieving block hash @$height..." }
        val jsonBody = JsonRpcRequestBody("getblockhash", listOf(height)).toJson()
        return try {
            config.host.httpPost()
                .authenticate()
                .body(jsonBody)
                .rpcResponse()
        } catch (e: RpcException) {
            if (e.errorCode == -8) {
                // Block height out of range
                return null
            } else {
                throw e
            }
        }
    }

    override fun getBlock(height: Int): SecurityInheritingBlock? {
        logger.debug { "Retrieving block @$height..." }
        val blockHash = getBlockHash(height)
            ?: return null
        return getBlock(blockHash)
    }

    override fun checkBlockIsOnMainChain(height: Int, blockHeaderToCheck: ByteArray): Boolean {
        logger.debug { "Checking block @$height has header ${blockHeaderToCheck.toHex()}..." }
        val blockHash = getBlockHash(height)
            ?: return false
        // Get raw block
        val jsonBody = JsonRpcRequestBody("getblock", listOf(blockHash, 0)).toJson()
        val rawBlock: String = try {
            config.host.httpPost()
                .authenticate()
                .body(jsonBody)
                .rpcResponse()
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

    override fun getTransaction(txId: String): SecurityInheritingTransaction? {
        logger.debug { "Retrieving transaction $txId..." }
        val jsonBody = JsonRpcRequestBody("getrawtransaction", listOf(txId, 1)).toJson()
        val btcTransaction: BtcTransaction = try {
            config.host.httpPost()
                .authenticate()
                .body(jsonBody)
                .rpcResponse()
        } catch (e: RpcException) {
            if (e.errorCode == -5) {
                // Block not found
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
                    it.value,
                    it.scriptPubKey.hex
                )
            }
        )
    }

    override fun getMiningInstruction(blockHeight: Int?): MiningInstruction {
        val actualBlockHeight = blockHeight
        // Retrieve top block height from API if not supplied
            ?: getBestBlockHeight()

        logger.info { "Retrieving mining instruction at height $actualBlockHeight from $key daemon at ${config.host}..." }
        val jsonBody = JsonRpcRequestBody("getpopdata", listOf(actualBlockHeight)).toJson()
        val response: BtcPublicationData = config.host.httpPost()
            .authenticate()
            .body(jsonBody)
            .rpcResponse()

        val publicationData = PublicationData(
            id,
            response.block_header.asHexBytes(),
            payoutAddressScript,
            response.raw_contextinfocontainer.asHexBytes()
        )
        if (response.last_known_veriblock_blocks.isEmpty()) {
            error("Publication data's context (last known VeriBlock blocks) must not be empty!")
        }
        return MiningInstruction(
            actualBlockHeight,
            publicationData,
            response.last_known_veriblock_blocks.map { it.asHexBytes() },
            response.last_known_bitcoin_blocks.map { it.asHexBytes() }
        )
    }

    override fun submit(proofOfProof: AltPublication, veriBlockPublications: List<VeriBlockPublication>): String {
        logger.info { "Submitting PoP and VeriBlock publications to $key daemon at ${config.host}..." }
        val jsonBody = JsonRpcRequestBody("submitpop", listOf(
            SerializeDeserializeService.serialize(proofOfProof).toHex(),
            veriBlockPublications.map { SerializeDeserializeService.serialize(it).toHex() }
        )).toJson()

        return config.host.httpPost()
            .authenticate()
            .body(jsonBody)
            .rpcResponse()
    }

    override fun updateContext(veriBlockPublications: List<VeriBlockPublication>): String {
        logger.info { "Submitting PoP and VeriBlock publications to $key daemon at ${config.host}..." }
        val jsonBody = JsonRpcRequestBody("updatecontext", listOf(
            veriBlockPublications.map {
                it.transaction
            }.flatMap {
                it.blocks
            }.map {
                SerializeDeserializeService.getHeaderBytesBitcoinBlock(it).toHex()
            },
            veriBlockPublications.flatMap {
                it.blocks
            }.map {
                SerializeDeserializeService.serializeHeaders(it).toHex()
            }
        )).toJson()

        return config.host.httpPost()
            .authenticate()
            .body(jsonBody)
            .rpcResponse()
    }

    private val crypto = Crypto()

    override fun extractBlockEndorsement(altchainPopEndorsement: AltchainPoPEndorsement): BlockEndorsement {
        val contextBuffer = ByteBuffer.wrap(altchainPopEndorsement.getContextInfo())
        val height = contextBuffer.getInt()
        val hash = crypto.SHA256D(altchainPopEndorsement.getHeader()).reversed().toByteArray()
        val previousHash = altchainPopEndorsement.getHeader().copyOfRange(4, 36).reversed().toByteArray()
        val previousKeystone = contextBuffer.getBytes(32).reversed().toByteArray()
        val secondPreviousKeystone = contextBuffer.getBytes(32).reversed().toByteArray()

        return BlockEndorsement(height, hash, previousHash, previousKeystone, secondPreviousKeystone)
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
