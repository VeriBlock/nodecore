// VeriBlock NodeCore
// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.core.utilities

import io.kotest.matchers.shouldBe
import org.junit.Test

class UtilityTests {
    @Test
    fun flipWhenEvenNumberLength() {
        val test = byteArrayOf(0xFF.toByte(), 0xAA.toByte(), 0x55.toByte(), 0x00.toByte())
        val result = Utility.flip(test)
        result[0] shouldBe test[3]
        result[1] shouldBe test[2]
        result[2] shouldBe test[1]
        result[3] shouldBe test[0]
    }

    @Test
    fun flipWhenOddNumberLength() {
        val test = byteArrayOf(0xFF.toByte(), 0x88.toByte(), 0x00.toByte())
        val result = Utility.flip(test)
        result[0] shouldBe test[2]
        result[1] shouldBe test[1]
        result[2] shouldBe test[0]
    }

    @Test
    fun flipHex() {
        val version = "02000000"
        val flipped = Utility.flipHex(version)
        flipped shouldBe "00000002"
    }
}
