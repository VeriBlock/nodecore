// VeriBlock NodeCore
// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.core.utilities

import io.kotest.assertions.fail
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.veriblock.core.types.BitString

class BitStringReaderTests {
    @Test
    fun bitStringReaderTestSingleByte1() {
        // 01000000
        val bytes = byteArrayOf(0x40.toByte())
        val bitString = BitString(bytes)
        var reader = BitStringReader(bitString)
        Utility.byteArraysAreEqual(reader.readBits(1), byteArrayOf(0x00.toByte())) shouldBe true
        Utility.byteArraysAreEqual(reader.readBits(7), byteArrayOf(0x40.toByte())) shouldBe true
        reader = BitStringReader(bitString)
        Utility.byteArraysAreEqual(reader.readBits(2), byteArrayOf(0x01.toByte())) shouldBe true
        Utility.byteArraysAreEqual(reader.readBits(6), byteArrayOf(0x00.toByte())) shouldBe true
        reader = BitStringReader(bitString)
        Utility.byteArraysAreEqual(reader.readBits(3), byteArrayOf(0x02.toByte())) shouldBe true
        Utility.byteArraysAreEqual(reader.readBits(5), byteArrayOf(0x00.toByte())) shouldBe true
        reader = BitStringReader(bitString)
        Utility.byteArraysAreEqual(reader.readBits(4), byteArrayOf(0x04.toByte())) shouldBe true
        Utility.byteArraysAreEqual(reader.readBits(4), byteArrayOf(0x00.toByte())) shouldBe true
        reader = BitStringReader(bitString)
        Utility.byteArraysAreEqual(reader.readBits(5), byteArrayOf(0x08.toByte())) shouldBe true
        Utility.byteArraysAreEqual(reader.readBits(3), byteArrayOf(0x00.toByte())) shouldBe true
        reader = BitStringReader(bitString)
        Utility.byteArraysAreEqual(reader.readBits(6), byteArrayOf(0x10.toByte())) shouldBe true
        Utility.byteArraysAreEqual(reader.readBits(2), byteArrayOf(0x00.toByte())) shouldBe true
        reader = BitStringReader(bitString)
        Utility.byteArraysAreEqual(reader.readBits(7), byteArrayOf(0x20.toByte())) shouldBe true
        Utility.byteArraysAreEqual(reader.readBits(1), byteArrayOf(0x00.toByte())) shouldBe true
        reader = BitStringReader(bitString)
        Utility.byteArraysAreEqual(reader.readBits(8), byteArrayOf(0x40.toByte())) shouldBe true
        reader = BitStringReader(bitString)
        Utility.byteArraysAreEqual(reader.readBits(3), byteArrayOf(0x02.toByte())) shouldBe true
        Utility.byteArraysAreEqual(reader.readBits(2), byteArrayOf(0x00.toByte())) shouldBe true
        Utility.byteArraysAreEqual(reader.readBits(3), byteArrayOf(0x00.toByte())) shouldBe true
        try {
            reader = BitStringReader(bitString)
            reader.readBits(9)
            fail("")
        } catch (e: Exception) {
        }
        reader = BitStringReader(bitString)
        reader.remaining().toLong() shouldBe 8
        reader.readBit().toLong() shouldBe 0x00
        reader.remaining().toLong() shouldBe 7
        reader.readBit().toLong() shouldBe 0x01
        reader.remaining().toLong() shouldBe 6
        reader.readBit().toLong() shouldBe 0x00
        reader.remaining().toLong() shouldBe 5
        reader.readBit().toLong() shouldBe 0x00
        reader.remaining().toLong() shouldBe 4
        reader.readBit().toLong() shouldBe 0x00
        reader.remaining().toLong() shouldBe 3
        reader.readBit().toLong() shouldBe 0x00
        reader.remaining().toLong() shouldBe 2
        reader.readBit().toLong() shouldBe 0x00
        reader.remaining().toLong() shouldBe 1
        reader.readBit().toLong() shouldBe 0x00
        reader.remaining().toLong() shouldBe 0
    }

