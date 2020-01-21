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
import com.github.kittinunf.fuel.httpPost
import org.veriblock.core.utilities.Configuration
import org.veriblock.core.utilities.createLogger
import org.veriblock.core.utilities.extensions.asHexBytes
import org.veriblock.core.utilities.extensions.isHex
import org.veriblock.core.utilities.extensions.toHex
import org.veriblock.sdk.alt.ChainConfig
import org.veriblock.sdk.alt.FamilyPluginSpec
import org.veriblock.sdk.alt.PublicationDataWithContext
import org.veriblock.sdk.alt.SecurityInheritingChain
import org.veriblock.sdk.models.AltPublication
import org.veriblock.sdk.models.PublicationData
import org.veriblock.sdk.models.VeriBlockPublication
import org.veriblock.sdk.services.SerializeDeserializeService

private val logger = createLogger {}

class BtcConfig(
    override val host: String = "http://localhost:8332",
    val username: String? = null,
    val password: String? = null,
    val hrps: Map<String, String> = mapOf("mainnet" to "bc", "testnet" to "tb", "regtest" to "bcrt"),
    val network: String = "mainnet",
    val payoutAddress: String? = null,
    override val keystonePeriod: Int = 5,
    override val blockRoundIndices: IntArray = intArrayOf(4, 1, 2, 1, 2),
    override val autoMineRounds: List<Int> = emptyList()
) : ChainConfig()

private data class BtcPublicationData(
    val block_header: String,
    val raw_contextinfocontainer: String,
    val last_known_veriblock_blocks: List<String>,
    val last_known_bitcoin_blocks: List<String>,
    val first_address: String? = null
)

@FamilyPluginSpec(name = "BitcoinFamily", key = "btc")
class BitcoinFamilyChain(
    val id: Long,
    val key: String,
    val name: String
) : SecurityInheritingChain {

    override val config: BtcConfig = Configuration.extract("securityInheriting.$key")
        ?: error("Please configure the securityInheriting.$key section")

    private val payoutAddressScript: ByteArray

    init {
        config.checkValidity()
        checkNotNull(config.payoutAddress) {
            "$key's payoutAddress must be configured!"
        }
        val addressHrp = config.hrps[config.network]
        checkNotNull(addressHrp) {
            "$key's network must be one of ${config.hrps.keys}!"
        }
        payoutAddressScript = when {
            config.payoutAddress.startsWith(addressHrp) -> try {
                SegwitAddressUtility.generatePayoutScriptFromSegwitAddress(config.payoutAddress, addressHrp)
            } catch (e: Exception) {
                error("Invalid segwit address: ${e.message}")
            }
            config.payoutAddress.isHex() ->
                config.payoutAddress.asHexBytes()
            else -> {
                // Find if the user's input matches with any of the existing networks' hrps
                val candidateNetwork = config.hrps.entries.sortedByDescending {
                    it.value.length // Check the longest ones first for a better matching in case of multiple matches
                }.find {
                    config.payoutAddress.startsWith(it.value)
                }?.key
                val extraError = candidateNetwork?.let {
                    " Or was it a $it address? If so, make sure to set 'network' to '$it' in the config"
                } ?: ""
                error(
                    "$key's payoutAddress configuration must be a properly formed hex script or a valid segwit address!" +
                    extraError
                )
            }
        }
    }

    private fun Request.authenticate() = if (config.username != null && config.password != null) {
        authentication().basic(config.username, config.password)
    } else {
        this
    }

    override fun getChainIdentifier(): Long {
        return id
    }

    override fun getBestBlockHeight(): Int {
        logger.debug { "Retrieving best block height..." }
        val jsonBody = JsonRpcRequestBody("getblockcount").toJson()
        return config.host.httpPost()
            .authenticate()
            .body(jsonBody)
            .rpcResponse()
    }

    override fun getPublicationData(blockHeight: Int?): PublicationDataWithContext {
        val actualBlockHeight = blockHeight
            // Retrieve top block height from API if not supplied
            ?: getBestBlockHeight()

        logger.info { "Retrieving publication data at height $actualBlockHeight from $key daemon at ${config.host}..." }
        val jsonBody = JsonRpcRequestBody("getpopdata", listOf(actualBlockHeight)).toJson()
        val response: BtcPublicationData = config.host.httpPost()
            .authenticate()
            .body(jsonBody)
            .rpcResponse()

        val publicationData = PublicationData(
            getChainIdentifier(),
            response.block_header.asHexBytes(),
            payoutAddressScript,
            response.raw_contextinfocontainer.asHexBytes()
        )
        if (response.last_known_veriblock_blocks.isEmpty()) {
            error("Publication data's context (last known VeriBlock blocks) must not be empty!")
        }
        return PublicationDataWithContext(
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
            veriBlockPublications.map { it.transaction }.flatMap { it.blocks }.map { SerializeDeserializeService.serialize(it) },
            veriBlockPublications.flatMap { it.blocks }.map { SerializeDeserializeService.serialize(it).toHex() }
        )).toJson()

        return config.host.httpPost()
            .authenticate()
            .body(jsonBody)
            .rpcResponse()
    }
}
