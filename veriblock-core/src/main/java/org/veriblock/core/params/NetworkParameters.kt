// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.core.params

import org.veriblock.core.bitcoinj.BitcoinUtilities
import org.veriblock.core.crypto.PreviousBlockVbkHash
import org.veriblock.core.crypto.PreviousKeystoneVbkHash
import org.veriblock.core.crypto.Sha256Hash
import org.veriblock.core.crypto.Sha256Hash.VERIBLOCK_MERKLE_ROOT_LENGTH
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
    var progPowForkHeight: Int? = null
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

    var blockTimeSeconds: Int

    val progPowForkHeight: Int
    var progPowStartTimeEpoch: Long

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
            "testnet_progpow" -> TestNetProgPoWParameters
            "alpha" -> AlphaNetParameters
            "regtest" -> RegTestParameters
            "regtest_progpow" -> RegTestProgPoWParameters
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
                Sha256Hash.wrap(it.previousBlock),
                Sha256Hash.wrap(it.merkleRoot),
                it.timestamp.toInt(),
                it.difficulty.toInt(),
                it.nonce.toInt()
            )
        } ?: template.bitcoinOriginBlock
        protocolVersion = template.protocolVersion
        transactionPrefix = template.transactionPrefix
        minimumDifficulty = template.minimumDifficulty
        powNoRetargeting = template.powNoRetargeting
        blockTimeSeconds = template.blocktimeSeconds
        progPowForkHeight = when (config.network.toLowerCase()) {
            "regtest" -> config.progPowForkHeight ?: template.progPowForkHeight
            else -> template.progPowForkHeight
        }
        progPowStartTimeEpoch = template.progPowStartTimeEpoch
    }

    override fun toString() =
        name
}

/**
 * Network Parameters builder (kotlin builder functions start with upper case, to resemble a constructor)
 */
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

    open val blocktimeSeconds: Int = 30

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
        merkleRoot = Sha256Hash.wrap("A7E5F2B7EC94291767B4D67B4A33682D", VERIBLOCK_MERKLE_ROOT_LENGTH),
        timestamp = 1553497611,
        difficulty = BitcoinUtilities.encodeCompactBits(BigInteger.valueOf(1_000_000_000_000L)).toInt(),
        nonce = 289244493
    )
    override val bitcoinOriginBlockHeight = 568690
    override val bitcoinOriginBlock = BitcoinBlock(
        version = 545259520,
        previousBlock = Sha256Hash.wrap("00000000000000000018f62f3a9fbec9bce00dca759407649d0ac2eaee34e45e"),
        merkleRoot = Sha256Hash.wrap("11a29ab555186bde5ad5b20c54a3dc176ef9105a066df69934dcfe22f09c0984"),
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
        merkleRoot = Sha256Hash.wrap("A2EA7C29EF7915DB412EBD4012A9C617", VERIBLOCK_MERKLE_ROOT_LENGTH),
        timestamp = 1570649416,
        difficulty = BitcoinUtilities.encodeCompactBits(MINIMUM_POW_DIFFICULTY).toInt(),
        nonce = 14304633
    )
    override val bitcoinOriginBlockHeight = 1580892
    override val bitcoinOriginBlock = BitcoinBlock(
        version = 536870912,
        previousBlock = Sha256Hash.wrap("00000000251E9261A15339B4BF0540A44328EC83F3797B9BAC67F47558D5F14E"),
        merkleRoot = Sha256Hash.wrap("CBF519E1DC00F8FFBDC31A6AC3A73109D95890EDD9283EA71AD9BE11639249E9"),
        timestamp = 1570648139,
        difficulty = 486604799,
        nonce = 203968315
    )
    override val protocolVersion: Int = 2

    override val transactionPrefix = 0xAA.toByte()
    override val minimumDifficulty = MINIMUM_POW_DIFFICULTY
    override val powNoRetargeting = false

    override val progPowForkHeight = 872000
    override val progPowStartTimeEpoch: Long = 1600444017L;
}

object TestNetProgPoWParameters : NetworkParametersTemplate() {
    const val NETWORK = "testnet_progpow"
    private val MINIMUM_POW_DIFFICULTY = 100_000_000L.toBigInteger()

