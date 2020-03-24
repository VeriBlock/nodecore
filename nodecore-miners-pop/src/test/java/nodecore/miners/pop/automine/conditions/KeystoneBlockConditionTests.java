// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.automine.conditions;

import org.junit.Assert;
import org.junit.Test;

public class KeystoneBlockConditionTests {

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
