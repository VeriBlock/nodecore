// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.contracts;

import nodecore.miners.pop.common.Utility;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class PoPMiningInstructionTests {
    private PoPMiningInstruction data;

    @Before
    public void before() {
        data = new PoPMiningInstruction();
        data.publicationData = Utility.hexToBytes(
                "000000010000000000000000000000000000000000000000000000002d12960b6cc8a7021286ad682f0cbefa4babc4adc613b6925aaaa1d304017d78001d415c007bccf350d249a488c6fe8621b41723");
        data.endorsedBlockHeader = Utility.hexToBytes(
                "000000010000000000000000000000000000000000000000000000002d12960b6cc8a7021286ad682f0cbefa4babc4adc613b6925aaaa1d304017d78001d415c");
        data.lastBitcoinBlock = Utility.hexToBytes(
                "0000002018a0b6b0013ac826c28ca5e5f8f1ee2d30c1302c1c2aac2ee004d44f5bd4fb428fb04efbf3927784bed987b3a1f319434ec44f331302c11db158a2f9416dc7838b61ce5affff7f2001000000");
        data.minerAddress = Utility.hexToBytes("672a9d1281cc9d25bfb4539145702bfb2d8ae61dbbc0");
    }

    @Test
    public void serializationWhenAllPropertiesSet() {
        PoPMiningInstruction returned = serializeThenDeserialize(data);

        Assert.assertArrayEquals(data.endorsedBlockHeader, returned.endorsedBlockHeader);
        Assert.assertArrayEquals(data.lastBitcoinBlock, returned.lastBitcoinBlock);
        Assert.assertArrayEquals(data.minerAddress, returned.minerAddress);
        Assert.assertArrayEquals(data.publicationData, returned.publicationData);
    }

    @Test
    public void serializationWhenNullEndorsedBlockHeader() {
        data.endorsedBlockHeader = null;
        PoPMiningInstruction returned = serializeThenDeserialize(data);

        Assert.assertNull(returned.endorsedBlockHeader);
        Assert.assertArrayEquals(data.lastBitcoinBlock, returned.lastBitcoinBlock);
        Assert.assertArrayEquals(data.minerAddress, returned.minerAddress);
        Assert.assertArrayEquals(data.publicationData, returned.publicationData);
    }

    @Test
    public void serializationWhenNullLastBitcoinBlock() {
        data.lastBitcoinBlock = null;
        PoPMiningInstruction returned = serializeThenDeserialize(data);

        Assert.assertArrayEquals(data.endorsedBlockHeader, returned.endorsedBlockHeader);
        Assert.assertNull(returned.lastBitcoinBlock);
        Assert.assertArrayEquals(data.minerAddress, returned.minerAddress);
        Assert.assertArrayEquals(data.publicationData, returned.publicationData);
    }

    @Test
    public void serializationWhenNullMinerAddress() {
        data.minerAddress = null;
        PoPMiningInstruction returned = serializeThenDeserialize(data);

        Assert.assertArrayEquals(data.endorsedBlockHeader, returned.endorsedBlockHeader);
        Assert.assertArrayEquals(data.lastBitcoinBlock, returned.lastBitcoinBlock);
        Assert.assertNull(returned.minerAddress);
        Assert.assertArrayEquals(data.publicationData, returned.publicationData);
    }

    @Test
    public void serializationWhenNullPublicationData() {
        data.publicationData = null;
        PoPMiningInstruction returned = serializeThenDeserialize(data);

        Assert.assertArrayEquals(data.endorsedBlockHeader, returned.endorsedBlockHeader);
        Assert.assertArrayEquals(data.lastBitcoinBlock, returned.lastBitcoinBlock);
        Assert.assertArrayEquals(data.minerAddress, returned.minerAddress);
        Assert.assertNull(returned.publicationData);
    }

    private PoPMiningInstruction serializeThenDeserialize(PoPMiningInstruction input) {
        byte[] serialized = new byte[0];
        try (ByteArrayOutputStream dataOut = new ByteArrayOutputStream()) {
            try (ObjectOutputStream out = new ObjectOutputStream(dataOut)) {
                out.writeObject(this.data);

                serialized = dataOut.toByteArray();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try (ByteArrayInputStream dataIn = new ByteArrayInputStream(serialized)) {
            try (ObjectInputStream in = new ObjectInputStream(dataIn)) {
                return (PoPMiningInstruction) in.readObject();
            }
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