    override val name = NETWORK
    override val rpcPort = 10501
    override val p2pPort = 7502

    override val bootstrapDns = "seedtestnetprogpow.veriblock.org"
    override val fileTag = "test-progpow"
    override val genesisBlock = VeriBlockBlock(
        height = 0,
        version = 2.toShort(),
        previousBlock = PreviousBlockVbkHash.EMPTY_HASH,
        previousKeystone = PreviousKeystoneVbkHash.EMPTY_HASH,
        secondPreviousKeystone = PreviousKeystoneVbkHash.EMPTY_HASH,
        merkleRoot = Sha256Hash.wrap("A2EA7C29EF7915DB412EBD4012A9C617", VERIBLOCK_MERKLE_ROOT_LENGTH),
        timestamp = 1570649416,
        difficulty = BitcoinUtilities.encodeCompactBits(MINIMUM_POW_DIFFICULTY).toInt(),
        nonce = 14304633
    )
    override val bitcoinOriginBlockHeight = 1834502
    override val bitcoinOriginBlock = BitcoinBlock(
        version = 536870912,
        previousBlock = Sha256Hash.wrap("000000000000013e99a05bc2dca97efdab0976001eb73e4efabe6d2cded2d011"),
        merkleRoot = Sha256Hash.wrap("aab85d892f28b227557543645c406eb4ca32cf0def54d3c324272891ae8c94a6"),
        timestamp = 1600091168,
        difficulty = 436381186,
        nonce = 707329687L.toInt()
    )
    override val protocolVersion: Int = 2

    override val transactionPrefix = 0xAB.toByte()
    override val minimumDifficulty = MINIMUM_POW_DIFFICULTY
    override val powNoRetargeting = false

    //override val progPowForkHeight = 1 // Only genesis block doesn't use ProgPoW
    override val progPowForkHeight = 100 // First 100 blocks don't use ProgPoW
    override val progPowStartTimeEpoch: Long = 1600091168L
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
        merkleRoot = Sha256Hash.wrap("CF2025EC0EB8A8A325495FEB59500B50", VERIBLOCK_MERKLE_ROOT_LENGTH),
        timestamp = 1551465474,
        difficulty = BitcoinUtilities.encodeCompactBits(MINIMUM_POW_DIFFICULTY).toInt(),
        nonce = 51184863
    )
    override val bitcoinOriginBlockHeight = 1489475
    override val bitcoinOriginBlock = BitcoinBlock(
        version = 536870912,
        previousBlock = Sha256Hash.wrap("00000000000000b345b7bbf29bda1507a679b97967f99a10ab0088899529def7"),
        merkleRoot = Sha256Hash.wrap("5e16e6cef738a2eba1fe7409318e3f558bec325392427aa3d8eaf46b028654f8"),
        timestamp = 1555501858,
        difficulty = 436279940,
        nonce = 2599551022L.toInt()
    )
    override val protocolVersion: Int = 3

    override val transactionPrefix = 0xAA.toByte()
    override val minimumDifficulty = MINIMUM_POW_DIFFICULTY
    override val powNoRetargeting = false
}

object RegTestParameters : NetworkParametersTemplate() {
    const val NETWORK = "regtest"
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
        merkleRoot = Sha256Hash.wrap("84E710F30BB8CFC9AF12622A8F39B917", VERIBLOCK_MERKLE_ROOT_LENGTH),
        timestamp = 1603044490,
        difficulty = BitcoinUtilities.encodeCompactBits(BigInteger.valueOf(1L)).toInt(),
        nonce = 0
    )

    // regtest BTC genesis block:
    override val bitcoinOriginBlockHeight = 0
    override val bitcoinOriginBlock = BitcoinBlock(
        version = 1,
        previousBlock = Sha256Hash.wrap("0000000000000000000000000000000000000000000000000000000000000000"),
        merkleRoot = Sha256Hash.wrap("4a5e1e4baab89f3a32518a88c31bc87f618f76673e2cc77ab2127b7afdeda33b"),
        timestamp = 1296688602,
        difficulty = 0x207fffff,
        nonce = 2
    )
    override val protocolVersion: Int = 3

    override val transactionPrefix = 0xEE.toByte()
    override val minimumDifficulty = MINIMUM_POW_DIFFICULTY
    override val powNoRetargeting = true

    init {
        val hash = genesisBlock.hash.toString()
        check(hash == "7C08C0014554E5DD602FAE9C4D3C02F4D512C8BF3893C977") {
            "Regtest Genesis Block does not match expected hash!"
        }
    }
}

