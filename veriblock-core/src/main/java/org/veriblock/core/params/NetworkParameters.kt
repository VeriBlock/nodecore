// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.core.params

import org.veriblock.core.bitcoinj.BitcoinUtilities
import org.veriblock.core.crypto.PreviousBlockVbkHash
import org.veriblock.core.crypto.PreviousKeystoneVbkHash
import org.veriblock.core.crypto.asBtcHash
import org.veriblock.core.crypto.asMerkleRoot
import org.veriblock.core.crypto.asTruncatedMerkleRoot
import org.veriblock.sdk.models.BitcoinBlock
import org.veriblock.sdk.models.VeriBlockBlock
import java.math.BigInteger

class NetworkConfig(
    var network: String = "mainnet",
    var host: String? = null,
    var port: Int? = null,
    var certificateChainPath: String? = null,
    var isSsl: Boolean = false,
    var adminPassword: String? = null,
    var bitcoinOriginBlock: BitcoinOriginBlockConfig? = null,
    var protocolVersion: Int? = null,
    var transactionPrefix: Byte? = null,
    var minimumDifficulty: BigInteger? = null,
    var powNoRetargeting: Boolean? = null,
    var blockTimeSeconds: Int? = null,
    var progPowForkHeight: Int? = null,
    var progPowStartTimeEpoch: Long? = null
)

class BitcoinOriginBlockConfig(
    val height: Int,
    val version: Int,
    val previousBlock: String,
    val merkleRoot: String,
    val timestamp: Long,
    val difficulty: Long,
    val nonce: Long
)

const val LOCALHOST = "127.0.0.1"

class NetworkParameters(
    config: NetworkConfig
) {
    val name: String
    val rpcHost: String
    val rpcPort: Int
    val p2pPort: Int

    val bootstrapDns: String?
    val fileTag: String?
    val genesisBlock: VeriBlockBlock
    val bitcoinOriginBlockHeight: Int
    val bitcoinOriginBlock: BitcoinBlock
    val protocolVersion: Int

    val transactionPrefix: Byte?
    val minimumDifficulty: BigInteger
    val powNoRetargeting: Boolean

    val blockTimeSeconds: Int

    val progPowForkHeight: Int
    val progPowStartTimeEpoch: Long

    var certificateChainPath: String? = config.certificateChainPath
    var isSsl = config.isSsl
    var adminPassword: String? = config.adminPassword

    val databaseName: String
        get() = if (fileTag == null) {
            "database.db"
        } else {
            "database-$fileTag.db"
        }

    init {
        val template = when (config.network.toLowerCase()) {
            "mainnet" -> MainNetParameters
            "testnet" -> TestNetParameters
            "alpha" -> AlphaNetParameters
            "regtest" -> if (config.progPowForkHeight == 0) {
                RegTestProgPowParameters
            } else {
                RegTestParameters
            }
            else -> error("Invalid network")
        }
        name = template.name
        rpcHost = config.host ?: LOCALHOST
        rpcPort = config.port ?: template.rpcPort
        p2pPort = template.p2pPort
        bootstrapDns = template.bootstrapDns
        fileTag = template.fileTag
        genesisBlock = template.genesisBlock
        bitcoinOriginBlockHeight = config.bitcoinOriginBlock?.height ?: template.bitcoinOriginBlockHeight
        bitcoinOriginBlock = config.bitcoinOriginBlock?.let {
            BitcoinBlock(
                it.version,
                it.previousBlock.asBtcHash(),
                it.merkleRoot.asMerkleRoot(),
                it.timestamp.toInt(),
                it.difficulty.toInt(),
                it.nonce.toInt()
            )
        } ?: template.bitcoinOriginBlock
        protocolVersion = config.protocolVersion ?: template.protocolVersion
        transactionPrefix = config.transactionPrefix ?: template.transactionPrefix
        minimumDifficulty = config.minimumDifficulty ?: template.minimumDifficulty
        powNoRetargeting = config.powNoRetargeting ?: template.powNoRetargeting
        blockTimeSeconds = config.blockTimeSeconds ?: template.blockTimeSeconds
        progPowForkHeight = config.progPowForkHeight ?: template.progPowForkHeight
        progPowStartTimeEpoch = config.progPowStartTimeEpoch ?: template.progPowStartTimeEpoch
    }

    override fun toString() =
        name
}

/**
 * Network Parameters builder
 */
@Suppress("FunctionName")
inline fun NetworkParameters(builder: NetworkConfig.() -> Unit = {}) =
    NetworkParameters(NetworkConfig().apply(builder))

sealed class NetworkParametersTemplate {
    abstract val name: String
    abstract val rpcPort: Int
    abstract val p2pPort: Int

    abstract val bootstrapDns: String?
    abstract val fileTag: String?
    abstract val genesisBlock: VeriBlockBlock
    abstract val bitcoinOriginBlockHeight: Int
    abstract val bitcoinOriginBlock: BitcoinBlock
    abstract val protocolVersion: Int

