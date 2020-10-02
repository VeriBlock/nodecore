// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.sdk.blockchain.store

import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.veriblock.core.Context
import org.veriblock.core.crypto.VBlakeHash
import org.veriblock.core.params.defaultTestNetParameters
import org.veriblock.core.utilities.extensions.asBase64Bytes
import org.veriblock.sdk.blockchain.store.StoredVeriBlockBlock.Companion.deserialize
import org.veriblock.sdk.services.SerializeDeserializeService
import java.math.BigInteger
import java.nio.ByteBuffer
import java.util.Base64

class StoredVeriBlockBlockTest {
    private lateinit var storedVeriBlockBlockExpected: StoredVeriBlockBlock
    private lateinit var raw: ByteArray

    @Before
    fun setUp() {
        Context.create(defaultTestNetParameters)
        raw = "AAATiAAClOfcPjviGpbszw+99fYqMzHcmVw2sJNWN4YGed3V2w8TUxKywnhnyag+8bmbmFyblJMHAjrWcrr9dw==".asBase64Bytes()
        val block = SerializeDeserializeService.parseVeriBlockBlock(raw)
        storedVeriBlockBlockExpected = StoredVeriBlockBlock(
            block, BigInteger.TEN, block.hash
        )
    }

    @Test
    fun serializeAndDeserialize() {
        val bytes = storedVeriBlockBlockExpected.serialize()
        val storedVeriBlockBlockActual = deserialize(
            bytes
        )
        Assert.assertEquals(storedVeriBlockBlockExpected, storedVeriBlockBlockActual)
    }

    @Test
    fun serializeAndDeserializeWithBuffer() {
        val buffer = ByteBuffer.allocateDirect(StoredVeriBlockBlock.SIZE)
        storedVeriBlockBlockExpected.serialize(buffer)
        buffer.flip()
        buffer.position(buffer.position() + VBlakeHash.VERIBLOCK_LENGTH)
        val storedVeriBlockBlockActual = deserialize(
            buffer
        )
        Assert.assertEquals(storedVeriBlockBlockExpected, storedVeriBlockBlockActual)
    }
}
