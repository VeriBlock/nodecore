// VeriBlock PoP Miner
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.rules.conditions;

import nodecore.miners.pop.Configuration;
import org.junit.Assert;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class Round2ConditionTests {

    @Test
    public void isActiveWhenConfigurationFalse() {
        Configuration config = mock(Configuration.class);
        when(config.getBoolean("auto.mine.round2")).thenReturn(false);

        Round2Condition condition = new Round2Condition();
        Assert.assertFalse(condition.isActive(config));
    }

    @Test
    public void isActiveWhenConfigurationTrue() {
        Configuration config = mock(Configuration.class);
        when(config.getBoolean("auto.mine.round2")).thenReturn(true);

        Round2Condition condition = new Round2Condition();
        Assert.assertTrue(condition.isActive(config));
    }

    @Test
    public void evaluateWhenHeightNull() {
        Round2Condition condition = new Round2Condition();
        Assert.assertFalse(condition.evaluate(null));
    }

    @Test
    public void evaluateWhenHeightIsNotRound2() {
        Round2Condition condition = new Round2Condition();
        for (int i = 1; i <= 19; i++) {
            if (i % 3 != 2) {
                Assert.assertFalse(condition.evaluate(10000 + i));
            }
        }
    }

    @Test
    public void evaluateWhenHeightIsRound2() {
        Round2Condition condition = new Round2Condition();
        for (int i = 1; i <= 19; i++) {
            if (i % 3 == 2) {
                Assert.assertTrue(condition.evaluate(13240 + i));
            }
        }
    }
}