    abstract val transactionPrefix: Byte?
    abstract val minimumDifficulty: BigInteger
    abstract val powNoRetargeting: Boolean

    open val blockTimeSeconds: Int = 30

    open val progPowForkHeight: Int = Int.MAX_VALUE // Default of "never"
    open val progPowStartTimeEpoch: Long = Long.MAX_VALUE // Default of "never"
}

object MainNetParameters : NetworkParametersTemplate() {
    const val NETWORK = "mainnet"
    private val MINIMUM_POW_DIFFICULTY = 90_000_000_000L.toBigInteger()

    override val name = NETWORK
    override val rpcPort = 10500
    override val p2pPort = 7500

    override val bootstrapDns = "seed.veriblock.org"
    override val fileTag: String? = null
    override val genesisBlock = VeriBlockBlock(
        height = 0,
        version = 2.toShort(),
        previousBlock = PreviousBlockVbkHash.EMPTY_HASH,
        previousKeystone = PreviousKeystoneVbkHash.EMPTY_HASH,
        secondPreviousKeystone = PreviousKeystoneVbkHash.EMPTY_HASH,
        merkleRoot = "A7E5F2B7EC94291767B4D67B4A33682D".asTruncatedMerkleRoot(),
        timestamp = 1553497611,
        difficulty = BitcoinUtilities.encodeCompactBits(BigInteger.valueOf(1_000_000_000_000L)).toInt(),
        nonce = 289244493
    )
    override val bitcoinOriginBlockHeight = 568690
    override val bitcoinOriginBlock = BitcoinBlock(
        version = 545259520,
        previousBlock = "00000000000000000018f62f3a9fbec9bce00dca759407649d0ac2eaee34e45e".asBtcHash(),
        merkleRoot = "11a29ab555186bde5ad5b20c54a3dc176ef9105a066df69934dcfe22f09c0984".asMerkleRoot(),
        timestamp = 1553493015,
        difficulty = 388767596,
        nonce = 2328158480L.toInt()
    )
    override val protocolVersion: Int = 3

    override val transactionPrefix: Byte? = null
    override val minimumDifficulty = MINIMUM_POW_DIFFICULTY
    override val powNoRetargeting = false

    override val progPowForkHeight = 1512000
    override val progPowStartTimeEpoch: Long = 1600716052L
}

object TestNetParameters : NetworkParametersTemplate() {
    const val NETWORK = "testnet"
    private val MINIMUM_POW_DIFFICULTY = 100_000_000L.toBigInteger()

    override val name = NETWORK
    override val rpcPort = 10501
    override val p2pPort = 7501

    override val bootstrapDns = "seedtestnet.veriblock.org"
    override val fileTag = "test"
    override val genesisBlock = VeriBlockBlock(
        height = 0,
        version = 2.toShort(),
        previousBlock = PreviousBlockVbkHash.EMPTY_HASH,
        previousKeystone = PreviousKeystoneVbkHash.EMPTY_HASH,
        secondPreviousKeystone = PreviousKeystoneVbkHash.EMPTY_HASH,
        merkleRoot = "A2EA7C29EF7915DB412EBD4012A9C617".asTruncatedMerkleRoot(),
        timestamp = 1570649416,
        difficulty = BitcoinUtilities.encodeCompactBits(MINIMUM_POW_DIFFICULTY).toInt(),
        nonce = 14304633
    )
    override val bitcoinOriginBlockHeight = 1580892
    override val bitcoinOriginBlock = BitcoinBlock(
        version = 536870912,
        previousBlock = "00000000251E9261A15339B4BF0540A44328EC83F3797B9BAC67F47558D5F14E".asBtcHash(),
        merkleRoot = "CBF519E1DC00F8FFBDC31A6AC3A73109D95890EDD9283EA71AD9BE11639249E9".asMerkleRoot(),
        timestamp = 1570648139,
        difficulty = 486604799,
        nonce = 203968315
    )
    override val protocolVersion: Int = 2

    override val transactionPrefix = 0xAA.toByte()
    override val minimumDifficulty = MINIMUM_POW_DIFFICULTY
    override val powNoRetargeting = false

    override val progPowForkHeight = 872000
    override val progPowStartTimeEpoch: Long = 1600444017L
}

object AlphaNetParameters : NetworkParametersTemplate() {
    const val NETWORK = "alpha"
    private val MINIMUM_POW_DIFFICULTY = 9_999_872L.toBigInteger()

    override val name = NETWORK
    override val rpcPort = 10502
    override val p2pPort = 7502

