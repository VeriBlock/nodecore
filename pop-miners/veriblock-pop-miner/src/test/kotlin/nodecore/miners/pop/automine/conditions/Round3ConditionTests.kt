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
import org.veriblock.miners.pop.automine.round3Condition

class Round3ConditionTests {
    private val config = AutoMineConfig(round3 = true)

    @Test
    fun evaluateWhenHeightIsNotRound3() {
        for (i in 1..19) {
            if (i % 3 != 0) {
                round3Condition(config, 10000 + i) shouldBe false
            }
        }
    }

    @Test
    fun evaluateWhenHeightIsRound3() {
        for (i in 1..19) {
            if (i % 3 == 0) {
                round3Condition(config, 13240 + i) shouldBe true
            }
        }
    }

    @Test
    fun evaluateWhenHeightIsKeystone() {
        round3Condition(config, 13240) shouldBe false
    }
}
