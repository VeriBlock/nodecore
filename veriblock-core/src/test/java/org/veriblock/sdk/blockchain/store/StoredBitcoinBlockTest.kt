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
import org.veriblock.core.crypto.Sha256Hash
import org.veriblock.core.utilities.extensions.asBase64Bytes
import org.veriblock.sdk.blockchain.store.StoredBitcoinBlock.Companion.deserialize
import org.veriblock.sdk.blockchain.store.StoredBitcoinBlock.Companion.deserializeWithoutHash
import org.veriblock.sdk.services.SerializeDeserializeService
import java.math.BigInteger
import java.nio.ByteBuffer
import java.util.Base64

class StoredBitcoinBlockTest {
    private lateinit var storedBitcoinBlockExpected: StoredBitcoinBlock
    private lateinit var raw: ByteArray

    @Before
    fun setUp() {
        raw = "AAAAIPfeKZWJiACrEJr5Z3m5eaYHFdqb8ru3RbMAAAAAAAAA+FSGAmv06tijekKSUzLsi1U/jjEJdP6h66I4987mFl4iE7dchBoBGi4A8po=".asBase64Bytes()
        storedBitcoinBlockExpected = StoredBitcoinBlock(SerializeDeserializeService.parseBitcoinBlock(raw), BigInteger.TEN, 0)
    }

    @Test
    fun serializeAndDeserialize() {
        val bytes = storedBitcoinBlockExpected.serialize()
        val storedBitcoinBlockActual = deserialize(bytes)
        Assert.assertEquals(storedBitcoinBlockExpected, storedBitcoinBlockActual)
    }

    @Test
    fun serializeAndDeserializeWithBufferWithoutHash() {
        val buffer = ByteBuffer.allocateDirect(StoredBitcoinBlock.SIZE)
        storedBitcoinBlockExpected.serialize(buffer)
        buffer.flip()
        val storedBitcoinBlockActual = deserializeWithoutHash(buffer)
        Assert.assertEquals(storedBitcoinBlockExpected, storedBitcoinBlockActual)
    }

    @Test
    fun serializeAndDeserializeWithBuffer() {
        val buffer = ByteBuffer.allocateDirect(StoredBitcoinBlock.SIZE)
        storedBitcoinBlockExpected.serialize(buffer)
        buffer.flip()
        //Skip Hash
        buffer.position(buffer.position() + Sha256Hash.BITCOIN_LENGTH)
        val storedBitcoinBlockActual = deserialize(buffer)
        Assert.assertEquals(storedBitcoinBlockExpected, storedBitcoinBlockActual)
    }
}
