// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.alt.plugins.ethereum

import io.ktor.http.*
import org.bouncycastle.util.Arrays
import org.veriblock.alt.plugins.HttpSecurityInheritingChain
import org.veriblock.alt.plugins.createHttpClient
import org.veriblock.alt.plugins.rpcRequest
import org.veriblock.alt.plugins.util.RpcException
import org.veriblock.alt.plugins.util.createLoggerFor
import org.veriblock.core.altchain.AltchainPoPEndorsement
import org.veriblock.core.contracts.BlockEvidence
import org.veriblock.core.crypto.*
import org.veriblock.core.utilities.createLogger
import org.veriblock.core.utilities.debugWarn
import org.veriblock.core.utilities.extensions.asHexBytes
import org.veriblock.core.utilities.extensions.flip
import org.veriblock.core.utilities.extensions.isHex
import org.veriblock.core.utilities.extensions.toHex
import org.veriblock.sdk.alt.ApmInstruction
import org.veriblock.sdk.alt.model.Atv
import org.veriblock.sdk.alt.model.NetworkParam
import org.veriblock.sdk.alt.model.PopMempool
import org.veriblock.sdk.alt.model.PopParamsResponse
import org.veriblock.sdk.alt.model.PopPayoutParams
import org.veriblock.sdk.alt.model.SecurityInheritingBlock
import org.veriblock.sdk.alt.model.SecurityInheritingTransaction
import org.veriblock.sdk.alt.model.SecurityInheritingTransactionVout
import org.veriblock.sdk.alt.model.SubmitPopResponse
import org.veriblock.sdk.alt.model.Vtb
import org.veriblock.sdk.alt.plugin.PluginConfig
import org.veriblock.sdk.alt.plugin.PluginSpec
import org.veriblock.sdk.models.*
import org.veriblock.sdk.services.SerializeDeserializeService
import java.nio.ByteBuffer
import kotlin.math.abs
import kotlin.math.roundToLong

private val logger = createLogger {}

private const val NOT_FOUND_ERROR_CODE = -5

