// VeriBlock PoP Miner
// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.miners.pop.common

import io.kotest.matchers.shouldBe
import org.bitcoinj.core.Coin
import org.junit.Test
import java.math.BigDecimal

class UtilityTest {
    @Test
    fun amountToCoinWhenValidDecimal() {
        val test = BigDecimal("1.2537")
        val result = test.amountToCoin()
        result shouldBe Coin.valueOf(125370000)
    }

    @Test
    fun amountToCoinWhenMinimum() {
        val test = BigDecimal("0.00000001")
        val result = test.amountToCoin()
        result shouldBe Coin.valueOf(1)
    }

    @Test
    fun amountToCoinWhenBelowMinimum() {
        val test = BigDecimal("0.000000009")
        val result = test.amountToCoin()
        result shouldBe Coin.valueOf(0)
    }

    @Test
    fun isValidCronExpressionWhenValid() {
        val toTest = "0/30 * * * * ?"
        val result = toTest.isValidCronExpression()
        result shouldBe true
    }

    @Test
    fun formatBTCFriendlyString_whenAllDecimalsSignificant() {
        val coin = Coin.valueOf(12345678)
        val formatted = coin.formatBTCFriendlyString()
        formatted shouldBe "0.12345678 BTC"
    }

    @Test
    fun formatBTCFriendlyString_whenTrailingZeros() {
        val coin = Coin.valueOf(5000)
        val formatted = coin.formatBTCFriendlyString()
        formatted shouldBe "0.00005000 BTC"
    }

    @Test
    fun formatBTCFriendlyString_whenOneBitcoin() {
        val coin = Coin.valueOf(100000000)
        val formatted = coin.formatBTCFriendlyString()
        formatted shouldBe "1.00000000 BTC"
    }

    @Test
    fun formatBTCFriendlyString_whenMixed() {
        val coin = Coin.valueOf(1500700300)
        val formatted = coin.formatBTCFriendlyString()
        formatted shouldBe "15.00700300 BTC"
    }
}
