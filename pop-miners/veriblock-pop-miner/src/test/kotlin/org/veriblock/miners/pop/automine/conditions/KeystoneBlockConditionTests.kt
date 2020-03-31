// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.automine.conditions

import io.kotlintest.shouldBe
import org.junit.Test
import org.veriblock.miners.pop.AutoMineConfig
import org.veriblock.miners.pop.automine.keystoneBlockCondition

class KeystoneBlockConditionTests {
    private val config = AutoMineConfig(round4 = true)

    @Test
    fun evaluateWhenHeightIsNotKeystone() {
        for (i in 1..19) {
            keystoneBlockCondition(config, 10000 + i) shouldBe false
        }
    }

    @Test
    fun evaluateWhenHeightIsKeystone() {
        keystoneBlockCondition(config, 13240) shouldBe true
    }
}
