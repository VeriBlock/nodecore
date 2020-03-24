// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.automine.conditions;

import org.junit.Assert;
import org.junit.Test;

public class Round1ConditionTests {

    @Test
    public void evaluateWhenHeightNull() {
        Round1Condition condition = new Round1Condition();
        Assert.assertFalse(condition.evaluate(null));
    }

    @Test
    public void evaluateWhenHeightIsNotRound1() {
        Round1Condition condition = new Round1Condition();
        for (int i = 1; i <= 19; i++) {
            if (i % 3 != 1) {
                Assert.assertFalse(condition.evaluate(10000 + i));
            }
        }
    }

    @Test
    public void evaluateWhenHeightIsRound1() {
        Round1Condition condition = new Round1Condition();
        for (int i = 1; i <= 19; i++) {
            if (i % 3 == 1) {
                Assert.assertTrue(condition.evaluate(13240 + i));
            }
        }
    }
}
