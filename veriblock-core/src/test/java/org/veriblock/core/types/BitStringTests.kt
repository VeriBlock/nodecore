// VeriBlock NodeCore
// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.core.types

import io.kotest.matchers.shouldBe
import org.junit.Test

class BitStringTests {
    @Test
    fun bitStringTestSingleByte1() {
        val bytes = byteArrayOf(0x40.toByte())
        val bitString = BitString(bytes)
        bitString.toString() shouldBe "01000000"
        bitString.getBit(0).toLong() shouldBe 0x00
        bitString.getBit(1).toLong() shouldBe 0x01
        bitString.getBit(2).toLong() shouldBe 0x00
        bitString.getBit(3).toLong() shouldBe 0x00
        bitString.getBit(4).toLong() shouldBe 0x00
        bitString.getBit(5).toLong() shouldBe 0x00
        bitString.getBit(6).toLong() shouldBe 0x00
        bitString.getBit(7).toLong() shouldBe 0x00
        bitString.getBoolean(0) shouldBe false
        bitString.getBoolean(1) shouldBe true
        bitString.getBoolean(2) shouldBe false
        bitString.getBoolean(3) shouldBe false
        bitString.getBoolean(4) shouldBe false
        bitString.getBoolean(5) shouldBe false
        bitString.getBoolean(6) shouldBe false
        bitString.getBoolean(7) shouldBe false
    }

    @Test
    fun bitStringTestSingleByte2() {
        val bytes = byteArrayOf(0xF7.toByte())
        val bitString = BitString(bytes)
        bitString.toString() shouldBe "11110111"
        bitString.getBit(0).toLong() shouldBe 0x01
        bitString.getBit(1).toLong() shouldBe 0x01
        bitString.getBit(2).toLong() shouldBe 0x01
        bitString.getBit(3).toLong() shouldBe 0x01
        bitString.getBit(4).toLong() shouldBe 0x00
        bitString.getBit(5).toLong() shouldBe 0x01
        bitString.getBit(6).toLong() shouldBe 0x01
        bitString.getBit(7).toLong() shouldBe 0x01
        bitString.getBoolean(0) shouldBe true
        bitString.getBoolean(1) shouldBe true
        bitString.getBoolean(2) shouldBe true
        bitString.getBoolean(3) shouldBe true
        bitString.getBoolean(4) shouldBe false
        bitString.getBoolean(5) shouldBe true
        bitString.getBoolean(6) shouldBe true
        bitString.getBoolean(7) shouldBe true
    }

    @Test
    fun bitStringTestSingleByte3() {
        val bytes = byteArrayOf(0xA3.toByte())
        val bitString = BitString(bytes)
        bitString.toString() shouldBe "10100011"
        bitString.getBit(0).toLong() shouldBe 0x01
        bitString.getBit(1).toLong() shouldBe 0x00
        bitString.getBit(2).toLong() shouldBe 0x01
        bitString.getBit(3).toLong() shouldBe 0x00
        bitString.getBit(4).toLong() shouldBe 0x00
        bitString.getBit(5).toLong() shouldBe 0x00
        bitString.getBit(6).toLong() shouldBe 0x01
        bitString.getBit(7).toLong() shouldBe 0x01
        bitString.getBoolean(0) shouldBe true
        bitString.getBoolean(1) shouldBe false
        bitString.getBoolean(2) shouldBe true
        bitString.getBoolean(3) shouldBe false
        bitString.getBoolean(4) shouldBe false
        bitString.getBoolean(5) shouldBe false
        bitString.getBoolean(6) shouldBe true
        bitString.getBoolean(7) shouldBe true
    }

