// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.sdk

import io.kotest.assertions.fail
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.veriblock.core.utilities.Utility
import org.veriblock.sdk.models.asCoin
import org.veriblock.sdk.models.parseCoin
import org.veriblock.sdk.services.SerializeDeserializeService
import org.veriblock.sdk.util.writeSingleByteLengthValue
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.ByteBuffer

class CoinTests {
    @Test
    fun parse() {
        val input = 123456789L.asCoin()
        val serialized = SerializeDeserializeService.serialize(input)
        val deserialized = ByteBuffer.wrap(serialized).parseCoin()
        deserialized shouldBe input
    }

    @Test
    @Throws(IOException::class)
    fun parseWhenInvalid() {
        val array = Utility.fillBytes(0xFF.toByte(), 9)
        try {
            ByteArrayOutputStream().use { stream ->
                stream.writeSingleByteLengthValue(array)
                val buffer = ByteBuffer.wrap(stream.toByteArray())
                buffer.parseCoin()
                fail("Expected IllegalArgumentException")
            }
        } catch (e: IllegalArgumentException) {
            e.message!!.startsWith("Unexpected length") shouldBe true
        }
    }
}
