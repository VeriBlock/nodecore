// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.alt.plugins.bitcoin

import io.ktor.http.*
import org.bouncycastle.util.Arrays
import org.veriblock.alt.plugins.HttpSecurityInheritingChain
import org.veriblock.alt.plugins.createHttpClient
import org.veriblock.alt.plugins.rpcRequest
import org.veriblock.alt.plugins.util.RpcException
import org.veriblock.alt.plugins.util.createLoggerFor
import org.veriblock.alt.plugins.util.segwitToBech32
import org.veriblock.core.altchain.AltchainPopEndorsement
import org.veriblock.core.contracts.BlockEvidence
import org.veriblock.core.contracts.asEgBlockHash
import org.veriblock.core.crypto.*
import org.veriblock.core.utilities.createLogger
import org.veriblock.core.utilities.debugWarn
import org.veriblock.core.utilities.extensions.asHexBytes
import org.veriblock.core.utilities.extensions.isHex
import org.veriblock.core.utilities.extensions.toHex
import org.veriblock.sdk.alt.ApmInstruction
import org.veriblock.sdk.alt.model.Atv
import org.veriblock.sdk.alt.model.PopMempool
import org.veriblock.sdk.alt.model.PopParamsResponse
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
import kotlin.math.abs
import kotlin.math.roundToLong

private val logger = createLogger {}

