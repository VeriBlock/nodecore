// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.sdk

import io.kotlintest.shouldBe
import org.junit.Assert
import org.junit.Test
import org.veriblock.core.utilities.Utility
import org.veriblock.sdk.models.Address
import org.veriblock.sdk.models.Coin
import org.veriblock.sdk.models.Output
import java.nio.ByteBuffer

class OutputTests {
    @Test
    fun parse() {
        val input = Utility.hexToBytes("01166772F51AB208D32771AB1506970EEB664462730B838E020539")
        val decoded = Output.parse(ByteBuffer.wrap(input))
        decoded.address shouldBe Address("V5Ujv72h4jEBcKnALGc4fKqs6CDAPX")
        decoded.amount shouldBe Coin(1337)
    }

    @Test
    fun roundtrip() {
        val input = Output.of("V5Ujv72h4jEBcKnALGc4fKqs6CDAPX", 1337)
        val bytes = input.serialize()
        val decoded = Output.parse(ByteBuffer.wrap(bytes))
        input shouldBe decoded
    }
}
