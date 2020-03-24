// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.automine.conditions

import org.junit.Test

class Round3ConditionTests {
    @Test
    fun evaluateWhenHeightNull() {
        val condition = Round3Condition()
        condition.evaluate(null) shouldBe false
    }

    @Test
    fun evaluateWhenHeightIsNotRound3() {
        val condition = Round3Condition()
        for (i in 1..19) {
            if (i % 3 != 0) {
                condition.evaluate(10000 + i) shouldBe false
            }
        }
    }

    @Test
    fun evaluateWhenHeightIsRound3() {
        val condition = Round3Condition()
        for (i in 1..19) {
            if (i % 3 == 0) {
                condition.evaluate(13240 + i) shouldBe true
            }
        }
    }

    @Test
    fun evaluateWhenHeightIsKeystone() {
        val condition = Round3Condition()
        condition.evaluate(13240) shouldBe false
    }
}
