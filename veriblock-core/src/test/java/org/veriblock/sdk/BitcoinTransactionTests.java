// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.sdk;

import org.junit.Assert;
import org.junit.Test;
import org.veriblock.sdk.models.BitcoinTransaction;
import org.veriblock.core.utilities.Utility;

import java.nio.ByteBuffer;
import java.util.Random;

public class BitcoinTransactionTests {
    @Test
    public void containsSplit_WhenDescriptorBeforeChunks() {
        Random random = new Random(100L);

        ByteBuffer buffer = ByteBuffer.allocateDirect(243);

        // Random starting bytes
        byte[] randomBytes = new byte[15];
        random.nextBytes(randomBytes);
        buffer.put(randomBytes);

        // Descriptor bytes (3 MAGIC, 1 SIZE, 7 SECTIONALS)
        buffer.put(Utility.hexToBytes("927A594624509D41F548C0"));

        // Random bytes
        randomBytes = new byte[10];
        random.nextBytes(randomBytes);
        buffer.put(randomBytes);

        // First chunk of 20 bytes
        buffer.put(Utility.hexToBytes("00000767000193093228BD2B4906F6B84BE5E618"));

        randomBytes = new byte[39];
        random.nextBytes(randomBytes);
        buffer.put(randomBytes);

        // Second chunk of 20 bytes
        buffer.put(Utility.hexToBytes("09C0522626145DDFB988022A0684E2110D384FE2"));

        randomBytes = new byte[31];
        random.nextBytes(randomBytes);
        buffer.put(randomBytes);

        // Third chunk of 21 bytes
        buffer.put(Utility.hexToBytes("BFD38549CB19C41893C258BA5B9CAB24060BA2D410"));

        randomBytes = new byte[35];
        random.nextBytes(randomBytes);
        buffer.put(randomBytes);

        // Fourth chunk of unstated 19 bytes
        buffer.put(Utility.hexToBytes("39DFC857801424B0F5DE63992A016F5F38FEB4"));

        randomBytes = new byte[22];
        random.nextBytes(randomBytes);
        buffer.put(randomBytes);

        buffer.flip();

        byte[] transaction = new byte[243];
        buffer.get(transaction);

        BitcoinTransaction test = new BitcoinTransaction(transaction);
        boolean result = test.containsSplit(Utility.hexToBytes("00000767000193093228BD2B4906F6B84BE5E61809C0522626145DDFB988022A0684E2110D384FE2BFD38549CB19C41893C258BA5B9CAB24060BA2D41039DFC857801424B0F5DE63992A016F5F38FEB4"));

        Assert.assertTrue(result);
    }
    @Test
    public void contains_WhenChunked() {
        Random random = new Random(100L);

        ByteBuffer buffer = ByteBuffer.allocateDirect(243);

        // Random starting bytes
        byte[] randomBytes = new byte[15];
        random.nextBytes(randomBytes);
        buffer.put(randomBytes);

        // Descriptor bytes (3 MAGIC, 1 SIZE, 7 SECTIONALS)
        buffer.put(Utility.hexToBytes("927A594624509D41F548C0"));

        // Random bytes
        randomBytes = new byte[10];
        random.nextBytes(randomBytes);
        buffer.put(randomBytes);

        // First chunk of 20 bytes
        buffer.put(Utility.hexToBytes("00000767000193093228BD2B4906F6B84BE5E618"));

        randomBytes = new byte[39];
        random.nextBytes(randomBytes);
        buffer.put(randomBytes);

        // Second chunk of 20 bytes
        buffer.put(Utility.hexToBytes("09C0522626145DDFB988022A0684E2110D384FE2"));

        randomBytes = new byte[31];
        random.nextBytes(randomBytes);
        buffer.put(randomBytes);

        // Third chunk of 21 bytes
        buffer.put(Utility.hexToBytes("BFD38549CB19C41893C258BA5B9CAB24060BA2D410"));

        randomBytes = new byte[35];
        random.nextBytes(randomBytes);
        buffer.put(randomBytes);

        // Fourth chunk of unstated 19 bytes
        buffer.put(Utility.hexToBytes("39DFC857801424B0F5DE63992A016F5F38FEB4"));

        randomBytes = new byte[22];
        random.nextBytes(randomBytes);
        buffer.put(randomBytes);

        buffer.flip();

        byte[] transaction = new byte[243];
        buffer.get(transaction);

        BitcoinTransaction test = new BitcoinTransaction(transaction);
        boolean result = test.contains(Utility.hexToBytes("00000767000193093228BD2B4906F6B84BE5E61809C0522626145DDFB988022A0684E2110D384FE2BFD38549CB19C41893C258BA5B9CAB24060BA2D41039DFC857801424B0F5DE63992A016F5F38FEB4"));

        Assert.assertTrue(result);
    }
}