    @Test
    fun bitStringTestDoubleByte1() {
        val bytes = byteArrayOf(0x40.toByte(), 0xF7.toByte())
        val bitString = BitString(bytes)
        bitString.toString() shouldBe "0100000011110111"
        bitString.getBit(0).toLong() shouldBe 0x00
        bitString.getBit(1).toLong() shouldBe 0x01
        bitString.getBit(2).toLong() shouldBe 0x00
        bitString.getBit(3).toLong() shouldBe 0x00
        bitString.getBit(4).toLong() shouldBe 0x00
        bitString.getBit(5).toLong() shouldBe 0x00
        bitString.getBit(6).toLong() shouldBe 0x00
        bitString.getBit(7).toLong() shouldBe 0x00
        bitString.getBit(8).toLong() shouldBe 0x01
        bitString.getBit(9).toLong() shouldBe 0x01
        bitString.getBit(10).toLong() shouldBe 0x01
        bitString.getBit(11).toLong() shouldBe 0x01
        bitString.getBit(12).toLong() shouldBe 0x00
        bitString.getBit(13).toLong() shouldBe 0x01
        bitString.getBit(14).toLong() shouldBe 0x01
        bitString.getBit(15).toLong() shouldBe 0x01
        bitString.getBoolean(0) shouldBe false
        bitString.getBoolean(1) shouldBe true
        bitString.getBoolean(2) shouldBe false
        bitString.getBoolean(3) shouldBe false
        bitString.getBoolean(4) shouldBe false
        bitString.getBoolean(5) shouldBe false
        bitString.getBoolean(6) shouldBe false
        bitString.getBoolean(7) shouldBe false
        bitString.getBoolean(8) shouldBe true
        bitString.getBoolean(9) shouldBe true
        bitString.getBoolean(10) shouldBe true
        bitString.getBoolean(11) shouldBe true
        bitString.getBoolean(12) shouldBe false
        bitString.getBoolean(13) shouldBe true
        bitString.getBoolean(14) shouldBe true
        bitString.getBoolean(15) shouldBe true
    }

    @Test
    fun bitStringTestDoubleByte2() {
        val bytes = byteArrayOf(0xF7.toByte(), 0xC9.toByte())
        val bitString = BitString(bytes)
        bitString.toString() shouldBe "1111011111001001"
        bitString.getBit(0).toLong() shouldBe 0x01
        bitString.getBit(1).toLong() shouldBe 0x01
        bitString.getBit(2).toLong() shouldBe 0x01
        bitString.getBit(3).toLong() shouldBe 0x01
        bitString.getBit(4).toLong() shouldBe 0x00
        bitString.getBit(5).toLong() shouldBe 0x01
        bitString.getBit(6).toLong() shouldBe 0x01
        bitString.getBit(7).toLong() shouldBe 0x01
        bitString.getBit(8).toLong() shouldBe 0x01
        bitString.getBit(9).toLong() shouldBe 0x01
        bitString.getBit(10).toLong() shouldBe 0x00
        bitString.getBit(11).toLong() shouldBe 0x00
        bitString.getBit(12).toLong() shouldBe 0x01
        bitString.getBit(13).toLong() shouldBe 0x00
        bitString.getBit(14).toLong() shouldBe 0x00
        bitString.getBit(15).toLong() shouldBe 0x01
        bitString.getBoolean(0) shouldBe true
        bitString.getBoolean(1) shouldBe true
        bitString.getBoolean(2) shouldBe true
        bitString.getBoolean(3) shouldBe true
        bitString.getBoolean(4) shouldBe false
        bitString.getBoolean(5) shouldBe true
        bitString.getBoolean(6) shouldBe true
        bitString.getBoolean(7) shouldBe true
        bitString.getBoolean(8) shouldBe true
        bitString.getBoolean(9) shouldBe true
        bitString.getBoolean(10) shouldBe false
        bitString.getBoolean(11) shouldBe false
        bitString.getBoolean(12) shouldBe true
        bitString.getBoolean(13) shouldBe false
        bitString.getBoolean(14) shouldBe false
        bitString.getBoolean(15) shouldBe true
    }

    @Test
    fun bitStringTestManyBytes() {
        val bytes = ByteArray(512)
        var expectedBitString = ""
        for (i in bytes.indices) {
            bytes[i] = i.toByte()
            var convertedBitString = Integer.toBinaryString(bytes[i].toInt() and 0xFF)
            while (convertedBitString.length < 8) {
                convertedBitString = "0$convertedBitString"
            }
            expectedBitString += convertedBitString
        }
        val bitString = BitString(bytes)
        bitString.toString() shouldBe expectedBitString
        for (i in 0 until bitString.length()) {
            (if (expectedBitString[i] == '0') 0x00.toByte() else 0x01.toByte()).toLong() shouldBe bitString.getBit(i).toLong()
            (expectedBitString[i] != '0') shouldBe bitString.getBoolean(i)
        }
    }
}