    @Test
    fun bitStringReaderTestSingleByte2() {
        // 10001010
        val bytes = byteArrayOf(0x8A.toByte())
        val bitString = BitString(bytes)
        var reader = BitStringReader(bitString)
        Utility.byteArraysAreEqual(reader.readBits(1), byteArrayOf(0x01.toByte())) shouldBe true
        Utility.byteArraysAreEqual(reader.readBits(7), byteArrayOf(0x0A.toByte())) shouldBe true
        reader = BitStringReader(bitString)
        Utility.byteArraysAreEqual(reader.readBits(2), byteArrayOf(0x02.toByte())) shouldBe true
        Utility.byteArraysAreEqual(reader.readBits(6), byteArrayOf(0x0A.toByte())) shouldBe true
        reader = BitStringReader(bitString)
        Utility.byteArraysAreEqual(reader.readBits(3), byteArrayOf(0x04.toByte())) shouldBe true
        Utility.byteArraysAreEqual(reader.readBits(5), byteArrayOf(0x0A.toByte())) shouldBe true
        reader = BitStringReader(bitString)
        Utility.byteArraysAreEqual(reader.readBits(4), byteArrayOf(0x08.toByte())) shouldBe true
        Utility.byteArraysAreEqual(reader.readBits(4), byteArrayOf(0x0A.toByte())) shouldBe true
        reader = BitStringReader(bitString)
        Utility.byteArraysAreEqual(reader.readBits(5), byteArrayOf(0x11.toByte())) shouldBe true
        Utility.byteArraysAreEqual(reader.readBits(3), byteArrayOf(0x02.toByte())) shouldBe true
        reader = BitStringReader(bitString)
        Utility.byteArraysAreEqual(reader.readBits(6), byteArrayOf(0x22.toByte())) shouldBe true
        Utility.byteArraysAreEqual(reader.readBits(2), byteArrayOf(0x02.toByte())) shouldBe true
        reader = BitStringReader(bitString)
        Utility.byteArraysAreEqual(reader.readBits(7), byteArrayOf(0x45.toByte())) shouldBe true
        Utility.byteArraysAreEqual(reader.readBits(1), byteArrayOf(0x00.toByte())) shouldBe true
        reader = BitStringReader(bitString)
        Utility.byteArraysAreEqual(reader.readBits(8), byteArrayOf(0x8A.toByte())) shouldBe true
        reader = BitStringReader(bitString)
        Utility.byteArraysAreEqual(reader.readBits(1), byteArrayOf(0x01.toByte())) shouldBe true
        Utility.byteArraysAreEqual(reader.readBits(4), byteArrayOf(0x01.toByte())) shouldBe true
        Utility.byteArraysAreEqual(reader.readBits(2), byteArrayOf(0x01.toByte())) shouldBe true
        Utility.byteArraysAreEqual(reader.readBits(1), byteArrayOf(0x00.toByte())) shouldBe true
        try {
            reader = BitStringReader(bitString)
            reader.readBits(9)
            fail("")
        } catch (e: Exception) {
        }
        reader = BitStringReader(bitString)
        reader.remaining().toLong() shouldBe 8
        reader.readBit().toLong() shouldBe 0x01
        reader.remaining().toLong() shouldBe 7
        reader.readBit().toLong() shouldBe 0x00
        reader.remaining().toLong() shouldBe 6
        reader.readBit().toLong() shouldBe 0x00
        reader.remaining().toLong() shouldBe 5
        reader.readBit().toLong() shouldBe 0x00
        reader.remaining().toLong() shouldBe 4
        reader.readBit().toLong() shouldBe 0x01
        reader.remaining().toLong() shouldBe 3
        reader.readBit().toLong() shouldBe 0x00
        reader.remaining().toLong() shouldBe 2
        reader.readBit().toLong() shouldBe 0x01
        reader.remaining().toLong() shouldBe 1
        reader.readBit().toLong() shouldBe 0x00
        reader.remaining().toLong() shouldBe 0
    }

