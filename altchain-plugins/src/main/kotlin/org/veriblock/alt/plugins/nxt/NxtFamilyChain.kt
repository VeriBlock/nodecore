// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.alt.plugins.nxt

import com.google.gson.Gson
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.ContentType
import org.veriblock.alt.plugins.createHttpClient
import org.veriblock.core.altchain.AltchainPoPEndorsement
import org.veriblock.core.contracts.BlockEvidence
import org.veriblock.core.utilities.createLogger
import org.veriblock.core.utilities.extensions.asHexBytes
import org.veriblock.sdk.alt.ApmInstruction
import org.veriblock.sdk.alt.SecurityInheritingChain
import org.veriblock.sdk.alt.model.Atv
import org.veriblock.sdk.alt.model.PopMempool
import org.veriblock.sdk.alt.model.PopParamsResponse
import org.veriblock.sdk.alt.model.SecurityInheritingBlock
import org.veriblock.sdk.alt.model.SecurityInheritingTransaction
import org.veriblock.sdk.alt.model.SubmitPopResponse
import org.veriblock.sdk.alt.model.Vtb
import org.veriblock.sdk.alt.plugin.PluginConfig
import org.veriblock.sdk.alt.plugin.PluginSpec
import org.veriblock.sdk.models.*

private val logger = createLogger {}

@PluginSpec(name = "NxtFamily", key = "nxt")
class NxtFamilyChain(
    override val key: String,
    configuration: PluginConfig
) : SecurityInheritingChain {

    override val config = NxtConfig(configuration)

    override val id: Long = configuration.id
        ?: error("Failed to load altchain plugin $key: please configure the chain 'id'!")

    override val name: String = configuration.name
        ?: error("Failed to load altchain plugin $key: please configure the chain 'name'!")

    private val httpClient = createHttpClient(
        authConfig = config.auth,
        contentTypes = listOf(ContentType.Application.Json, ContentType.Text.Any)
    )

    init {
        config.checkValidity()
    }

    override suspend fun getBestBlockHeight(): Int {
        logger.trace { "Retrieving best block height..." }
        return httpClient.get<NxtBlockData>("${config.host}/nxt") {
            parameter("requestType", "getBlock")
        }.height
    }

    override suspend fun getBlock(hash: String): SecurityInheritingBlock? {
        TODO("Not yet implemented")
    }

    override suspend fun getBlock(height: Int): SecurityInheritingBlock? {
        TODO("Not yet implemented")
    }

    override suspend fun checkBlockIsOnMainChain(height: Int, blockHeaderToCheck: ByteArray): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun getTransaction(txId: String, blockHash: String?): SecurityInheritingTransaction? {
        TODO("Not yet implemented")
    }

    override fun getPayoutDelay(): Int {
        return config.payoutDelay
    }

    override suspend fun getBestKnownVbkBlockHash(): String {
        TODO("Not yet implemented")
    }

    override suspend fun getPopParams(): PopParamsResponse {
        TODO("Not yet implemented")
    }

    override suspend fun getVbkBlock(hash: String): VeriBlockBlock? {
        TODO("Not yet implemented")
    }

    override suspend fun getBestKnownBtcBlockHash(): String {
        TODO("Not yet implemented")
    }

    override suspend fun getBtcBlock(hash: String): BitcoinBlock? {
        TODO("Not yet implemented")
    }

    override suspend fun getPopMempool(): PopMempool {
        TODO("Not yet implemented")
    }

    override suspend fun getAtv(id: String): Atv? {
        TODO("Not yet implemented")
    }

    override suspend fun getVtb(id: String): Vtb? {
        TODO("Not yet implemented")
    }

    override suspend fun getMiningInstructionByHeight(blockHeight: Int?): ApmInstruction {
        val payoutAddress = config.payoutAddress
            ?: error("Payout address is not configured! Please set 'payoutAddress' in the '$key' configuration section.")

        val actualBlockHeight = blockHeight
        // Retrieve top block height from API if not supplied
            ?: getBestBlockHeight()

        logger.info { "Retrieving mining instruction at height $actualBlockHeight from $key daemon at ${config.host}..." }

        val response: NxtPublicationData = httpClient.get("${config.host}/nxt") {
            parameter("requestType", "getPopData")
            parameter("height", actualBlockHeight)
        }

        if (response.error != null) {
            error("Error calling $key daemon's API: ${response.error} (${response.errorDescription})")
        }

        checkNotNull(response.blockHeader) {
            "Null block header in NXT getPopData response"
        }
        checkNotNull(response.contextInfoContainer) {
            "Null context info in NXT getPopData response"
        }
        check(!response.last_known_veriblock_blocks.isNullOrEmpty()) {
            "Publication data's context (last known VeriBlock blocks) must not be empty!"
        }
        check(!response.last_known_bitcoin_blocks.isNullOrEmpty()) {
            "Publication data's context (last known Bitcoin blocks) must not be empty!"
        }

        val publicationData = PublicationData(
            id,
            response.blockHeader.asHexBytes(),
            payoutAddress.toByteArray(Charsets.US_ASCII),
            response.contextInfoContainer.asHexBytes()
        )
        return ApmInstruction(
                actualBlockHeight,
                publicationData,
                response.last_known_veriblock_blocks.map { it.asHexBytes() },
                response.last_known_bitcoin_blocks.map { it.asHexBytes() }
        )
    }

    override suspend fun submitPopAtv(atv: AltPublication): SubmitPopResponse {
        TODO("Not yet implemented")
    }

    override suspend fun submitPopVbk(block: VeriBlockBlock): SubmitPopResponse {
        TODO("Not yet implemented")
    }

    override suspend fun submitPopVtb(vtb: VeriBlockPublication): SubmitPopResponse {
        TODO("Not yet implemented")
    }

    override fun extractAddressDisplay(addressData: ByteArray): String = TODO()

    override suspend fun extractBlockEvidences(altchainPopEndorsements: List<AltchainPoPEndorsement>): List<BlockEvidence> = TODO()

    override suspend fun getBlockChainInfo(): StateInfo = TODO()

    override suspend fun getMissingBtcBlockHashes(): List<String> {
        TODO("Not yet implemented")
    }

    override fun validatePayoutAddress() {
        TODO("Not yet implemented")
    }

    private fun Any.toJson() = Gson().toJson(this)
}

private data class NxtBlockData(
    val height: Int
)

private abstract class NxtResponse(
    val errorCode: Int? = null,
    val error: String? = null,
    val errorDescription: String? = null
)

private class NxtPublicationData(
    val blockHeader: String? = null,
    val contextInfoContainer: String? = null,
    val last_known_veriblock_blocks: List<String>? = null,
    val last_known_bitcoin_blocks: List<String>? = null,
    errorCode: Int? = null,
    error: String? = null,
    errorDescription: String? = null
) : NxtResponse(errorCode, error, errorDescription)

private data class NxtSubmitData(
    val atv: String,
    val vtb: List<String>
)

private class NxtSubmitResponse(
    val requestProcessingTime: Int? = null,
    val transaction: NxtTransactionData? = null,
    errorCode: Int? = null,
    error: String? = null,
    errorDescription: String? = null
) : NxtResponse(errorCode, error, errorDescription)

private data class NxtTransactionData(
    val senderPublicKey: String,
    val signature: String
)
