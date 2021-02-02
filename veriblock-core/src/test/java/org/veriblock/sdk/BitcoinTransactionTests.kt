// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.sdk

import io.kotest.matchers.shouldBe
import org.junit.Test
import org.veriblock.core.utilities.Utility
import org.veriblock.sdk.models.BitcoinTransaction
import java.nio.ByteBuffer
import java.util.*

class BitcoinTransactionTests {
    @Test
    fun containsSplit_WhenDescriptorBeforeChunks() {
        val random = Random(100L)
        val buffer = ByteBuffer.allocateDirect(243)

        // Random starting bytes
        var randomBytes = ByteArray(15)
        random.nextBytes(randomBytes)
        buffer.put(randomBytes)

        // Descriptor bytes (3 MAGIC, 1 SIZE, 7 SECTIONALS)
        buffer.put(Utility.hexToBytes("927A594624509D41F548C0"))

        // Random bytes
        randomBytes = ByteArray(10)
        random.nextBytes(randomBytes)
        buffer.put(randomBytes)

        // First chunk of 20 bytes
        buffer.put(Utility.hexToBytes("00000767000193093228BD2B4906F6B84BE5E618"))
        randomBytes = ByteArray(39)
        random.nextBytes(randomBytes)
        buffer.put(randomBytes)

        // Second chunk of 20 bytes
        buffer.put(Utility.hexToBytes("09C0522626145DDFB988022A0684E2110D384FE2"))
        randomBytes = ByteArray(31)
        random.nextBytes(randomBytes)
        buffer.put(randomBytes)

        // Third chunk of 21 bytes
        buffer.put(Utility.hexToBytes("BFD38549CB19C41893C258BA5B9CAB24060BA2D410"))
        randomBytes = ByteArray(35)
        random.nextBytes(randomBytes)
        buffer.put(randomBytes)

        // Fourth chunk of unstated 19 bytes
        buffer.put(Utility.hexToBytes("39DFC857801424B0F5DE63992A016F5F38FEB4"))
        randomBytes = ByteArray(22)
        random.nextBytes(randomBytes)
        buffer.put(randomBytes)
        buffer.flip()
        val transaction = ByteArray(243)
        buffer[transaction]
        val test = BitcoinTransaction(transaction)
        val result =
            test.containsSplit(
                Utility.hexToBytes(
                    "00000767000193093228BD2B4906F6B84BE5E61809C0522626145DDFB988022A0684E2110D384FE2BFD38549CB19C41893C258BA5B9CAB24060BA2D41039DFC857801424B0F5DE63992A016F5F38FEB4"
                )
            )
        result shouldBe true
    }

    @Test
    fun contains_WhenChunked() {
        val random = Random(100L)
        val buffer = ByteBuffer.allocateDirect(243)

        // Random starting bytes
        var randomBytes = ByteArray(15)
        random.nextBytes(randomBytes)
        buffer.put(randomBytes)

        // Descriptor bytes (3 MAGIC, 1 SIZE, 7 SECTIONALS)
        buffer.put(Utility.hexToBytes("927A594624509D41F548C0"))

        // Random bytes
        randomBytes = ByteArray(10)
        random.nextBytes(randomBytes)
        buffer.put(randomBytes)

        // First chunk of 20 bytes
        buffer.put(Utility.hexToBytes("00000767000193093228BD2B4906F6B84BE5E618"))
        randomBytes = ByteArray(39)
        random.nextBytes(randomBytes)
        buffer.put(randomBytes)

        // Second chunk of 20 bytes
        buffer.put(Utility.hexToBytes("09C0522626145DDFB988022A0684E2110D384FE2"))
        randomBytes = ByteArray(31)
        random.nextBytes(randomBytes)
        buffer.put(randomBytes)

        // Third chunk of 21 bytes
        buffer.put(Utility.hexToBytes("BFD38549CB19C41893C258BA5B9CAB24060BA2D410"))
        randomBytes = ByteArray(35)
        random.nextBytes(randomBytes)
        buffer.put(randomBytes)

        // Fourth chunk of unstated 19 bytes
        buffer.put(Utility.hexToBytes("39DFC857801424B0F5DE63992A016F5F38FEB4"))
        randomBytes = ByteArray(22)
        random.nextBytes(randomBytes)
        buffer.put(randomBytes)
        buffer.flip()
        val transaction = ByteArray(243)
        buffer[transaction]
        val test = BitcoinTransaction(transaction)
        val result =
            test.contains(
                Utility.hexToBytes(
                    "00000767000193093228BD2B4906F6B84BE5E61809C0522626145DDFB988022A0684E2110D384FE2BFD38549CB19C41893C258BA5B9CAB24060BA2D41039DFC857801424B0F5DE63992A016F5F38FEB4"
                )
            )
        result shouldBe true
    }
}
