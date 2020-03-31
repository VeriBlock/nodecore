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
import org.veriblock.miners.pop.automine.round2Condition

class Round2ConditionTests {
    private val config = AutoMineConfig(round2 = true)

    @Test
    fun evaluateWhenHeightIsNotRound2() {
        for (i in 1..19) {
            if (i % 3 != 2) {
                round2Condition(config, 10000 + i) shouldBe false
            }
        }
    }

    @Test
    fun evaluateWhenHeightIsRound2() {
        for (i in 1..19) {
            if (i % 3 == 2) {
                round2Condition(config, 13240 + i) shouldBe true
            }
        }
    }
}
