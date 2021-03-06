// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.spv.model

import io.kotest.matchers.shouldBe
import org.junit.Before
import org.junit.Test
import org.veriblock.core.Context
import org.veriblock.core.params.getDefaultNetworkParameters
import org.veriblock.core.utilities.Utility
import org.veriblock.sdk.services.SerializeDeserializeService
import java.math.BigInteger
import java.nio.ByteBuffer

class StoredVeriBlockBlockTest {
    private lateinit var storedVeriBlockBlockExpected: StoredVeriBlockBlock
    private lateinit var raw: ByteArray

    @Before
    fun setUp() {
        Context.create(getDefaultNetworkParameters("mainnet"))
        raw = Utility.hexToBytes("00001388000294e7dc3e3be21a96eccf0fbdf5f62a3331dc995c36b0935637860679ddd5db0f135312b2c27867c9a83ef1b99b985c9b949307023ad672bafd77")
        val block = SerializeDeserializeService.parseVeriBlockBlock(raw)
        storedVeriBlockBlockExpected = StoredVeriBlockBlock(
            block, BigInteger.TEN, block.hash
        )
    }

    @Test
    fun serializeAndDeserialize() {
        val bytes = storedVeriBlockBlockExpected.serialize()
        val storedVeriBlockBlockActual = bytes.deserializeStoredVeriBlockBlock()
        storedVeriBlockBlockActual shouldBe storedVeriBlockBlockExpected
    }

    @Test
    fun serializeAndDeserializeWithBuffer() {
        val buffer = ByteBuffer.allocateDirect(StoredVeriBlockBlock.SIZE)
        storedVeriBlockBlockExpected.serialize(buffer)
        buffer.flip()
        val storedVeriBlockBlockActual = buffer.deserializeStoredVeriBlockBlock()
        storedVeriBlockBlockActual shouldBe storedVeriBlockBlockExpected
    }
}
