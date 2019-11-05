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

public class KeystoneBlockConditionTests {

    @Test
    public void isActiveWhenConfigurationFalse() {
        Configuration config = mock(Configuration.class);
        when(config.getBoolean("auto.mine.round4")).thenReturn(false);

        KeystoneBlockCondition condition = new KeystoneBlockCondition();
        Assert.assertFalse(condition.isActive(config));
    }

    @Test
    public void isActiveWhenConfigurationTrue() {
        Configuration config = mock(Configuration.class);
        when(config.getBoolean("auto.mine.round4")).thenReturn(true);

        KeystoneBlockCondition condition = new KeystoneBlockCondition();
        Assert.assertTrue(condition.isActive(config));
    }

    @Test
    public void evaluateWhenHeightNull() {
        KeystoneBlockCondition condition = new KeystoneBlockCondition();
        Assert.assertFalse(condition.evaluate(null));
    }

    @Test
    public void evaluateWhenHeightNotKeystone() {
        KeystoneBlockCondition condition = new KeystoneBlockCondition();
        for (int i = 1; i <= 19; i++) {
            Assert.assertFalse(condition.evaluate(10000 + i));
        }
    }

    @Test
    public void evaluateWhenHeightKeystone() {
        KeystoneBlockCondition condition = new KeystoneBlockCondition();
        Assert.assertTrue(condition.evaluate(13240));
    }
}
