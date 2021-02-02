// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.sdk.util

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.veriblock.core.utilities.Utility
import java.math.BigInteger
import java.util.*

class UtilityTests {
    @Test
    fun toBytes_WhenLessThanSize() {
        val value = BigInteger.valueOf(999989182464L)
        val result = Utility.toBytes(value, 12)
        val expected = byteArrayOf(0, 0, 0, 0, 0, 0, 0, -24, -44, 0, 0, 0)
        result.toList() shouldContainExactly expected.toList()
    }

    @Test
    fun verifySignature_WhenValid() {
        val message = Utility.hexToBytes("0123456789ABCDEF")
        val publicKey =
            Utility.hexToBytes(
                "3056301006072A8648CE3D020106052B8104000A03420004CB427E41A0114874080A4B1E2AB7920E22CD2D188C87140DEFA447EE5FC44BB848E1C0DB5EF206DE2E7002F6C86952BE4823A4C08E65E4CDBEB904A8B95763AA"
            )
        val signature =
            Utility.hexToBytes(
                "304402202F2B136EB22EDDACA3E9EA9C43A06478FF095108A19F433C358CE2C84461DE800220617E54A3FBC8B61B22D29772D58B27F47395915515F040E170BB50D951646C57"
            )
        Utility.verifySignature(message, signature, publicKey) shouldBe true
    }

    @Test
    fun toInt() {
        val hex = Utility.hexToBytes("24509D41F548C0")
        val bitSet = BitSet.valueOf(Utility.flip(hex))
        val offset = 6
        Utility.toInt(bitSet[offset, offset + 8]).toLong() shouldBe 35
        Utility.toInt(bitSet[offset + 8, offset + 14]).toLong() shouldBe 21
        Utility.toInt(bitSet[offset + 14, offset + 22]).toLong() shouldBe 31
        Utility.toInt(bitSet[offset + 22, offset + 28]).toLong() shouldBe 20
        Utility.toInt(bitSet[offset + 28, offset + 36]).toLong() shouldBe 39
        Utility.toInt(bitSet[offset + 36, offset + 42]).toLong() shouldBe 20
        Utility.toInt(bitSet[offset + 42, bitSet.length()]).toLong() shouldBe 36
    }
}
