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
import org.veriblock.sdk.models.VBlakeHash;
import org.veriblock.sdk.services.SerializeDeserializeService;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Base64;

public class StoredVeriBlockBlockTest {

    private StoredVeriBlockBlock storedVeriBlockBlockExpected;
    private byte[] raw;

    @Before
    public void setUp() {
        this.raw = Base64.getDecoder().decode("AAATiAAClOfcPjviGpbszw+99fYqMzHcmVw2sJNWN4YGed3V2w8TUxKywnhnyag+8bmbmFyblJMHAjrWcrr9dw==");
        this.storedVeriBlockBlockExpected = new StoredVeriBlockBlock(SerializeDeserializeService.parseVeriBlockBlock(raw), BigInteger.TEN);
    }

    @Test
    public void serializeAndDeserialize() {
        byte[] bytes = storedVeriBlockBlockExpected.serialize();
        StoredVeriBlockBlock storedVeriBlockBlockActual = StoredVeriBlockBlock.deserialize(bytes);

        Assert.assertEquals(storedVeriBlockBlockExpected, storedVeriBlockBlockActual);
    }

    @Test
    public void serializeAndDeserializeWithBuffer() {
        ByteBuffer buffer = ByteBuffer.allocateDirect(StoredVeriBlockBlock.SIZE);
        storedVeriBlockBlockExpected.serialize(buffer);
        buffer.flip();
        buffer.position(buffer.position() + VBlakeHash.VERIBLOCK_LENGTH);

        StoredVeriBlockBlock storedVeriBlockBlockActual = StoredVeriBlockBlock.deserialize(buffer);
        Assert.assertEquals(storedVeriBlockBlockExpected, storedVeriBlockBlockActual);
    }

    @Test
    public void serializeAndDeserializeWithBufferWithoutHash() {
        ByteBuffer buffer = ByteBuffer.allocateDirect(StoredVeriBlockBlock.SIZE);
        storedVeriBlockBlockExpected.serialize(buffer);
        buffer.flip();

        StoredVeriBlockBlock storedVeriBlockBlockActual = StoredVeriBlockBlock.deserializeWithoutHash(buffer);
        Assert.assertEquals(storedVeriBlockBlockExpected, storedVeriBlockBlockActual);
    }

}