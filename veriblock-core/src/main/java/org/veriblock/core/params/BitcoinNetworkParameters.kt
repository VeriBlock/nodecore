// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.core.params

import java.math.BigInteger

interface BitcoinNetworkParameters {
    val powLimit: BigInteger
    val powTargetTimespan: Int
    val powTargetSpacing: Int
    val allowMinDifficultyBlocks: Boolean
    val powNoRetargeting: Boolean
}

class BitcoinMainNetParameters : BitcoinNetworkParameters {
    override val powLimit = BigInteger("00000000ffffffffffffffffffffffffffffffffffffffffffffffffffffffff", 16)
    override val powTargetTimespan = 14 * 24 * 60 * 60
    override val powTargetSpacing = 10 * 60
    override val allowMinDifficultyBlocks = false
    override val powNoRetargeting = false
}

class BitcoinTestNetParameters : BitcoinNetworkParameters {
    override val powLimit = BigInteger("00000000ffffffffffffffffffffffffffffffffffffffffffffffffffffffff", 16)
    override val powTargetTimespan = 14 * 24 * 60 * 60
    override val powTargetSpacing = 10 * 60
    override val allowMinDifficultyBlocks = true
    override val powNoRetargeting = false
}

class BitcoinRegTestParameters : BitcoinNetworkParameters {
    override val powLimit = BigInteger("7fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff", 16)
    override val powTargetTimespan = 14 * 24 * 60 * 60
    override val powTargetSpacing = 10 * 60
    override val allowMinDifficultyBlocks = true
    override val powNoRetargeting = true
}
