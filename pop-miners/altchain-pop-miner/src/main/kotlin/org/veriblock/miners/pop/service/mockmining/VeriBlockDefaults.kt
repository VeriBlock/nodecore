// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.miners.pop.service.mockmining

import org.veriblock.sdk.blockchain.VeriBlockBlockchainBootstrapConfig
import org.veriblock.sdk.conf.RegTestParameters
import org.veriblock.sdk.conf.VeriBlockNetworkParameters
import org.veriblock.sdk.models.Sha256Hash
import org.veriblock.sdk.models.VBlakeHash
import org.veriblock.sdk.models.VeriBlockBlock
import org.veriblock.sdk.util.BitcoinUtils
import java.util.Arrays

object VeriBlockDefaults {
    val networkParameters: VeriBlockNetworkParameters = RegTestParameters()
    val genesis = VeriBlockBlock(
        0,
        2.toShort(),
        VBlakeHash.EMPTY_HASH.trimToPreviousBlockSize(),
        VBlakeHash.EMPTY_HASH.trimToPreviousKeystoneSize(),
        VBlakeHash.EMPTY_HASH.trimToPreviousKeystoneSize(),
        Sha256Hash.ZERO_HASH,
        1577367966,
        BitcoinUtils.encodeCompactBits(networkParameters.minimumDifficulty).toInt(),
        0
    )
    val bootstrap = VeriBlockBlockchainBootstrapConfig(
        Arrays.asList(genesis)
    )
}
