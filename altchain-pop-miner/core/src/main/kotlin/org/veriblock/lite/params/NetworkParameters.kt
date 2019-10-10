// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.lite.params

import org.veriblock.sdk.Configuration
import org.veriblock.sdk.VeriBlockBlock

import java.math.BigInteger

class NetworkConfig(
    val network: String = "alpha",
    val ip: String? = null,
    val port: Int? = null
)

private val config: NetworkConfig = Configuration.extract("nodecore")
    ?: NetworkConfig()

const val LOCALHOST = "127.0.0.1"

object NetworkParameters {
    val network: String
    val adminHost: String
    val adminPort: Int
    val transactionPrefix: Byte
    val minimumDifficulty: BigInteger

    // TODO
    var certificateChainPath: String? = null
    var isSsl = false
    var adminPassword: String? = null

    init {
        val template = when (config.network) {
            "testnet" -> TestNetParameters
            else -> AlphaNetParameters
        }
        network = template.network
        adminHost = config.ip ?: LOCALHOST
        adminPort = config.port ?: template.defaultAdminPort
        transactionPrefix = template.transactionPrefix
        minimumDifficulty = template.minimumDifficulty
    }

    fun getGenesisBlock(): VeriBlockBlock? = null
}

sealed class NetworkParametersTemplate {
    abstract val network: String
    abstract val defaultAdminPort: Int
    abstract val transactionPrefix: Byte
    abstract val minimumDifficulty: BigInteger
}

private object TestNetParameters : NetworkParametersTemplate() {
    const val NETWORK = "TestNet"
    private val MINIMUM_POW_DIFFICULTY = BigInteger.valueOf(100_000_000L)

    override val transactionPrefix: Byte
        get() = 0xAA.toByte()

    override val network: String
        get() = NETWORK

    override val defaultAdminPort: Int
        get() = 10501

    override val minimumDifficulty: BigInteger
        get() = MINIMUM_POW_DIFFICULTY
}

private object AlphaNetParameters : NetworkParametersTemplate() {
    const val NETWORK = "Alpha"
    private val MINIMUM_POW_DIFFICULTY = BigInteger.valueOf(9_999_872L)

    override val transactionPrefix: Byte
        get() = 0xAA.toByte()

    override val network: String
        get() = NETWORK

    override val defaultAdminPort: Int
        get() = 10502

    override val minimumDifficulty: BigInteger
        get() = MINIMUM_POW_DIFFICULTY
}