    @Test
    fun bitStringReaderTestDoubleByte1() {
        // 0011000101101101
        val bytes = byteArrayOf(0x31.toByte(), 0x6D.toByte())
        val bitString = BitString(bytes)
        var reader = BitStringReader(bitString)
        Utility.byteArraysAreEqual(reader.readBits(1), byteArrayOf(0x00.toByte())) shouldBe true
        Utility.byteArraysAreEqual(reader.readBits(15), byteArrayOf(0x31.toByte(), 0x6D.toByte())) shouldBe true
        reader = BitStringReader(bitString)
        Utility.byteArraysAreEqual(reader.readBits(2), byteArrayOf(0x00.toByte())) shouldBe true
        Utility.byteArraysAreEqual(reader.readBits(14), byteArrayOf(0x31.toByte(), 0x6D.toByte())) shouldBe true
        reader = BitStringReader(bitString)
        Utility.byteArraysAreEqual(reader.readBits(3), byteArrayOf(0x01.toByte())) shouldBe true
        Utility.byteArraysAreEqual(reader.readBits(13), byteArrayOf(0x11.toByte(), 0x6D.toByte())) shouldBe true
        reader = BitStringReader(bitString)
        Utility.byteArraysAreEqual(reader.readBits(4), byteArrayOf(0x03.toByte())) shouldBe true
        Utility.byteArraysAreEqual(reader.readBits(12), byteArrayOf(0x01.toByte(), 0x6D.toByte())) shouldBe true
        reader = BitStringReader(bitString)
        Utility.byteArraysAreEqual(reader.readBits(5), byteArrayOf(0x06.toByte())) shouldBe true
        Utility.byteArraysAreEqual(reader.readBits(11), byteArrayOf(0x01.toByte(), 0x6D.toByte())) shouldBe true
        reader = BitStringReader(bitString)
        Utility.byteArraysAreEqual(reader.readBits(6), byteArrayOf(0x0C.toByte())) shouldBe true
        Utility.byteArraysAreEqual(reader.readBits(10), byteArrayOf(0x01.toByte(), 0x6D.toByte())) shouldBe true
        reader = BitStringReader(bitString)
        Utility.byteArraysAreEqual(reader.readBits(7), byteArrayOf(0x18.toByte())) shouldBe true
        Utility.byteArraysAreEqual(reader.readBits(9), byteArrayOf(0x01.toByte(), 0x6D.toByte())) shouldBe true
        reader = BitStringReader(bitString)
        Utility.byteArraysAreEqual(reader.readBits(8), byteArrayOf(0x31.toByte())) shouldBe true
        Utility.byteArraysAreEqual(reader.readBits(8), byteArrayOf(0x6D.toByte())) shouldBe true
        reader = BitStringReader(bitString)
        Utility.byteArraysAreEqual(reader.readBits(3), byteArrayOf(0x01.toByte())) shouldBe true
        Utility.byteArraysAreEqual(reader.readBits(2), byteArrayOf(0x02.toByte())) shouldBe true
        Utility.byteArraysAreEqual(reader.readBits(5), byteArrayOf(0x05.toByte())) shouldBe true
        Utility.byteArraysAreEqual(reader.readBits(6), byteArrayOf(0x2D.toByte())) shouldBe true
        try {
            reader = BitStringReader(bitString)
            reader.readBits(5)
            reader.readBits(10)
            reader.readBits(2)
            fail("")
        } catch (e: Exception) {
        }
        reader = BitStringReader(bitString)
        reader.remaining().toLong() shouldBe 16
        reader.readBit().toLong() shouldBe 0x00
        reader.remaining().toLong() shouldBe 15
        reader.readBit().toLong() shouldBe 0x00
        reader.remaining().toLong() shouldBe 14
        reader.readBit().toLong() shouldBe 0x01
        reader.remaining().toLong() shouldBe 13
        reader.readBit().toLong() shouldBe 0x01
        reader.remaining().toLong() shouldBe 12
        reader.readBit().toLong() shouldBe 0x00
        reader.remaining().toLong() shouldBe 11
        reader.readBit().toLong() shouldBe 0x00
        reader.remaining().toLong() shouldBe 10
        reader.readBit().toLong() shouldBe 0x00
        reader.remaining().toLong() shouldBe 9
        reader.readBit().toLong() shouldBe 0x01
        reader.remaining().toLong() shouldBe 8
        reader.readBit().toLong() shouldBe 0x00
        reader.remaining().toLong() shouldBe 7
        reader.readBit().toLong() shouldBe 0x01
        reader.remaining().toLong() shouldBe 6
        reader.readBit().toLong() shouldBe 0x01
        reader.remaining().toLong() shouldBe 5
        reader.readBit().toLong() shouldBe 0x00
        reader.remaining().toLong() shouldBe 4
        reader.readBit().toLong() shouldBe 0x01
        reader.remaining().toLong() shouldBe 3
        reader.readBit().toLong() shouldBe 0x01
        reader.remaining().toLong() shouldBe 2
        reader.readBit().toLong() shouldBe 0x00
        reader.remaining().toLong() shouldBe 1
        reader.readBit().toLong() shouldBe 0x01
        reader.remaining().toLong() shouldBe 0
    }
}
