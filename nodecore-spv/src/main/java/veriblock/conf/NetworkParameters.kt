// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package veriblock.conf

import org.veriblock.sdk.models.BitcoinBlock
import org.veriblock.sdk.models.Sha256Hash
import org.veriblock.sdk.models.VeriBlockBlock
import org.veriblock.sdk.services.SerializeDeserializeService
import java.util.Base64

const val LOCALHOST = "127.0.0.1"

/**
 * Configure parameters that depends on a network. (test | main | alpha)
 */
abstract class NetworkParameters {
    abstract val networkName: String

    abstract val adminHost: String
    abstract val adminPort: Int

    abstract val p2pPort: Int

    abstract val bootstrapDns: String?
    abstract val databaseName: String

    abstract val genesisBlock: VeriBlockBlock

    abstract val bitcoinOriginBlock: BitcoinBlock

    abstract val protocolVersion: Int
}

object MainNetParameters : NetworkParameters() {
    override val networkName = "mainnet"
    override val adminHost = LOCALHOST
    override val adminPort = 10500
    override val p2pPort = 7500
    override val bootstrapDns = "seed.veriblock.org"
    override val databaseName = "database.sqlite"
    override val genesisBlock: VeriBlockBlock = SerializeDeserializeService.parseVeriBlockBlock(Base64.getDecoder().decode(
        "AAAAAAACAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAp+Xyt+yUKRdntNZ7SjNoLVyYfgsGAOjUET2FTQ=="
    ))
    override val bitcoinOriginBlock = BitcoinBlock(
        545259520,
        Sha256Hash.wrap("00000000000000000018f62f3a9fbec9bce00dca759407649d0ac2eaee34e45e"),
        Sha256Hash.wrap("11a29ab555186bde5ad5b20c54a3dc176ef9105a066df69934dcfe22f09c0984"),
        1553493015,
        388767596,
        2328158480L.toInt()
    )
    override val protocolVersion: Int = 3
}

object TestNetParameters : NetworkParameters() {
    override val networkName = "testnet"
    override val adminHost = LOCALHOST
    override val adminPort = 10501
    override val p2pPort = 7501
    override val bootstrapDns = "seedtestnet.veriblock.org"
    override val databaseName = "database-test.sqlite"
    override val genesisBlock: VeriBlockBlock = SerializeDeserializeService.parseVeriBlockBlock(Base64.getDecoder().decode(
        "AAAAAAACAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAoup8Ke95FdtBLr1AEqnGF12eNUgEBfXhANpFeQ=="
    ))
    override val bitcoinOriginBlock = BitcoinBlock(
        536870912,
        Sha256Hash.wrap("00000000251E9261A15339B4BF0540A44328EC83F3797B9BAC67F47558D5F14E"),
        Sha256Hash.wrap("CBF519E1DC00F8FFBDC31A6AC3A73109D95890EDD9283EA71AD9BE11639249E9"),
        1570648139,
        486604799,
        203968315
    )
    override val protocolVersion: Int = 2
}

object AlphaNetParameters : NetworkParameters() {
    override val networkName = "alphanet"
    override val adminHost = LOCALHOST
    override val adminPort = 10502
    override val p2pPort = 7502
    override val bootstrapDns = null
    override val databaseName = "database-alpha.sqlite"
    override val genesisBlock: VeriBlockBlock = SerializeDeserializeService.parseVeriBlockBlock(Base64.getDecoder().decode(
        "AAAAAAABAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAzyAl7A64qKMlSV/rWVALUFx5fAIEBfXhAw0E3w=="
    ))
    override val bitcoinOriginBlock = BitcoinBlock(
        536870912,
        Sha256Hash.wrap("00000000000000b345b7bbf29bda1507a679b97967f99a10ab0088899529def7"),
        Sha256Hash.wrap("5e16e6cef738a2eba1fe7409318e3f558bec325392427aa3d8eaf46b028654f8"),
        1555501858,
        436279940,
        2599551022L.toInt()
    )
    override val protocolVersion: Int = 3
}

fun getNetworkParameters(network: String) = when(network) {
    MainNetParameters.networkName -> MainNetParameters
    TestNetParameters.networkName -> TestNetParameters
    AlphaNetParameters.networkName -> AlphaNetParameters
    else -> throw IllegalArgumentException("Network parameter '$network' is wrong.")
}
