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
import org.veriblock.alt.plugins.util.createLoggerFor
import org.veriblock.core.altchain.AltchainPoPEndorsement
import org.veriblock.core.contracts.BlockEvidence
import org.veriblock.core.crypto.Crypto
import org.veriblock.core.utilities.createLogger
import org.veriblock.core.utilities.extensions.asHexBytes
import org.veriblock.core.utilities.extensions.flip
import org.veriblock.core.utilities.extensions.isHex
import org.veriblock.core.utilities.extensions.toHex
import org.veriblock.sdk.alt.ApmInstruction
import org.veriblock.sdk.alt.model.Atv
import org.veriblock.sdk.alt.model.PopMempool
import org.veriblock.sdk.alt.model.SecurityInheritingBlock
import org.veriblock.sdk.alt.model.SecurityInheritingTransaction
import org.veriblock.sdk.alt.model.SecurityInheritingTransactionVout
import org.veriblock.sdk.alt.model.Vtb
import org.veriblock.sdk.alt.plugin.PluginConfig
import org.veriblock.sdk.alt.plugin.PluginSpec
import org.veriblock.sdk.models.AltPublication
import org.veriblock.sdk.models.PublicationData
import org.veriblock.sdk.models.StateInfo
import org.veriblock.sdk.models.VeriBlockBlock
import org.veriblock.sdk.models.VeriBlockPublication
import org.veriblock.sdk.services.SerializeDeserializeService
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

    private val payoutAddress: String

    private var payoutAddressScript: ByteArray? = null

    private suspend fun getPayoutAddressScript() = payoutAddressScript ?: run {
        val script = if (payoutAddress.isHex()) {
            payoutAddress.asHexBytes()
        } else {
            validateAddress(payoutAddress)
        }
        payoutAddressScript = script
        script
    }

    override val httpClient = createHttpClient(
        authConfig = config.auth,
        contentTypes = listOf(ContentType.Application.Json, ContentType.Text.Any),
        connectionTimeout = config.daemonConnectionTimeout
    )

    override val requestsLogger = config.requestLogsPath?.let {
        createLoggerFor(key)
    }

    init {
        config.checkValidity()
        checkNotNull(config.payoutAddress) {
            "$name's payoutAddress ($key.payoutAddress) must be configured!"
        }
        if (config.payoutAddress.isEmpty() || config.payoutAddress == "INSERT PAYOUT ADDRESS") {
            error("'${config.payoutAddress}' is not a valid value for the $name's payoutAddress configuration ($key.payoutAddress). Please set up a valid payout address")
        }
        payoutAddress = config.payoutAddress
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
            hash = btcBlock.hash,
            height = btcBlock.height,
            previousHash = btcBlock.previousblockhash ?: "0000000000000000000000000000000000000000000000000000000000000000",
            confirmations = btcBlock.confirmations,
            version = btcBlock.version,
            nonce = btcBlock.nonce,
            merkleRoot = btcBlock.merkleroot,
            difficulty = btcBlock.difficulty,
            coinbaseTransactionId = btcBlock.tx[0],
            transactionIds = btcBlock.tx.drop(1),
            endorsedBy = btcBlock.pop.state.endorsedBy,
            knownVbkHashes = btcBlock.pop.state.stored.vbkblocks,
            veriBlockPublicationIds = btcBlock.pop.state.stored.atvs,
            bitcoinPublicationIds = btcBlock.pop.state.stored.vtbs
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
            },
            btcTransaction.blockhash
        )
    }

    override fun getPayoutDelay(): Int {
        return config.payoutDelay
    }

    override suspend fun getBestKnownVbkBlockHash(): String {
        return rpcRequest("getvbkbestblockhash")
    }

    override suspend fun getPopMempool(): PopMempool {
        val response: BtcPopStoredStateData = rpcRequest("getrawpopmempool")
        return PopMempool(response.vbkblocks, response.atvs, response.vtbs)
    }

    override suspend fun getAtv(id: String): Atv? {
        val response: BtcAtv = try {
            rpcRequest("getrawatv", listOf(id, 1))
        } catch (e: RpcException) {
            if (e.errorCode == -5) {
                // ATV not found
                return null
            } else {
                throw e
            }
        }
        return Atv(response.atv.transaction.hash, response.atv.blockOfProof.hash)
    }

    override suspend fun getVtb(id: String): Vtb? {
        val response: BtcVtb = try {
            rpcRequest("getrawvtb", listOf(id, 1))
        } catch (e: RpcException) {
            if (e.errorCode == -5) {
                // VTB not found
                return null
            } else {
                throw e
            }
        }
        return Vtb(response.vtb.containingBlock.hash)
    }

    override suspend fun getMiningInstruction(blockHeight: Int?): ApmInstruction {
        val actualBlockHeight = blockHeight
        // Retrieve top block height from API if not supplied
            ?: getBestBlockHeight()

        logger.info { "Retrieving mining instruction at height $actualBlockHeight from $name daemon at ${config.host}..." }
        val response: BtcPublicationData = rpcRequest("getpopdata", listOf(actualBlockHeight))

        if (response.block_header == null) {
            error("Publication data's 'block_header' must be set!")
        }
        if (response.raw_contextinfocontainer == null) {
            error("Publication data's 'raw_contextinfocontainer' must be set!")
        }
        if (response.last_known_veriblock_blocks.isNullOrEmpty()) {
            error("Publication data's 'last_known_veriblock_blocks' must not be empty!")
        }
        if (response.last_known_bitcoin_blocks.isNullOrEmpty()) {
            error("Publication data's 'last_known_bitcoin_blocks' must not be empty!")
        }

        val publicationData = PublicationData(
            id,
            response.block_header.asHexBytes(),
            getPayoutAddressScript(),
            response.raw_contextinfocontainer.asHexBytes()
        )
        return ApmInstruction(
            actualBlockHeight,
            publicationData,
            response.last_known_veriblock_blocks.map { it.asHexBytes() },
            response.last_known_bitcoin_blocks.map { it.asHexBytes() }
        )
    }

    override suspend fun submit(
        contextBlocks: List<VeriBlockBlock>,
        atvs: List<AltPublication>,
        vtbs: List<VeriBlockPublication>
    ) {
        logger.debug { "Submitting PoP data to $name daemon at ${config.host}..." }
        val submitPopResponse: SubmitPopResponse = rpcRequest("submitpop", listOf(
            contextBlocks.map {
                SerializeDeserializeService.serialize(it).toHex()
            },
            // Submit only 1 VTB at most
            vtbs.asSequence().take(1).map {
                SerializeDeserializeService.serialize(
                    it.copy(context = emptyList())
                ).toHex()
            }.toList(),
            atvs.map {
                SerializeDeserializeService.serialize(
                    it.copy(context = emptyList())
                ).toHex()
            }
        ))
        logger.debug { "SubmitPoP Response: $submitPopResponse" }
        // Submit extra VTBs one by one
        vtbs.asSequence().drop(1).forEach {
            logger.debug { "Submitting VTB with first BTC context block: ${it.getFirstBitcoinBlock()?.hash}" }
            val submitVtbResponse: SubmitPopResponse = rpcRequest("submitpop", listOf(
                emptyList(),
                listOf(SerializeDeserializeService.serialize(
                    it.copy(context = emptyList())
                ).toHex()),
                emptyList()
            ))
            logger.debug { "SubmitPoP VTB Partial Response: $submitVtbResponse" }
        }
    }

    override fun extractAddressDisplay(addressData: ByteArray): String {
        // TODO: extract correctly from param, use BitcoinJ maybe?
        return config.payoutAddress ?: "UNKNOWN_ADDRESS"
    }

    private val crypto = Crypto()

    override fun extractBlockEvidence(altchainPopEndorsement: AltchainPoPEndorsement): BlockEvidence {
        val contextBuffer = ByteBuffer.wrap(altchainPopEndorsement.getContextInfo())
        val height = contextBuffer.getInt()
        val hash = crypto.SHA256D(altchainPopEndorsement.getHeader()).flip()
        val previousHash = altchainPopEndorsement.getHeader().copyOfRange(4, 36).flip()
        val previousKeystone = contextBuffer.getBytes(32).flip()

        return BlockEvidence(height, hash, previousHash, previousKeystone, null)
    }

    override suspend fun getBlockChainInfo(): StateInfo {
        return try {
            val response: BlockChainInfo = rpcRequest("getblockchaininfo")
            val blockDifference = abs(response.headers - response.blocks)
            StateInfo(
                response.headers,
                response.blocks,
                blockDifference,
                (response.headers > 0 && blockDifference < 4) && !response.initialblockdownload,
                response.initialblockdownload,
                response.chain
            )
        } catch (e: Exception) {
            logger.warn { "Unable to perform the getblockchaininfo rpc call to ${config.host} (is it reachable?)" }
            StateInfo()
        }
    }

    private suspend fun validateAddress(address: String): ByteArray {
        try {
            val response: AddressValidationResponse = rpcRequest("validateaddress", listOf(address))
            if (response.isvalid) {
                return response.scriptPubKey?.asHexBytes()
                    ?: error("Unexpected response from 'validateaddress'. 'scriptPubKey' is not set!")
            } else {
                error("Invalid Address: $address")
            }
        } catch (e: RpcException) {
            if (e.errorCode == -32601) {
                // Method not found
                error("Method 'validateaddress' not found. It must be implemented in order to specify payout addresses.")
            } else {
                throw e
            }
        } catch (e: Exception) {
            error("Unable to perform the validateaddress rpc call to ${config.host} (is it reachable?)")
        }
    }
}

fun ByteBuffer.getBytes(count: Int): ByteArray {
    val result = ByteArray(count)
    get(result)
    return result
}
