// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.lite.params

import org.veriblock.core.utilities.Configuration
import java.math.BigInteger

class NetworkConfig(
    val network: String = "mainnet",
    val ip: String? = null,
    val port: Int? = null,
    val certificateChainPath: String? = null,
    val isSsl: Boolean = false,
    val adminPassword: String? = null
)

private val config: NetworkConfig = Configuration.extract("nodecore")
    ?: NetworkConfig()

const val LOCALHOST = "127.0.0.1"

object NetworkParameters {
    val network: String
    val adminHost: String
    val adminPort: Int
    val transactionPrefix: Byte?
    val minimumDifficulty: BigInteger

    var certificateChainPath: String? = config.certificateChainPath
    var isSsl = config.isSsl
    var adminPassword: String? = config.adminPassword

    init {
        val template = when (config.network) {
            "mainnet" -> MainNetParameters
            "testnet" -> TestNetParameters
            "alpha" -> AlphaNetParameters
            else -> error("Invalid network")
        }
        network = template.network
        adminHost = config.ip ?: LOCALHOST
        adminPort = config.port ?: template.defaultAdminPort
        transactionPrefix = template.transactionPrefix
        minimumDifficulty = template.minimumDifficulty
    }
}

sealed class NetworkParametersTemplate {
    abstract val network: String
    abstract val defaultAdminPort: Int
    abstract val transactionPrefix: Byte?
    abstract val minimumDifficulty: BigInteger
}

private object MainNetParameters : NetworkParametersTemplate() {
    const val NETWORK = "MainNet"
    private val MINIMUM_POW_DIFFICULTY = BigInteger.valueOf(900000000000L)

    override val transactionPrefix: Byte?
        get() = null

    override val network: String
        get() = NETWORK

    override val defaultAdminPort: Int
        get() = 10500

    override val minimumDifficulty: BigInteger
        get() = MINIMUM_POW_DIFFICULTY
}

private object TestNetParameters : NetworkParametersTemplate() {
    const val NETWORK = "TestNet"
    private val MINIMUM_POW_DIFFICULTY = BigInteger.valueOf(100_000_000L)

    override val transactionPrefix: Byte?
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

    override val transactionPrefix: Byte?
        get() = 0xAA.toByte()

    override val network: String
        get() = NETWORK

    override val defaultAdminPort: Int
        get() = 10502

    override val minimumDifficulty: BigInteger
        get() = MINIMUM_POW_DIFFICULTY
}
