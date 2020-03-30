// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.automine.conditions

import io.kotlintest.shouldBe
import nodecore.miners.pop.AutoMineConfig
import nodecore.miners.pop.automine.round1Condition
import org.junit.Test

class Round1ConditionTests {
    private val config = AutoMineConfig(round1 = true)

    @Test
    fun evaluateWhenHeightIsNotRound2() {
        for (i in 1..19) {
            if (i % 3 != 1) {
                round1Condition(config, 10000 + i) shouldBe false
            }
        }
    }

    @Test
    fun evaluateWhenHeightIsRound1() {
        for (i in 1..19) {
            if (i % 3 == 1) {
                round1Condition(config, 13240 + i) shouldBe true
            }
        }
    }
}
