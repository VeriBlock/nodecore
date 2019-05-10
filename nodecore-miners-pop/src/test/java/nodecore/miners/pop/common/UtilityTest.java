// VeriBlock PoP Miner
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.common;

import org.bitcoinj.core.Coin;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;

public class UtilityTest {
    @Test()
    public void isIntegerReturnsTrue() {
        boolean isInt = Utility.isInteger("1");
        Assert.assertEquals(true, isInt);
    }

    @Test
    public void amountToCoinWhenValidDecimal() {
        BigDecimal test = new BigDecimal("1.2537");

        Coin result = Utility.amountToCoin(test);
        Assert.assertEquals(Coin.valueOf(125370000), result);
    }

    @Test
    public void amountToCoinWhenMinimum() {
        BigDecimal test = new BigDecimal("0.00000001");

        Coin result = Utility.amountToCoin(test);
        Assert.assertEquals(Coin.valueOf(1), result);
    }

    @Test
    public void amountToCoinWhenBelowMinimum() {
        BigDecimal test = new BigDecimal("0.000000009");

        Coin result = Utility.amountToCoin(test);
        Assert.assertEquals(Coin.valueOf(0), result);
    }

    @Test
    public void isValidCronExpressionWhenValid() {
        String toTest = "0/30 * * * * ?";

        boolean result = Utility.isValidCronExpression(toTest);
        Assert.assertTrue(result);
    }

    @Test
    public void formatBTCFriendlyString_whenAllDecimalsSignificant() {
        Coin coin = Coin.valueOf(12345678);
        String formatted = Utility.formatBTCFriendlyString(coin);

        Assert.assertTrue("0.12345678 BTC".equals(formatted));
    }

    @Test
    public void formatBTCFriendlyString_whenTrailingZeros() {
        Coin coin = Coin.valueOf(5000);
        String formatted = Utility.formatBTCFriendlyString(coin);

        Assert.assertTrue("0.00005000 BTC".equals(formatted));
    }

    @Test
    public void formatBTCFriendlyString_whenOneBitcoin() {
        Coin coin = Coin.valueOf(100000000);
        String formatted = Utility.formatBTCFriendlyString(coin);

        Assert.assertTrue("1.00000000 BTC".equals(formatted));
    }

    @Test
    public void formatBTCFriendlyString_whenMixed() {
        Coin coin = Coin.valueOf(1500700300);
        String formatted = Utility.formatBTCFriendlyString(coin);

        Assert.assertTrue("15.00700300 BTC".equals(formatted));
    }
}
