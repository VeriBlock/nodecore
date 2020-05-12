// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.sdk.blockchain.store;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.veriblock.sdk.models.Sha256Hash;
import org.veriblock.sdk.services.SerializeDeserializeService;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Base64;

public class StoredBitcoinBlockTest {

    private StoredBitcoinBlock storedBitcoinBlockExpected;
    private byte[] raw;

    @Before
    public void setUp() {
        this.raw = Base64.getDecoder().decode("AAAAIPfeKZWJiACrEJr5Z3m5eaYHFdqb8ru3RbMAAAAAAAAA+FSGAmv06tijekKSUzLsi1U/jjEJdP6h66I4987mFl4iE7dchBoBGi4A8po=");
        this.storedBitcoinBlockExpected = new StoredBitcoinBlock(SerializeDeserializeService.parseBitcoinBlock(raw), BigInteger.TEN, 0);
    }

    @Test
    public void serializeAndDeserialize() {
        byte[] bytes = storedBitcoinBlockExpected.serialize();
        StoredBitcoinBlock storedBitcoinBlockActual = StoredBitcoinBlock.deserialize(bytes);
        Assert.assertEquals(storedBitcoinBlockExpected, storedBitcoinBlockActual);
    }

    @Test
    public void serializeAndDeserializeWithBufferWithoutHash() {
        ByteBuffer buffer = ByteBuffer.allocateDirect(StoredBitcoinBlock.SIZE);
        storedBitcoinBlockExpected.serialize(buffer);
        buffer.flip();

        StoredBitcoinBlock storedBitcoinBlockActual = StoredBitcoinBlock.deserializeWithoutHash(buffer);
        Assert.assertEquals(storedBitcoinBlockExpected, storedBitcoinBlockActual);
    }

    @Test
    public void serializeAndDeserializeWithBuffer() {
        ByteBuffer buffer = ByteBuffer.allocateDirect(StoredBitcoinBlock.SIZE);
        storedBitcoinBlockExpected.serialize(buffer);
        buffer.flip();
        //Skip Hash
        buffer.position(buffer.position() + Sha256Hash.BITCOIN_LENGTH);

        StoredBitcoinBlock storedBitcoinBlockActual = StoredBitcoinBlock.deserialize(buffer);
        Assert.assertEquals(storedBitcoinBlockExpected, storedBitcoinBlockActual);
    }

}