private const val NOT_FOUND_ERROR_CODE = -5

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

    private lateinit var payoutAddress: String

    private var payoutAddressScript: ByteArray? = null

    private suspend fun getPayoutAddressScript() = payoutAddressScript ?: run {
        if (!::payoutAddress.isInitialized) {
            throw RuntimeException("Trying to access the 'payoutAddress' before it was initialized! 'validatePayoutAddress()' must be called first")
        }
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
    }

    override suspend fun getBestBlockHeight(): Int {
        logger.trace { "Retrieving best block height..." }
        return rpcRequest("getblockcount")
    }

    override suspend fun getBlock(hash: String): SecurityInheritingBlock? {
        logger.debug { "Retrieving block $hash..." }
        val btcBlock: BtcBlock = try {
            rpcRequest("getblock", listOf(hash, 1))
        } catch (e: RpcException) {
            if (e.errorCode == -1 || e.errorCode == NOT_FOUND_ERROR_CODE) {
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
            merkleRoot = btcBlock.merkleroot,
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
        val btcTransaction: BtcTransaction = try {
            if (blockHash == null) {
                rpcRequest("getrawtransaction", listOf(txId, 1))
            } else {
                rpcRequest("getrawtransaction", listOf(txId, 1, blockHash))
            }
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
        return rpcRequest("getvbkbestblockhash")
    }

    override suspend fun getPopMempool(): PopMempool {
        val response: BtcPopStoredStateData = rpcRequest("getrawpopmempool")
        return PopMempool(response.vbkblocks, response.atvs, response.vtbs)
    }

    override suspend fun getAtv(id: String): Atv? {
        val response: BtcAtv = try {
            rpcRequest("getrawatv", listOf(id, 2))
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
        val response: BtcVtb = try {
            rpcRequest("getrawvtb", listOf(id, 1))
        } catch (e: RpcException) {
            if (e.errorCode == NOT_FOUND_ERROR_CODE) {
                // VTB not found
                return null
            } else {
                throw e
            }
        }
        return Vtb(
            btcBlockOfProof = response.vtb.transaction.blockOfProof,
            btcBlockOfProofContext = response.vtb.transaction.blockOfProofContext
        )
    }

    override suspend fun getMiningInstructionByHeight(blockHeight: Int?): ApmInstruction {
        val actualBlockHeight = blockHeight
        // Retrieve top block height from API if not supplied
            ?: getBestBlockHeight()

        logger.debug { "Retrieving mining instruction at height $actualBlockHeight from $name daemon at ${config.host}..." }
        val response: BtcPublicationData = rpcRequest("getpopdatabyheight", listOf(actualBlockHeight))

        if (response.last_known_veriblock_blocks.isEmpty()) {
            error("Publication data's 'last_known_veriblock_blocks' must not be empty!")
        }
        if (response.last_known_bitcoin_blocks.isEmpty()) {
            error("Publication data's 'last_known_bitcoin_blocks' must not be empty!")
        }

        val publicationData = PublicationData(
            id,
            response.block_header.asHexBytes(),
            getPayoutAddressScript(),
            response.authenticated_context.serialized.asHexBytes()
        )
        return ApmInstruction(
            actualBlockHeight,
            publicationData,
            response.last_known_veriblock_blocks.map { it.asHexBytes() },
            response.last_known_bitcoin_blocks.map { it.asHexBytes() }
        )
    }

    override suspend fun submitPopVbk(block: VeriBlockBlock): SubmitPopResponse {
        return rpcRequest("submitpopvbk", listOf(SerializeDeserializeService.serialize(block).toHex()))
    }

    override suspend fun submitPopAtv(atv: AltPublication): SubmitPopResponse {
        return rpcRequest("submitpopatv", listOf(SerializeDeserializeService.serialize(atv).toHex()))
    }

    override suspend fun submitPopVtb(vtb: VeriBlockPublication): SubmitPopResponse {
        return rpcRequest("submitpopvtb", listOf(SerializeDeserializeService.serialize(vtb).toHex()))
    }

    override fun extractAddressDisplay(addressData: ByteArray): String {
        return if (config.addressPrefix != null) {
            val witnessProgram = ByteArray(addressData.size - 2)
            addressData.copyInto(witnessProgram, 0,2, addressData.size)
            try {
                segwitToBech32(config.addressPrefix,0, witnessProgram)
            } catch (exception: Exception) {
                throw IllegalArgumentException("Can't extract the Bech32 address from the address data: ${addressData.toHex()}", exception)
            }
        } else {
            config.payoutAddress ?: "UNKNOWN_ADDRESS"
        }
    }

    override suspend fun extractBlockEvidences(altchainPopEndorsements: List<AltchainPopEndorsement>): List<BlockEvidence> {
        val hexPopEndorsements = altchainPopEndorsements.map { altchainPopEndorsement ->
            altchainPopEndorsement.getRawData().toHex()
        }
        val blockEvidence: List<BtcBlockEvidence> = rpcRequest("extractblockinfo", listOf(hexPopEndorsements))
        return blockEvidence.map { btcBlockEvidence ->
            BlockEvidence(
                btcBlockEvidence.height,
                btcBlockEvidence.hash.asEgBlockHash(),
                btcBlockEvidence.previousHash.asEgBlockHash(),
                btcBlockEvidence.previousKeystone.asEgBlockHash(),
                btcBlockEvidence.secondPreviousKeystone?.asEgBlockHash()
            )
        }
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
            logger.debugWarn(e) { "Unable to perform the getblockchaininfo rpc call to ${config.host} (is it reachable?)" }
            StateInfo()
        }
    }

    override suspend fun getPopParams(): PopParamsResponse {
        return rpcRequest("getpopparams", emptyList<String>())
    }

    override suspend fun getVbkBlock(hash: String): VeriBlockBlock? {
        logger.debug { "Retrieving the VBK block for the hash $hash" }
        return try {
            val response: VbkBlock = rpcRequest("getvbkblock", listOf(hash))
            VeriBlockBlock(
                height = response.header?.height ?: error("getvbkblock height field must be set"),
                version = response.header.version ?: error("getvbkblock version field must be set"),
                previousBlock = response.header.previousBlock?.asAnyVbkHash()?.trimToPreviousBlockSize() ?: error("getvbkblock previousBlock field must be set"),
                previousKeystone = response.header.previousKeystone?.asAnyVbkHash()?.trimToPreviousKeystoneSize() ?: error("getvbkblock previousKeystone field must be set"),
                secondPreviousKeystone = response.header.secondPreviousKeystone?.asAnyVbkHash()?.trimToPreviousKeystoneSize() ?: error("getvbkblock secondPreviousKeystone field must be set"),
                merkleRoot = response.header.merkleRoot?.asTruncatedMerkleRoot() ?: error("getvbkblock merkleRoot field must be set"),
                timestamp = response.header.timestamp ?: error("getvbkblock timestamp field must be set"),
                difficulty = response.header.difficulty ?: error("getvbkblock difficulty field must be set"),
                nonce = response.header.nonce ?: error("getvbkblock nonce field must be set")
            )
        } catch (e: RpcException) {
            if (e.errorCode == NOT_FOUND_ERROR_CODE) {
                // Block not found
                null
            } else {
                throw e
            }
        }
    }

    override suspend fun getBestKnownBtcBlockHash(): String {
        logger.debug { "Retrieving the best known BTC block hash..." }
        return rpcRequest("getbtcbestblockhash")
    }

    override suspend fun getBtcBlock(hash: String): BitcoinBlock? {
        logger.debug { "Retrieving the BTC block for the hash $hash" }
        return try {
            val response: BtcBlockBlock = rpcRequest("getbtcblock", listOf(hash))
            BitcoinBlock(
                version = response.header?.version ?: error("getbtcblock version field must be set"),
                previousBlock = response.header.previousBlock?.asBtcHash() ?: error("getbtcblock previousBlock field must be set"),
                merkleRoot = response.header.merkleRoot?.asMerkleRoot() ?: error("getbtcblock merkleRoot field must be set"),
                timestamp = response.header.timestamp ?: error("getbtcblock timestamp field must be set"),
                difficulty = 0, // FIXME difficulty field is not at the response
                nonce = response.header.nonce ?: error("getbtcblock nonce field must be set")
            )
        } catch (e: RpcException) {
            if (e.errorCode == NOT_FOUND_ERROR_CODE) {
                // Block not found
                null
            } else {
                throw e
            }
        }
    }

    override suspend fun getMissingBtcBlockHashes(): List<String> {
        logger.debug { "Retrieving the missing BTC block hashes" }
        return rpcRequest("getmissingbtcblockhashes")
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
        val response: AddressValidationResponse = try {
            rpcRequest("validateaddress", listOf(address))
        } catch (e: RpcException) {
            if (e.errorCode == -32601) {
                // Method not found
                error("Method 'validateaddress' not found. It must be implemented in order to specify payout addresses.")
            } else {
                throw e
            }
        } catch (e: Exception) {
            throw IllegalStateException("Unable to perform the validateaddress rpc call to ${config.host} (is it reachable?)", e)
        }
        if (response.isvalid) {
            return response.scriptPubKey?.asHexBytes()
                ?: error("Unexpected response from 'validateaddress'. 'scriptPubKey' is not set!")
        } else {
            error("Invalid Address: $address")
        }
    }
}