object RegTestProgPoWParameters : NetworkParametersTemplate() {
    const val NETWORK = "regtest_progpow"
    private val MINIMUM_POW_DIFFICULTY: BigInteger = 100_000_000L.toBigInteger()

    override val name = NETWORK
    override val rpcPort = 10503
    override val p2pPort = 7503

    override val bootstrapDns: String? = null
    override val fileTag = "regtest-progpow"
    override val genesisBlock = VeriBlockBlock(
        height = 0,
        version = 2.toShort(),
        previousBlock = PreviousBlockVbkHash.EMPTY_HASH,
        previousKeystone = PreviousKeystoneVbkHash.EMPTY_HASH,
        secondPreviousKeystone = PreviousKeystoneVbkHash.EMPTY_HASH,
        merkleRoot = Sha256Hash.wrap("A2EA7C29EF7915DB412EBD4012A9C617", VERIBLOCK_MERKLE_ROOT_LENGTH),
        timestamp = 1570649416,
        difficulty = BitcoinUtilities.encodeCompactBits(MINIMUM_POW_DIFFICULTY).toInt(),
        nonce = 14304633
    )

    // regtest BTC genesis block:
    override val bitcoinOriginBlockHeight = 0
    override val bitcoinOriginBlock = BitcoinBlock(
        version = 1,
        previousBlock = Sha256Hash.wrap("0000000000000000000000000000000000000000000000000000000000000000"),
        merkleRoot = Sha256Hash.wrap("4a5e1e4baab89f3a32518a88c31bc87f618f76673e2cc77ab2127b7afdeda33b"),
        timestamp = 1296688602,
        difficulty = 0x207fffff,
        nonce = 2
    )
    override val protocolVersion: Int = 3

    override val transactionPrefix = 0xEE.toByte()
    override val minimumDifficulty = MINIMUM_POW_DIFFICULTY
    override val powNoRetargeting = false

    override val progPowForkHeight = 1 // Only genesis block doesn't use vProgPoW
    override val progPowStartTimeEpoch: Long = 1604419537L

    init {
        val hash = genesisBlock.hash.toString()
        check(hash == "00000017EB579EC7D0CDD63379A0615DC3D68032CE248823") {
            "Regtest Genesis Block does not match expected hash!"
        }
    }
}

@JvmField
val defaultMainNetParameters = NetworkParameters { network = MainNetParameters.NETWORK }

@JvmField
val defaultTestNetParameters = NetworkParameters { network = TestNetParameters.NETWORK }

@JvmField
val defaultTestNetProgPoWParameters = NetworkParameters { network = TestNetProgPoWParameters.NETWORK }

@JvmField
val defaultAlphaNetParameters = NetworkParameters { network = AlphaNetParameters.NETWORK }

@JvmField
val defaultRegTestParameters = NetworkParameters { network = RegTestParameters.NETWORK }

@JvmField
val defaultRegTestProgPoWParameters = NetworkParameters { network = RegTestProgPoWParameters.NETWORK }

fun getDefaultNetworkParameters(name: String) = when (name) {
    MainNetParameters.NETWORK -> defaultMainNetParameters
    TestNetParameters.NETWORK -> defaultTestNetParameters
    TestNetProgPoWParameters.NETWORK -> defaultTestNetProgPoWParameters
    AlphaNetParameters.NETWORK -> defaultAlphaNetParameters
    RegTestParameters.NETWORK -> defaultRegTestParameters
    RegTestProgPoWParameters.NETWORK -> defaultRegTestProgPoWParameters
    else -> error("Unknown VBK network: $name")
}
