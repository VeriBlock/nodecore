// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.alt.plugins

import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.extensions.authentication
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import com.google.gson.Gson
import org.veriblock.core.utilities.Configuration
import org.veriblock.core.utilities.createLogger
import org.veriblock.core.utilities.extensions.asHexBytes
import org.veriblock.core.utilities.extensions.toHex
import org.veriblock.sdk.AltPublication
import org.veriblock.sdk.PublicationData
import org.veriblock.sdk.VeriBlockPublication
import org.veriblock.sdk.alt.FamilyPluginSpec
import org.veriblock.sdk.alt.PublicationDataWithContext
import org.veriblock.sdk.alt.SecurityInheritingChain
import org.veriblock.sdk.services.SerializeDeserializeService

private val logger = createLogger {}

class NxtConfig(
    val host: String = "http://localhost:8332",
    val username: String? = null,
    val password: String? = null,
    val autoMine: NxtAutoMineConfig? = null,
    val payoutAddress: String? = null
)

class NxtAutoMineConfig(
    val round1: Boolean = false,
    val round2: Boolean = false,
    val round3: Boolean = false,
    val round4: Boolean = false
)

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

@FamilyPluginSpec(name = "NxtFamily", key = "nxt")
class NxtFamilyChain(
    val id: Long,
    val key: String,
    val name: String
) : SecurityInheritingChain {

    private val config = Configuration.extract("securityInheriting.$key") ?: NxtConfig()

    private fun Request.authenticate() = if (config.username != null && config.password != null) {
        authentication().basic(config.username, config.password)
    } else {
        this
    }

    override fun getChainIdentifier(): Long {
        return id
    }

    override fun shouldAutoMine(): Boolean {
        return config.autoMine != null && (config.autoMine.round1 || config.autoMine.round2 || config.autoMine.round3 || config.autoMine.round4)
    }

    override fun shouldAutoMine(blockHeight: Int): Boolean {
        // TODO proper round calculation for each alt
        val round = blockHeight % 5
        return config.autoMine != null && (
            (round == 1 && config.autoMine.round1) ||
            (round == 2 && config.autoMine.round2) ||
            (round == 3 && config.autoMine.round3) ||
            (round == 4 && config.autoMine.round4)
        )
    }

    override fun getBestBlockHeight(): Int {
        logger.debug { "Retrieving best block height..." }
        return "${config.host}/nxt".httpGet(listOf(
            "requestType" to "getBlock"
        )).authenticate().httpResponse<NxtBlockData>().height
    }

    override fun getPublicationData(blockHeight: Int?): PublicationDataWithContext {
        val payoutAddress = config.payoutAddress
            ?: error("Payout address is not configured! Please set 'payoutAddress' in the '$key' configuration section.")

        val actualBlockHeight = blockHeight
            // Retrieve top block height from API if not supplied
            ?: getBestBlockHeight()

        logger.info { "Retrieving publication data at height $actualBlockHeight from $key daemon at ${config.host}..." }
        val response: NxtPublicationData = "${config.host}/nxt".httpGet(listOf(
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
            getChainIdentifier(),
            response.blockHeader.asHexBytes(),
            payoutAddress.toByteArray(Charsets.US_ASCII),
            response.contextInfoContainer.asHexBytes()
        )
        return PublicationDataWithContext(
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
        )).authenticate().header("content-type", "application/json").body(jsonBody).httpResponse()

        if (submitResponse.error != null) {
            error("Error calling $key daemon's API: ${submitResponse.error} (${submitResponse.errorDescription})")
        }

        return submitResponse.transaction?.signature
            ?: error("Unable to retrieve $key's submission response data")
    }

    private fun Any.toJson() = Gson().toJson(this)
}
