// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.alt.plugins.nxt

import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.extensions.authentication
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import com.google.gson.Gson
import org.veriblock.alt.plugins.util.httpResponse
import org.veriblock.core.altchain.AltchainPoPEndorsement
import org.veriblock.core.contracts.BlockEndorsement
import org.veriblock.core.utilities.createLogger
import org.veriblock.core.utilities.extensions.asHexBytes
import org.veriblock.core.utilities.extensions.toHex
import org.veriblock.sdk.alt.ApmInstruction
import org.veriblock.sdk.alt.SecurityInheritingChain
import org.veriblock.sdk.alt.model.SecurityInheritingBlock
import org.veriblock.sdk.alt.model.SecurityInheritingTransaction
import org.veriblock.sdk.alt.plugin.PluginConfig
import org.veriblock.sdk.alt.plugin.PluginSpec
import org.veriblock.sdk.models.AltPublication
import org.veriblock.sdk.models.PublicationData
import org.veriblock.sdk.models.VeriBlockPublication
import org.veriblock.sdk.services.SerializeDeserializeService

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

    init {
        config.checkValidity()
    }

    private fun Request.authenticate() = if (config.username != null && config.password != null) {
        authentication().basic(config.username, config.password)
    } else {
        this
    }

    override fun getBestBlockHeight(): Int {
        logger.debug { "Retrieving best block height..." }
        return "${config.host}/nxt".httpGet(listOf(
            "requestType" to "getBlock"
        )).authenticate().httpResponse<NxtBlockData>().height
    }

    override fun getBlock(hash: String): SecurityInheritingBlock? {
        TODO("Not yet implemented")
    }

    override fun getBlock(height: Int): SecurityInheritingBlock? {
        TODO("Not yet implemented")
    }

    override fun checkBlockIsOnMainChain(height: Int, blockHeaderToCheck: ByteArray): Boolean {
        TODO("Not yet implemented")
    }

    override fun getTransaction(txId: String): SecurityInheritingTransaction? {
        TODO("Not yet implemented")
    }

    override fun getPayoutInterval(): Int {
        TODO("TBD")
    }

    override fun getMiningInstruction(blockHeight: Int?): ApmInstruction {
        val payoutAddress = config.payoutAddress
                ?: error("Payout address is not configured! Please set 'payoutAddress' in the '$key' configuration section.")

        val actualBlockHeight = blockHeight
        // Retrieve top block height from API if not supplied
                ?: getBestBlockHeight()

        logger.info { "Retrieving mining instruction at height $actualBlockHeight from $key daemon at ${config.host}..." }
        val response: NxtPublicationData = "${config.host}/nxt".httpGet(
                listOf(
                "requestType" to "getPopData",
            "height" to actualBlockHeight
        )).authenticate().httpResponse()

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

    override fun submit(proofOfProof: AltPublication, veriBlockPublications: List<VeriBlockPublication>): String {
        logger.info { "Submitting PoP and VeriBlock publications to $key daemon at ${config.host}..." }
        val jsonBody = NxtSubmitData(
            atv = SerializeDeserializeService.serialize(proofOfProof).toHex(),
            vtb = veriBlockPublications.map { SerializeDeserializeService.serialize(it).toHex() }
        ).toJson()
        val submitResponse: NxtSubmitResponse = "${config.host}/nxt".httpPost(listOf(
            "requestType" to "submitPop"
        )
        ).authenticate().header("content-type", "application/json").body(jsonBody).httpResponse()

        if (submitResponse.error != null) {
            error("Error calling $key daemon's API: ${submitResponse.error} (${submitResponse.errorDescription})")
        }

        return submitResponse.transaction?.signature
            ?: error("Unable to retrieve $key's submission response data")
    }

    override fun updateContext(veriBlockPublications: List<VeriBlockPublication>): String = TODO()

    override fun extractBlockEndorsement(altchainPopEndorsement: AltchainPoPEndorsement): BlockEndorsement = TODO()

    override fun isConnected(): Boolean = TODO()

    override fun isSynchronized(): Boolean = TODO()
    
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
