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
import org.veriblock.alt.plugins.nullableRpcRequest
import org.veriblock.alt.plugins.rpcRequest
import org.veriblock.alt.plugins.util.RpcException
import org.veriblock.alt.plugins.util.toEthHash
import org.veriblock.alt.plugins.util.asEthHex
import org.veriblock.alt.plugins.util.asEthHexInt
import org.veriblock.alt.plugins.util.createLoggerFor
import org.veriblock.alt.plugins.util.asEthHash
import org.veriblock.core.altchain.AltchainPoPEndorsement
import org.veriblock.core.contracts.BlockEvidence
import org.veriblock.core.tuweni.crypto.Hash
import org.veriblock.core.utilities.SerializerUtility
import org.veriblock.core.utilities.createLogger
import org.veriblock.core.utilities.debugWarn
import org.veriblock.core.utilities.extensions.asHexBytes
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
import java.lang.RuntimeException
import java.nio.ByteBuffer
import org.veriblock.alt.plugins.bitcoin.BtcBlockEvidence
import org.veriblock.core.contracts.asEgBlockHash
import kotlin.math.abs
import kotlin.math.roundToLong

private val logger = createLogger {}

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

    private lateinit var payoutAddress: String

    private var payoutAddressScript: ByteArray? = null

    private suspend fun getPayoutAddressScript() = payoutAddressScript ?: run {
        if (!::payoutAddress.isInitialized) {
            throw RuntimeException("Trying to access the 'payoutAddress' before it was initialized! 'validatePayoutAddress()' must be called first")
        }
        val script = validateAddress(payoutAddress)
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
    }

    override suspend fun getBestBlockHeight(): Int {
        logger.trace { "Retrieving best block height..." }
        return rpcRequest<String>("eth_blockNumber", version = "2.0").asEthHexInt()
    }

    override suspend fun getBlock(hash: String): SecurityInheritingBlock? {
        val ethHash = hash.toEthHash()
        logger.debug { "Retrieving block $hash..." }
        val block: EthBlock? = nullableRpcRequest("eth_getBlockByHash", listOf(ethHash, true), "2.0")
        val popBlock: EthPopBlockData? = try {
            nullableRpcRequest("pop_getBlockByHash", listOf(ethHash), "2.0")
        } catch (exception: RpcException) {
            if (exception.errorCode == -32000) {
                null
            } else {
                throw exception
            }
        }
        return block?.let { ethBlock ->
            SecurityInheritingBlock(
                hash = ethBlock.hash.asEthHash(),
                height = ethBlock.number.asEthHexInt(),
                previousHash = ethBlock.parentHash?.asEthHash() ?: "0000000000000000000000000000000000000000000000000000000000000000",
                merkleRoot = ethBlock.transactionsRoot,
                coinbaseTransactionId = "TODO",
                transactionIds = ethBlock.transactions,
                endorsedBy = popBlock?.pop?.state?.endorsedBy ?: listOf(),
                knownVbkHashes = popBlock?.pop?.data?.context?.map { it.id } ?: listOf(),
                veriBlockPublicationIds = popBlock?.pop?.data?.atvs?.map { it.id } ?: listOf(),
                bitcoinPublicationIds = popBlock?.pop?.data?.vtbs?.map { it.id } ?: listOf()
            )
        }
    }

    private suspend fun getBlockHash(height: Int): String? {
        val ethHeight = height.asEthHex()
        logger.debug { "Retrieving block hash @ $height..." }
        return nullableRpcRequest<EthBlock>("eth_getBlockByNumber", listOf(ethHeight, false), "2.0")?.hash
    }

    override suspend fun getBlock(height: Int): SecurityInheritingBlock? {
        logger.debug { "Retrieving block @ $height..." }
        val blockHash = getBlockHash(height)
            ?: return null
        return getBlock(blockHash)
    }

    override suspend fun checkBlockIsOnMainChain(height: Int, blockHeaderToCheck: ByteArray): Boolean {
        logger.debug { "Checking block @$height has header ${blockHeaderToCheck.toHex()}..." }
        val blockHash = getBlockHash(height)
            ?: return false
        // Get raw block
        val rawBlock: String = rpcRequest("eth_getBlockByHash", listOf(blockHash, true), "2.0")
        // Extract header
        val header: ByteArray = Arrays.copyOf(rawBlock.asHexBytes(), blockHeaderToCheck.size)
        // Check header
        return header.contentEquals(blockHeaderToCheck)
    }

    override suspend fun getTransaction(txId: String, blockHash: String?): SecurityInheritingTransaction? {
        val ethTxId = txId.toEthHash()
        logger.debug { "Retrieving transaction $txId..." }
        val btcTransaction: EthTransaction? = nullableRpcRequest("eth_getRawTransactionByHash", listOf(ethTxId), "2.0")
        return btcTransaction?.let { ethTransaction ->
            SecurityInheritingTransaction(
                ethTransaction.txid,
                ethTransaction.confirmations,
                ethTransaction.vout.map {
                    SecurityInheritingTransactionVout(
                        (it.value * 100000000).roundToLong(),
                        it.scriptPubKey.hex
                    )
                },
                ethTransaction.blockhash
            )
        }
    }

    override fun getPayoutDelay(): Int {
        return config.payoutDelay
    }

    override suspend fun getBestKnownVbkBlockHash(): String {
        return rpcRequest("pop_getVbkBestBlockHash", version = "2.0")
    }

    override suspend fun getPopMempool(): PopMempool {
        val response: EthPopStoredStateData = rpcRequest("pop_getRawPopMempool", version = "2.0")
        return PopMempool(response.vbkblocks, response.atvs, response.vtbs)
    }

    override suspend fun getAtv(id: String): Atv? {
        val response: EthAtv? = nullableRpcRequest("pop_getRawAtv", listOf(id, 1), "2.0")
        return response?.let {
            Atv(
                vbkTransactionId = it.atv.transaction.hash,
                vbkBlockOfProofHash = it.atv.blockOfProof.hash,
                containingBlock = it.blockhash,
                confirmations = it.confirmations
            )
        }
    }

    override suspend fun getVtb(id: String): Vtb? {
        val response: EthVtb? = nullableRpcRequest("pop_getRawVtb", listOf(id, 1), "2.0")
        return response?.let {
            Vtb(it.vtb.containingBlock.hash)
        }
    }

    override suspend fun getMiningInstructionByHeight(blockHeight: Int?): ApmInstruction {
        val actualBlockHeight = blockHeight
        // Retrieve top block height from API if not supplied
            ?: getBestBlockHeight()

        logger.debug { "Retrieving mining instruction at height $actualBlockHeight from $name daemon at ${config.host}..." }
        val response: EthPublicationData = rpcRequest("pop_getPopDataByHeight", listOf(actualBlockHeight), "2.0")

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
        return rpcRequest("pop_submitPopVbk", listOf(SerializeDeserializeService.serialize(block).toHex()), "2.0")
    }

    override suspend fun submitPopAtv(atv: AltPublication): SubmitPopResponse {
        return rpcRequest("pop_submitPopAtv", listOf(SerializeDeserializeService.serialize(atv).toHex()), "2.0")
    }

    override suspend fun submitPopVtb(vtb: VeriBlockPublication): SubmitPopResponse {
        return rpcRequest("pop_submitPopVtb", listOf(SerializeDeserializeService.serialize(vtb).toHex()), "2.0")
    }

    override fun extractAddressDisplay(addressData: ByteArray): String {
        return addressData.toHex().toEthHash()
    }

    override suspend fun extractBlockEvidence(altchainPopEndorsement: AltchainPoPEndorsement): BlockEvidence {
        val blockEvidence: BtcBlockEvidence = rpcRequest("pop_extractBlockInfo", listOf(altchainPopEndorsement.getRawData().toHex()))
        return BlockEvidence(
            blockEvidence.height,
            blockEvidence.hash.asEgBlockHash(),
            blockEvidence.previousHash.asEgBlockHash(),
            blockEvidence.previousKeystone.asEgBlockHash(),
            blockEvidence.secondPreviousKeystone?.asEgBlockHash()
        )
    }

    override suspend fun getBlockChainInfo(): StateInfo {
        return try {
            val ethBestBlockHeight = getBestBlockHeight()
            val ethNetworkType = getNetworkType()
            val ethSyncStatus = getSyncStatus()
            val isSynchronized = ethSyncStatus.currentBlock == null && ethSyncStatus.highestBlock == null && ethSyncStatus.startingBlock == null
            val networkHeight = if (!isSynchronized) {
                ethSyncStatus.highestBlock!!
            } else {
                ethBestBlockHeight
            }
            val localBlockchainHeight = if (!isSynchronized) {
                ethSyncStatus.currentBlock!!
            } else {
                ethBestBlockHeight
            }
            val blockDifference = if (!isSynchronized) {
                abs(ethSyncStatus.highestBlock!! - ethSyncStatus.currentBlock!!)
            } else {
                0
            }
            StateInfo(networkHeight, localBlockchainHeight, blockDifference, isSynchronized, false, ethNetworkType)
        } catch (e: Exception) {
            logger.debugWarn(e) { "Unable to perform the getblockchaininfo rpc call to ${config.host} (is it reachable?)" }
            StateInfo()
        }
    }

    private suspend fun getSyncStatus() = try {
        rpcRequest<Boolean>("eth_syncing", emptyList<String>(), "2.0")
        EthSyncStatus()
    } catch (exception: IllegalStateException) {
        rpcRequest("eth_syncing", emptyList<String>(), "2.0")
    }

    private suspend fun getNetworkType(): String {
        val popParams = getPopParams()
        return when(popParams.networkId) {
            1L -> "mainnet"
            2L, 3L, 4L, 42L -> "testnet"
            else -> "unknown"
        }
    }

    override suspend fun getPopParams(): PopParamsResponse {
        val ethPopParams = rpcRequest<EthPoPParams>("pop_getPopParams", emptyList<String>(), "2.0")
        return PopParamsResponse(
            ethPopParams.popActivationHeight.toInt(),
            ethPopParams.networkId,
            PopPayoutParams(ethPopParams.popPayoutDelay),
            NetworkParam(ethPopParams.vbkBootstrap.toString())
        )
    }

    override suspend fun getVbkBlock(hash: String): VeriBlockBlock? {
        TODO("Not yet implemented (getVbkBlock)") // pop_getVbkBlockByHash
    }

    override suspend fun getBestKnownBtcBlockHash(): String {
        logger.debug { "Retrieving the best known BTC block hash..." }
        return rpcRequest("pop_getBtcBestBlockHash", version = "2.0")
    }

    override suspend fun getBtcBlock(hash: String): BitcoinBlock? {
        TODO("Not yet implemented (getBtcBlock)") // pop_getBtcBlockByHash
    }

    override fun validatePayoutAddress() {
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

    private suspend fun validateAddress(address: String): ByteArray {
        val addressHex = address.asEthHash()
        if (!addressHex.isHex()) {
            throw IllegalArgumentException("Invalid ETH address: $address")
        }
        return addressHex.asHexBytes()
    }
}
