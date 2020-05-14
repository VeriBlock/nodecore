// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.core.params

import org.veriblock.core.crypto.Sha256Hash
import org.veriblock.sdk.models.BitcoinBlock
import org.veriblock.sdk.models.VeriBlockBlock
import org.veriblock.sdk.services.SerializeDeserializeService
import java.math.BigInteger
import java.util.Base64

class NetworkConfig(
    val network: String = "mainnet",
    val host: String? = null,
    val port: Int? = null,
    val certificateChainPath: String? = null,
    val isSsl: Boolean = false,
    val adminPassword: String? = null,
    val bitcoinOriginBlock: BitcoinOriginBlockConfig? = null
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
            "regtest" -> RegTestParameters
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
    }
}

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
}

object MainNetParameters : NetworkParametersTemplate() {
    const val NETWORK = "mainnet"
    private val MINIMUM_POW_DIFFICULTY = BigInteger.valueOf(900000000000L)

    override val name = NETWORK
    override val rpcPort = 10500
    override val p2pPort = 7500

    override val bootstrapDns = "seed.veriblock.org"
    override val fileTag = null
    override val genesisBlock: VeriBlockBlock = SerializeDeserializeService.parseVeriBlockBlock(Base64.getDecoder().decode(
        "AAAAAAACAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAp+Xyt+yUKRdntNZ7SjNoLVyYfgsGAOjUET2FTQ=="
    ))
    override val bitcoinOriginBlockHeight = 568690
    override val bitcoinOriginBlock = BitcoinBlock(
        545259520,
        Sha256Hash.wrap("00000000000000000018f62f3a9fbec9bce00dca759407649d0ac2eaee34e45e"),
        Sha256Hash.wrap("11a29ab555186bde5ad5b20c54a3dc176ef9105a066df69934dcfe22f09c0984"),
        1553493015,
        388767596,
        2328158480L.toInt()
    )
    override val protocolVersion: Int = 3

    override val transactionPrefix = null
    override val minimumDifficulty = MINIMUM_POW_DIFFICULTY
    override val powNoRetargeting = false
}

object TestNetParameters : NetworkParametersTemplate() {
    const val NETWORK = "testnet"
    private val MINIMUM_POW_DIFFICULTY = BigInteger.valueOf(100_000_000L)

    override val name = NETWORK
    override val rpcPort = 10501
    override val p2pPort = 7501

    override val bootstrapDns = "seedtestnet.veriblock.org"
    override val fileTag = "test"
    override val genesisBlock: VeriBlockBlock = SerializeDeserializeService.parseVeriBlockBlock(Base64.getDecoder().decode(
        "AAAAAAACAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAoup8Ke95FdtBLr1AEqnGF12eNUgEBfXhANpFeQ=="
    ))
    override val bitcoinOriginBlockHeight = 1580892
    override val bitcoinOriginBlock = BitcoinBlock(
        536870912,
        Sha256Hash.wrap("00000000251E9261A15339B4BF0540A44328EC83F3797B9BAC67F47558D5F14E"),
        Sha256Hash.wrap("CBF519E1DC00F8FFBDC31A6AC3A73109D95890EDD9283EA71AD9BE11639249E9"),
        1570648139,
        486604799,
        203968315
    )
    override val protocolVersion: Int = 2

    override val transactionPrefix = 0xAA.toByte()
    override val minimumDifficulty = MINIMUM_POW_DIFFICULTY
    override val powNoRetargeting = false
}

object AlphaNetParameters : NetworkParametersTemplate() {
    const val NETWORK = "alpha"
    private val MINIMUM_POW_DIFFICULTY = BigInteger.valueOf(9_999_872L)

    override val name = NETWORK
    override val rpcPort = 10502
    override val p2pPort = 7502

    override val bootstrapDns = null
    override val fileTag = "alpha"
    override val genesisBlock: VeriBlockBlock = SerializeDeserializeService.parseVeriBlockBlock(Base64.getDecoder().decode(
        "AAAAAAABAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAzyAl7A64qKMlSV/rWVALUFx5fAIEBfXhAw0E3w=="
    ))
    override val bitcoinOriginBlockHeight = 1489475
    override val bitcoinOriginBlock = BitcoinBlock(
        536870912,
        Sha256Hash.wrap("00000000000000b345b7bbf29bda1507a679b97967f99a10ab0088899529def7"),
        Sha256Hash.wrap("5e16e6cef738a2eba1fe7409318e3f558bec325392427aa3d8eaf46b028654f8"),
        1555501858,
        436279940,
        2599551022L.toInt()
    )
    override val protocolVersion: Int = 3

    override val transactionPrefix = 0xAA.toByte()
    override val minimumDifficulty = MINIMUM_POW_DIFFICULTY
    override val powNoRetargeting = false
}

object RegTestParameters : NetworkParametersTemplate() {
    const val NETWORK = "regtest"

    override val name = NETWORK
    override val rpcPort = 10503
    override val p2pPort = 7503

    override val bootstrapDns = null
    override val fileTag = "regtest"
    override val genesisBlock: VeriBlockBlock = SerializeDeserializeService.parseVeriBlockBlock(Base64.getDecoder().decode(
        "AAAAAAABAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAzyAl7A64qKMlSV/rWVALUFx5fAIEBfXhAw0E3w=="
    ))
    override val bitcoinOriginBlockHeight = 1489475
    override val bitcoinOriginBlock = BitcoinBlock(
        536870912,
        Sha256Hash.wrap("00000000000000b345b7bbf29bda1507a679b97967f99a10ab0088899529def7"),
        Sha256Hash.wrap("5e16e6cef738a2eba1fe7409318e3f558bec325392427aa3d8eaf46b028654f8"),
        1555501858,
        436279940,
        2599551022L.toInt()
    )
    override val protocolVersion: Int = 3

    override val transactionPrefix = 0xBB.toByte()
    override val minimumDifficulty = BigInteger.ONE
    override val powNoRetargeting = true
}

@JvmField
val defaultMainNetParameters = NetworkParameters(NetworkConfig(MainNetParameters.NETWORK))
@JvmField
val defaultTestNetParameters = NetworkParameters(NetworkConfig(TestNetParameters.NETWORK))
@JvmField
val defaultAlphaNetParameters = NetworkParameters(NetworkConfig(AlphaNetParameters.NETWORK))
@JvmField
val defaultRegTestParameters = NetworkParameters(NetworkConfig(RegTestParameters.NETWORK))

fun getDefaultNetworkParameters(name: String) = when (name) {
    MainNetParameters.NETWORK -> defaultMainNetParameters
    TestNetParameters.NETWORK -> defaultTestNetParameters
    AlphaNetParameters.NETWORK -> defaultAlphaNetParameters
    RegTestParameters.NETWORK -> defaultRegTestParameters
    else -> error("Unknown VBK network: $name")
}