    override val bootstrapDns: String? = null
    override val fileTag = "alpha"
    override val genesisBlock = VeriBlockBlock(
        height = 0,
        version = 1.toShort(),
        previousBlock = PreviousBlockVbkHash.EMPTY_HASH,
        previousKeystone = PreviousKeystoneVbkHash.EMPTY_HASH,
        secondPreviousKeystone = PreviousKeystoneVbkHash.EMPTY_HASH,
        merkleRoot = "CF2025EC0EB8A8A325495FEB59500B50".asTruncatedMerkleRoot(),
        timestamp = 1551465474,
        difficulty = BitcoinUtilities.encodeCompactBits(MINIMUM_POW_DIFFICULTY).toInt(),
        nonce = 51184863
    )
    override val bitcoinOriginBlockHeight = 1489475
    override val bitcoinOriginBlock = BitcoinBlock(
        version = 536870912,
        previousBlock = "00000000000000b345b7bbf29bda1507a679b97967f99a10ab0088899529def7".asBtcHash(),
        merkleRoot = "5e16e6cef738a2eba1fe7409318e3f558bec325392427aa3d8eaf46b028654f8".asMerkleRoot(),
        timestamp = 1555501858,
        difficulty = 436279940,
        nonce = 2599551022L.toInt()
    )
    override val protocolVersion: Int = 3

    override val transactionPrefix = 0xAA.toByte()
    override val minimumDifficulty = MINIMUM_POW_DIFFICULTY
    override val powNoRetargeting = false
}

sealed class RegTestParametersTemplate : NetworkParametersTemplate() {
    val NETWORK = "regtest"
    private val MINIMUM_POW_DIFFICULTY: BigInteger = BigInteger.ONE

    override val name = NETWORK
    override val rpcPort = 10503
    override val p2pPort = 7503

    override val bootstrapDns: String? = null
    override val fileTag = "regtest"
    override val genesisBlock = VeriBlockBlock(
        height = 0,
        version = 2.toShort(),
        previousBlock = PreviousBlockVbkHash.EMPTY_HASH,
        previousKeystone = PreviousKeystoneVbkHash.EMPTY_HASH,
        secondPreviousKeystone = PreviousKeystoneVbkHash.EMPTY_HASH,
        merkleRoot = "84E710F30BB8CFC9AF12622A8F39B917".asTruncatedMerkleRoot(),
        timestamp = 1603044490,
        difficulty = BitcoinUtilities.encodeCompactBits(BigInteger.valueOf(1L)).toInt(),
        nonce = 0
    )

    // regtest BTC genesis block:
    override val bitcoinOriginBlockHeight = 0
    override val bitcoinOriginBlock = BitcoinBlock(
        version = 1,
        previousBlock = "0000000000000000000000000000000000000000000000000000000000000000".asBtcHash(),
        merkleRoot = "4a5e1e4baab89f3a32518a88c31bc87f618f76673e2cc77ab2127b7afdeda33b".asMerkleRoot(),
        timestamp = 1296688602,
        difficulty = 0x207fffff,
        nonce = 2
    )
    override val protocolVersion: Int = 3

    override val transactionPrefix = 0xEE.toByte()
    override val minimumDifficulty = MINIMUM_POW_DIFFICULTY
    override val powNoRetargeting = true
}

object RegTestParameters : RegTestParametersTemplate()

private object RegTestProgPowParameters : RegTestParametersTemplate() {
    override val genesisBlock = VeriBlockBlock(
        height = 0,
        version = 2.toShort(),
        previousBlock = PreviousBlockVbkHash.EMPTY_HASH,
        previousKeystone = PreviousKeystoneVbkHash.EMPTY_HASH,
        secondPreviousKeystone = PreviousKeystoneVbkHash.EMPTY_HASH,
        merkleRoot = "80D7178046D25CA9AD283C5AF587A7C5".asTruncatedMerkleRoot(),
        timestamp = 1603044490,
        difficulty = BitcoinUtilities.encodeCompactBits(BigInteger.valueOf(1L)).toInt(),
        nonce = 0
    )

    override val progPowForkHeight = 0 // enabled from genesis block
}

@JvmField
val defaultMainNetParameters = NetworkParameters { network = MainNetParameters.NETWORK }

@JvmField
val defaultTestNetParameters = NetworkParameters { network = TestNetParameters.NETWORK }

@JvmField
val defaultAlphaNetParameters = NetworkParameters { network = AlphaNetParameters.NETWORK }

@JvmField
val defaultRegTestParameters = NetworkParameters { network = RegTestParameters.NETWORK }

@JvmField
val defaultRegTestProgPowParameters = NetworkParameters {
    network = RegTestParameters.NETWORK
    progPowForkHeight = 0
    progPowStartTimeEpoch = System.currentTimeMillis() / 1000
}

val allDefaultNetworkParameters = listOf(
    defaultMainNetParameters,
    defaultTestNetParameters,
    defaultAlphaNetParameters,
    defaultRegTestParameters
)

fun getDefaultNetworkParameters(name: String, progPowGenesis: Boolean = false) = when (name) {
    MainNetParameters.NETWORK -> defaultMainNetParameters
    TestNetParameters.NETWORK -> defaultTestNetParameters
    AlphaNetParameters.NETWORK -> defaultAlphaNetParameters
    RegTestParameters.NETWORK -> {
        if (!progPowGenesis) {
            defaultRegTestParameters
        } else {
            defaultRegTestProgPowParameters
        }
    }
    else -> error("Unknown VBK network: $name")
}