@PluginSpec(name = "EthereumFamily", key = "eth")
class EthereumFamilyChain(
    override val key: String,
    configuration: PluginConfig
) : HttpSecurityInheritingChain {

    override val config = EthereumConfig(configuration)

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
        contentTypes = listOf(ContentType.Application.Json),
        connectionTimeout = config.daemonConnectionTimeout,
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
            error(
                "'${config.payoutAddress}' is not a valid value for the $name's payoutAddress configuration ($key.payoutAddress). Please set up a valid payout address"
            )
        }
        payoutAddress = config.payoutAddress
    }

    override suspend fun getBestBlockHeight(): Int {
        logger.trace { "Retrieving best block height..." }
        return rpcRequest<String>("eth_blockNumber").asEthHexInt()
    }

    override suspend fun getBlock(hash: String): SecurityInheritingBlock? {
        logger.debug { "Retrieving block $hash..." }
        val btcBlock: EthBlock = try {
            rpcRequest("eth_getBlockByHash", listOf(hash, true))
        } catch (e: RpcException) {
            if (e.errorCode == NOT_FOUND_ERROR_CODE) {
                // Block not found
                return null
            } else {
                throw e
            }
        }
        return SecurityInheritingBlock(
            hash = btcBlock.hash,
            height = btcBlock.number.asEthHexInt(),
            previousHash = btcBlock.parentHash ?: "0000000000000000000000000000000000000000000000000000000000000000",
            confirmations = 0, // FIXME
            version = 0, // FIXME
            nonce = btcBlock.nonce.asEthHexInt(),
            merkleRoot = btcBlock.transactionsRoot,
            difficulty = btcBlock.difficulty.asEthHexDouble(),
            miner = btcBlock.miner,
            transactionIds = btcBlock.transactions,
            endorsedBy = btcBlock.pop.state.endorsedBy,
            knownVbkHashes = btcBlock.pop.state.stored.vbkblocks,
            veriBlockPublicationIds = btcBlock.pop.state.stored.atvs,
            bitcoinPublicationIds = btcBlock.pop.state.stored.vtbs
        )
    }

    private suspend fun getBlockHash(height: Int): String? {
        logger.debug { "Retrieving block hash @$height..." }
        return try {
            rpcRequest<EthBlock>("eth_getBlockByNumber", listOf(height.asEthHex(), false)).hash
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
            rpcRequest("eth_getBlockByHash", listOf(blockHash, true))
        } catch (e: RpcException) {
            if (e.errorCode == NOT_FOUND_ERROR_CODE) {
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

    override suspend fun getTransaction(txId: String, blockHash: String?): SecurityInheritingTransaction? {
        logger.debug { "Retrieving transaction $txId..." }
        val btcTransaction: EthTransaction = try {
            rpcRequest("eth_getRawTransactionByHash", listOf(txId))
        } catch (e: RpcException) {
            if (e.errorCode == NOT_FOUND_ERROR_CODE) {
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
        return rpcRequest("pop_getVbkBestBlockHash")
    }

    override suspend fun getPopMempool(): PopMempool {
        val response: EthPopStoredStateData = rpcRequest("pop_getRawPopMempool")
        return PopMempool(response.vbkblocks, response.atvs, response.vtbs)
    }

    override suspend fun getAtv(id: String): Atv? {
        val response: EthAtv = try {
            rpcRequest("pop_getRawAtv", listOf(id, 1)) // -
        } catch (e: RpcException) {
            if (e.errorCode == NOT_FOUND_ERROR_CODE) {
                // ATV not found
                return null
            } else {
                throw e
            }
        }
        return Atv(
            vbkTransactionId = response.atv.transaction.hash,
            vbkBlockOfProofHash = response.atv.blockOfProof.hash,
            containingBlock = response.blockhash,
            confirmations = response.confirmations
        )
    }

    override suspend fun getVtb(id: String): Vtb? {
        val response: EthVtb = try {
            rpcRequest("pop_getRawVtb", listOf(id, 1)) // -
        } catch (e: RpcException) {
            if (e.errorCode == NOT_FOUND_ERROR_CODE) {
                // VTB not found
                return null
            } else {
                throw e
            }
        }
        return Vtb(response.vtb.containingBlock.hash)
    }

    override suspend fun getMiningInstructionByHeight(blockHeight: Int?): ApmInstruction {
        val actualBlockHeight = blockHeight
        // Retrieve top block height from API if not supplied
            ?: getBestBlockHeight()

        logger.debug { "Retrieving mining instruction at height $actualBlockHeight from $name daemon at ${config.host}..." }
        val response: EthPublicationData = rpcRequest("pop_getPopDataByHeight", listOf(actualBlockHeight))

        if (response.last_known_veriblock_block.isEmpty()) {
            error("Publication data's 'last_known_veriblock_blocks' must not be empty!")
        }
        if (response.last_known_bitcoin_block.isEmpty()) {
            error("Publication data's 'last_known_bitcoin_blocks' must not be empty!")
        }

        val publicationData = PublicationData(
            id,
            response.block_header.asHexBytes(),
            getPayoutAddressScript(),
            response.authenticated_context.asHexBytes()
        )
        return ApmInstruction(
            actualBlockHeight,
            publicationData,
            listOf(response.last_known_veriblock_block.asHexBytes()),
            listOf(response.last_known_bitcoin_block.asHexBytes())
        )
    }

    override suspend fun submitPopVbk(block: VeriBlockBlock): SubmitPopResponse {
        return rpcRequest("pop_submitPopVbk", listOf(SerializeDeserializeService.serialize(block).toHex()))
    }

    override suspend fun submitPopAtv(atvs: AltPublication): SubmitPopResponse {
        return rpcRequest("pop_submitPopAtv", listOf(SerializeDeserializeService.serialize(atvs).toHex()))
    }

    override suspend fun submitPopVtb(vtb: VeriBlockPublication): SubmitPopResponse {
        return rpcRequest("pop_submitPopVtb", listOf(SerializeDeserializeService.serialize(vtb).toHex()))
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
            val response: BlockChainInfo = rpcRequest("getblockchaininfo") // FIXME
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
            logger.debugWarn(e) { "Unable to perform the getblockchaininfo rpc call to ${config.host} (is it reachable?)" }
            StateInfo()
        }
    }

    override suspend fun getPopParams(): PopParamsResponse {
        val ethPopParams = rpcRequest<EthPoPParams>("pop_getPopParams", emptyList<String>())
        return PopParamsResponse(
            ethPopParams.popActivationHeight.toInt(),
            ethPopParams.networkId,
            PopPayoutParams(ethPopParams.popPayoutDelay),
            NetworkParam(ethPopParams.vbkBootstrap.toString())
        )
    }

    override suspend fun getVbkBlock(hash: String): VeriBlockBlock? {
        TODO("Not yet implemented")
    }

    override suspend fun getBestKnownBtcBlockHash(): String {
        logger.debug { "Retrieving the best known BTC block hash..." }
        return rpcRequest("pop_getBtcBestBlockHash")
    }

    override suspend fun getBtcBlock(hash: String): BitcoinBlock? {
        TODO("Not yet implemented")
    }

    private suspend fun validateAddress(address: String): ByteArray {
        TODO("Not yet implemented")
    }
}

fun ByteBuffer.getBytes(count: Int): ByteArray {
    val result = ByteArray(count)
    get(result)
    return result
}
