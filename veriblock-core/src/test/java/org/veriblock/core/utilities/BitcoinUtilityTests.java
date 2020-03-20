// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.core.utilities;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.veriblock.core.Context;
import org.veriblock.core.bitcoinj.BitcoinUtilities;
import org.veriblock.core.params.AlphaNetParameters;
import org.veriblock.core.types.BitString;

public class BitcoinUtilityTests {
    @BeforeClass
    public static void setupFixture() {
        Context.create(new AlphaNetParameters());
    }

    @Test
    public void embeddedDataUtilityDescriptorBeforeChunks() {
        String transactionBytes = "927A59"; // Magic bytes

        String descriptorBits = "0100"; // Four total chunks
        descriptorBits += "01"; // Eight bits offset distance
        descriptorBits += "10"; // Six bit descriptor length
        descriptorBits += "00100100"; // First chunk begins 36 bytes from start
        descriptorBits += "010100"; // First chunk is 20 bytes long
        descriptorBits += "00100111"; // Second chunk begins 39 bytes after first chunk ends
        descriptorBits += "010100"; // Second chunk is 20 bytes long
        descriptorBits += "00011111"; // Third chunk begins 31 bytes after second chunk ends
        descriptorBits += "010101"; // Third chunk is 21 bytes long
        descriptorBits += "00100011"; // Fourth chunk is 35 bytes after third chunk ends
        // Implied that fourth chunk is 19 bytes long (80 - (20 + 20 + 21))

        while (descriptorBits.length() % 8 != 0) {
            descriptorBits += "0";
        }

        BitString descriptor = new BitString(descriptorBits);
        BitStringReader descriptorReader = new BitStringReader(descriptor);

        transactionBytes += Utility.bytesToHex(descriptorReader.readBits(descriptorReader.remaining()));

        int totalTransactionSize = transactionBytes.length() / 2;

        // first chunk is 36 bytes from start, so get transaction to 36 bytes
        for (int i = totalTransactionSize; i < 36; i++) {
            transactionBytes += Utility.bytesToHex(new byte[]{(byte)i});
        }

        // First chunk; 20 bytes of a header
        transactionBytes += "00000767000193093228BD2B4906F6B84BE5E618";

        // 39 bytes of arbitrary data; second chunk begins 39 bytes after first chunk ends
        for (int i = 0; i <= 38; i++) {
            transactionBytes += Utility.bytesToHex(new byte[]{(byte)(i * 2 /* Arbitrary byte */)});
        }

        // Second chunk; 20 bytes of a header
        transactionBytes += "09C0522626145DDFB988022A0684E2110D384FE2";


        // 31 bytes of arbitrary data; third chunk begins 31 bytes after second chunk ends
        for (int i = 0; i <= 30; i++) {
            transactionBytes += Utility.bytesToHex(new byte[]{(byte)(i * 4 /* Arbitrary byte */)});
        }

        // Third chunk; 21 bytes of a header
        transactionBytes += "BFD38549CB19C41893C258BA5B9CAB24060BA2D410";

        // 35 bytes of arbitrary data; fourth chunk begins 35 bytes after third chunk ends
        for (int i = 0; i <= 34; i++) {
            transactionBytes += Utility.bytesToHex(new byte[]{(byte)(i * 19 /* Arbitrary byte */)});
        }

        // Fourth chunk
        transactionBytes += "39DFC8"; // last 3 bytes of header
        transactionBytes += "57801424B0F5DE63992A016F5F38FEB4"; // PoP miner identification (V6P56zKcXNexRSeWcrtX8vpoJEZJRy)

        byte[] embeddedData = BitcoinUtilities.extractPoPData(Utility.hexToBytes(transactionBytes));
        Assert.assertTrue(Utility.byteArraysAreEqual(embeddedData, Utility.hexToBytes("00000767000193093228BD2B4906F6B84BE5E61809C0522626145DDFB988022A0684E2110D384FE2BFD38549CB19C41893C258BA5B9CAB24060BA2D41039DFC857801424B0F5DE63992A016F5F38FEB4")));

        // And test with it padded
        for (int i = 0; i <= 38; i++) {
            transactionBytes += Utility.bytesToHex(new byte[]{(byte)(i * 11 /* Arbitrary byte */)});
        }

        embeddedData = BitcoinUtilities.extractPoPData(Utility.hexToBytes(transactionBytes));
        Assert.assertTrue(Utility.byteArraysAreEqual(embeddedData, Utility.hexToBytes("00000767000193093228BD2B4906F6B84BE5E61809C0522626145DDFB988022A0684E2110D384FE2BFD38549CB19C41893C258BA5B9CAB24060BA2D41039DFC857801424B0F5DE63992A016F5F38FEB4")));
    }

    @Test
    public void embeddedDataUtilityDescriptorBeforeChunksWithIgnoredMalformattedDescriptor() {
        String malformattedDescriptorBits = "0100"; // Four total chunks
        malformattedDescriptorBits += "01"; // Eight bits offset distance
        malformattedDescriptorBits += "10"; // Six bit descriptor length
        malformattedDescriptorBits += "00100100"; // First chunk begins 36 bytes from start
        malformattedDescriptorBits += "010100"; // First chunk is 20 bytes long
        malformattedDescriptorBits += "00100111"; // Second chunk begins 39 bytes after first chunk ends
        malformattedDescriptorBits += "010100"; // Second chunk is 20 bytes long
        malformattedDescriptorBits += "00011110"; // Third chunk begins 30 bytes after second chunk ends [incorrect/malformatted]
        malformattedDescriptorBits += "010101"; // Third chunk is 21 bytes long
        malformattedDescriptorBits += "00100011"; // Fourth chunk is 35 bytes after third chunk ends
        // Implied that fourth chunk is 19 bytes long (80 - (20 + 20 + 21))

        while (malformattedDescriptorBits.length() % 8 != 0) {
            malformattedDescriptorBits += "0";
        }


        BitString malformattedDscriptor = new BitString(malformattedDescriptorBits);
        BitStringReader malformattedDescriptorReader = new BitStringReader(malformattedDscriptor);

        // Write bad descriptor first
        String transactionBytes = "927A59"; // Magic bytes
        transactionBytes += Utility.bytesToHex(malformattedDescriptorReader.readBits(malformattedDescriptorReader.remaining()));

        // Write good descriptor second
        transactionBytes += "927A59"; // Magic bytes

        String descriptorBits = "0100"; // Four total chunks
        descriptorBits += "01"; // Eight bits offset distance
        descriptorBits += "10"; // Six bit descriptor length
        descriptorBits += "00100100"; // First chunk begins 36 bytes from start
        descriptorBits += "010100"; // First chunk is 20 bytes long
        descriptorBits += "00100111"; // Second chunk begins 39 bytes after first chunk ends
        descriptorBits += "010100"; // Second chunk is 20 bytes long
        descriptorBits += "00011111"; // Third chunk begins 31 bytes after second chunk ends
        descriptorBits += "010101"; // Third chunk is 21 bytes long
        descriptorBits += "00100011"; // Fourth chunk is 35 bytes after third chunk ends
        // Implied that fourth chunk is 19 bytes long (80 - (20 + 20 + 21))

        while (descriptorBits.length() % 8 != 0) {
            descriptorBits += "0";
        }

        BitString descriptor = new BitString(descriptorBits);
        BitStringReader descriptorReader = new BitStringReader(descriptor);

        transactionBytes += Utility.bytesToHex(descriptorReader.readBits(descriptorReader.remaining()));

        int totalTransactionSize = transactionBytes.length() / 2;

        // first chunk is 36 bytes from start, so get transaction to 36 bytes
        for (int i = totalTransactionSize; i < 36; i++) {
            transactionBytes += Utility.bytesToHex(new byte[]{(byte)i});
        }

        // First chunk; 20 bytes of a header
        transactionBytes += "00000767000193093228BD2B4906F6B84BE5E618";

        // 39 bytes of arbitrary data; second chunk begins 39 bytes after first chunk ends
        for (int i = 0; i <= 38; i++) {
            transactionBytes += Utility.bytesToHex(new byte[]{(byte)(i * 2 /* Arbitrary byte */)});
        }

        // Second chunk; 20 bytes of a header
        transactionBytes += "09C0522626145DDFB988022A0684E2110D384FE2";


        // 31 bytes of arbitrary data; third chunk begins 31 bytes after second chunk ends
        for (int i = 0; i <= 30; i++) {
            transactionBytes += Utility.bytesToHex(new byte[]{(byte)(i * 4 /* Arbitrary byte */)});
        }

        // Third chunk; 21 bytes of a header
        transactionBytes += "BFD38549CB19C41893C258BA5B9CAB24060BA2D410";

        // 35 bytes of arbitrary data; fourth chunk begins 35 bytes after third chunk ends
        for (int i = 0; i <= 34; i++) {
            transactionBytes += Utility.bytesToHex(new byte[]{(byte)(i * 19 /* Arbitrary byte */)});
        }

        // Fourth chunk
        transactionBytes += "39DFC8"; // last 3 bytes of header
        transactionBytes += "57801424B0F5DE63992A016F5F38FEB4"; // PoP miner identification (V6P56zKcXNexRSeWcrtX8vpoJEZJRy)


        byte[] embeddedData = BitcoinUtilities.extractPoPData(Utility.hexToBytes(transactionBytes));
        Assert.assertTrue(Utility.byteArraysAreEqual(embeddedData, Utility.hexToBytes("00000767000193093228BD2B4906F6B84BE5E61809C0522626145DDFB988022A0684E2110D384FE2BFD38549CB19C41893C258BA5B9CAB24060BA2D41039DFC857801424B0F5DE63992A016F5F38FEB4")));

        // And test with it padded
        for (int i = 0; i <= 38; i++) {
            transactionBytes += Utility.bytesToHex(new byte[]{(byte)(i * 11 /* Arbitrary byte */)});
        }

        embeddedData = BitcoinUtilities.extractPoPData(Utility.hexToBytes(transactionBytes));
        Assert.assertTrue(Utility.byteArraysAreEqual(embeddedData, Utility.hexToBytes("00000767000193093228BD2B4906F6B84BE5E61809C0522626145DDFB988022A0684E2110D384FE2BFD38549CB19C41893C258BA5B9CAB24060BA2D41039DFC857801424B0F5DE63992A016F5F38FEB4")));
    }

    @Test
    @Ignore
    public void embeddedDataUtilityDescriptorBeforeChunksLongOffsetAndDescriptors() {
        StringBuilder transactionBytes = new StringBuilder();

        transactionBytes.append("927A59"); // Magic bytes

        String descriptorBits = "0100"; // Four total chunks
        descriptorBits += "11"; // Sixteen bits offset distance
        descriptorBits += "11"; // Seven bit descriptor length
        descriptorBits += "0001000000100100"; // First chunk begins 4132 bytes from start
        descriptorBits += "0010100"; // First chunk is 20 bytes long
        descriptorBits += "0000000000100111"; // Second chunk begins 39 bytes after first chunk ends
        descriptorBits += "0010100"; // Second chunk is 20 bytes long
        descriptorBits += "0010000000011111"; // Third chunk begins 8223 bytes after second chunk ends
        descriptorBits += "0010101"; // Third chunk is 21 bytes long
        descriptorBits += "0000000000100011"; // Fourth chunk is 35 bytes after third chunk ends
        // Implied that fourth chunk is 19 bytes long (80 - (20 + 20 + 21))

        while (descriptorBits.length() % 8 != 0) {
            descriptorBits += "0";
        }

        BitString descriptor = new BitString(descriptorBits);
        BitStringReader descriptorReader = new BitStringReader(descriptor);

        transactionBytes.append(Utility.bytesToHex(descriptorReader.readBits(descriptorReader.remaining())));

        int totalTransactionSize = transactionBytes.length() / 2;

        // first chunk is 4132 bytes from start, so get transaction to 4132 bytes
        for (int i = totalTransactionSize; i < 4132; i++) {
            transactionBytes.append(Utility.bytesToHex(new byte[]{(byte) i}));
        }

        // First chunk; 20 bytes of a header
        transactionBytes.append("00000767000193093228BD2B4906F6B84BE5E618");

        // 39 bytes of arbitrary data; second chunk begins 39 bytes after first chunk ends
        for (int i = 0; i <= 38; i++) {
            transactionBytes.append(Utility.bytesToHex(new byte[]{(byte) (i * 2 /* Arbitrary byte */)}));
        }

        // Second chunk; 20 bytes of a header
        transactionBytes.append("09C0522626145DDFB988022A0684E2110D384FE2");


        // 31 bytes of arbitrary data; third chunk begins 8223 bytes after second chunk ends
        for (int i = 0; i <= 8222; i++) {
            transactionBytes.append(Utility.bytesToHex(new byte[]{(byte) (i * 4 /* Arbitrary byte */)}));
        }

        // Third chunk; 21 bytes of a header
        transactionBytes.append("BFD38549CB19C41893C258BA5B9CAB24060BA2D410");

        // 35 bytes of arbitrary data; fourth chunk begins 35 bytes after third chunk ends
        for (int i = 0; i <= 34; i++) {
            transactionBytes.append(Utility.bytesToHex(new byte[]{(byte) (i * 19 /* Arbitrary byte */)}));
        }

        // Fourth chunk
        transactionBytes.append("39DFC8"); // last 3 bytes of header
        transactionBytes.append("57801424B0F5DE63992A016F5F38FEB4"); // PoP miner identification (V6P56zKcXNexRSeWcrtX8vpoJEZJRy)

        byte[] embeddedData = BitcoinUtilities.extractPoPData(Utility.hexToBytes(transactionBytes.toString()));
        Assert.assertTrue(Utility.byteArraysAreEqual(embeddedData, Utility.hexToBytes("00000767000193093228BD2B4906F6B84BE5E61809C0522626145DDFB988022A0684E2110D384FE2BFD38549CB19C41893C258BA5B9CAB24060BA2D41039DFC857801424B0F5DE63992A016F5F38FEB4")));

        // And test with it padded
        for (int i = 0; i <= 38; i++) {
            transactionBytes.append(Utility.bytesToHex(new byte[]{(byte) (i * 12 /* Arbitrary byte */)}));
        }


        embeddedData = BitcoinUtilities.extractPoPData(Utility.hexToBytes(transactionBytes.toString()));
        Assert.assertTrue(Utility.byteArraysAreEqual(embeddedData, Utility.hexToBytes("00000767000193093228BD2B4906F6B84BE5E61809C0522626145DDFB988022A0684E2110D384FE2BFD38549CB19C41893C258BA5B9CAB24060BA2D41039DFC857801424B0F5DE63992A016F5F38FEB4")));
    }

    @Test
    public void embeddedDataUtilityDescriptorContiguousChunks() {
        String transactionBytes = "927A59"; // Magic bytes

        String descriptorBits = "0011"; // Three total chunks
        descriptorBits += "01"; // Eight bits offset distance
        descriptorBits += "10"; // Six bit descriptor length
        descriptorBits += "00011111"; // First chunk begins 31 bytes from start
        descriptorBits += "011110"; // First chunk is 20 bytes long
        descriptorBits += "00000000"; // Second chunk begins 0 bytes after first chunk ends (contiguous)
        descriptorBits += "011110"; // Second chunk is 30 bytes long
        descriptorBits += "00000001"; // Third chunk begins 1 byte after second chunk ends
        // Implied that third chunk is 20 bytes long (80 - (30 + 30))

        while (descriptorBits.length() % 8 != 0) {
            descriptorBits += "0";
        }

        BitString descriptor = new BitString(descriptorBits);
        BitStringReader descriptorReader = new BitStringReader(descriptor);

        transactionBytes += Utility.bytesToHex(descriptorReader.readBits(descriptorReader.remaining()));

        int totalTransactionSize = transactionBytes.length() / 2;

        // first chunk is 31 bytes from start, so get transaction to 31 bytes
        for (int i = totalTransactionSize; i < 31; i++) {
            transactionBytes += Utility.bytesToHex(new byte[]{(byte)i});
        }

        // First chunk; 30 bytes of a header
        transactionBytes += "00000767000193093228BD2B4906F6B84BE5E61809C0522626145DDFB988";

        // 39 bytes of arbitrary data; second chunk begins 39 bytes after first chunk ends
        for (int i = 0; i < 0; i++) {
            transactionBytes += Utility.bytesToHex(new byte[]{(byte)(i * 0 /* Arbitrary byte */)});
        }

        // Second chunk; 30 bytes of a header
        transactionBytes += "022A0684E2110D384FE2BFD38549CB19C41893C258BA5B9CAB24060BA2D4";


        // 31 bytes of arbitrary data; third chunk begins 31 bytes after second chunk ends
        for (int i = 0; i < 0; i++) {
            transactionBytes += Utility.bytesToHex(new byte[]{(byte)(i * 0 /* Arbitrary byte */)});
        }

        // Extra 1 byte
        transactionBytes += "FF";

        // Third chunk
        transactionBytes += "1039DFC8"; // last 4 bytes of header
        transactionBytes += "57801424B0F5DE63992A016F5F38FEB4"; // PoP miner identification (V6P56zKcXNexRSeWcrtX8vpoJEZJRy)

        byte[] embeddedData = BitcoinUtilities.extractPoPData(Utility.hexToBytes(transactionBytes));
        Assert.assertTrue(Utility.byteArraysAreEqual(embeddedData, Utility.hexToBytes("00000767000193093228BD2B4906F6B84BE5E61809C0522626145DDFB988022A0684E2110D384FE2BFD38549CB19C41893C258BA5B9CAB24060BA2D41039DFC857801424B0F5DE63992A016F5F38FEB4")));
    }

    @Test
    public void embeddedDataUtilityDescriptorOneChunkMagicAtStart() {
        String transactionBytes = "927A59"; // Magic bytes

        String descriptorBits = "0001"; // One total chunk
        descriptorBits += "01"; // Eight bits offset distance
        descriptorBits += "10"; // Six bit descriptor length (irrelevant, as the only chunk length is implied)
        descriptorBits += "00011111"; // First chunk begins 31 bytes from start
        // Implied that first chunk is 80 bytes long

        while (descriptorBits.length() % 8 != 0) {
            descriptorBits += "0";
        }

        BitString descriptor = new BitString(descriptorBits);
        BitStringReader descriptorReader = new BitStringReader(descriptor);

        transactionBytes += Utility.bytesToHex(descriptorReader.readBits(descriptorReader.remaining()));

        int totalTransactionSize = transactionBytes.length() / 2;

        // first chunk is 31 bytes from start, so get transaction to 31 bytes
        for (int i = totalTransactionSize; i < 31; i++) {
            transactionBytes += Utility.bytesToHex(new byte[]{(byte)i});
        }

        // First chunk; 64 bytes of a header + 16 bytes PoP miner identification
        transactionBytes += "00000767000193093228BD2B4906F6B84BE5E61809C0522626145DDFB988022A0684E2110D384FE2BFD38549CB19C41893C258BA5B9CAB24060BA2D41039DFC857801424B0F5DE63992A016F5F38FEB4";

        byte[] embeddedData = BitcoinUtilities.extractPoPData(Utility.hexToBytes(transactionBytes));
        Assert.assertTrue(Utility.byteArraysAreEqual(embeddedData, Utility.hexToBytes("00000767000193093228BD2B4906F6B84BE5E61809C0522626145DDFB988022A0684E2110D384FE2BFD38549CB19C41893C258BA5B9CAB24060BA2D41039DFC857801424B0F5DE63992A016F5F38FEB4")));

        // Random bytes
        transactionBytes += "00FFAC03B1C7";

        // And test with the end padded
        embeddedData = BitcoinUtilities.extractPoPData(Utility.hexToBytes(transactionBytes));
        Assert.assertTrue(Utility.byteArraysAreEqual(embeddedData, Utility.hexToBytes("00000767000193093228BD2B4906F6B84BE5E61809C0522626145DDFB988022A0684E2110D384FE2BFD38549CB19C41893C258BA5B9CAB24060BA2D41039DFC857801424B0F5DE63992A016F5F38FEB4")));
    }

    @Test
    public void embeddedDataUtilityDescriptorOneChunkAtBeginningMagicAtEnd() {
        String descriptorBytes = "927A59"; // Magic bytes

        String descriptorBits = "0001"; // One total chunk
        descriptorBits += "00"; // Four bits offset distance
        descriptorBits += "10"; // Six bit descriptor length (irrelevant, as the only chunk length is implied)
        descriptorBits += "0000"; // First chunk begins 0 bytes from start
        // Implied that first chunk is 80 bytes long

        while (descriptorBits.length() % 8 != 0) {
            descriptorBits += "0";
        }

        BitString descriptor = new BitString(descriptorBits);
        BitStringReader descriptorReader = new BitStringReader(descriptor);

        descriptorBytes += Utility.bytesToHex(descriptorReader.readBits(descriptorReader.remaining()));

        // First chunk; 64 bytes of a header + 16 bytes PoP miner identification
        String transactionBytes = "00000767000193093228BD2B4906F6B84BE5E61809C0522626145DDFB988022A0684E2110D384FE2BFD38549CB19C41893C258BA5B9CAB24060BA2D41039DFC857801424B0F5DE63992A016F5F38FEB4";

        transactionBytes += descriptorBytes;

        byte[] embeddedData = BitcoinUtilities.extractPoPData(Utility.hexToBytes(transactionBytes));
        Assert.assertTrue(Utility.byteArraysAreEqual(embeddedData, Utility.hexToBytes("00000767000193093228BD2B4906F6B84BE5E61809C0522626145DDFB988022A0684E2110D384FE2BFD38549CB19C41893C258BA5B9CAB24060BA2D41039DFC857801424B0F5DE63992A016F5F38FEB4")));


        // Random bytes
        transactionBytes += "00FFAC03B1C7";

        // And test with the end padded
        embeddedData = BitcoinUtilities.extractPoPData(Utility.hexToBytes(transactionBytes));
        Assert.assertTrue(Utility.byteArraysAreEqual(embeddedData, Utility.hexToBytes("00000767000193093228BD2B4906F6B84BE5E61809C0522626145DDFB988022A0684E2110D384FE2BFD38549CB19C41893C258BA5B9CAB24060BA2D41039DFC857801424B0F5DE63992A016F5F38FEB4")));
    }

    @Test
    public void embeddedDataUtilityDescriptorOneChunkNearBeginningMagicAtEnd() {
        String descriptorBytes = "927A59"; // Magic bytes

        String descriptorBits = "0001"; // One total chunk
        descriptorBits += "00"; // Four bits offset distance
        descriptorBits += "10"; // Six bit descriptor length (irrelevant, as the only chunk length is implied)
        descriptorBits += "1111"; // First chunk begins 15 bytes from start
        // Implied that first chunk is 80 bytes long

        while (descriptorBits.length() % 8 != 0) {
            descriptorBits += "0";
        }

        BitString descriptor = new BitString(descriptorBits);
        BitStringReader descriptorReader = new BitStringReader(descriptor);

        descriptorBytes += Utility.bytesToHex(descriptorReader.readBits(descriptorReader.remaining()));

        String transactionBytes = "";
        for (int i = 0; i < 15; i++) {
            transactionBytes += "FF";
        }

        // First chunk; 64 bytes of a header + 16 bytes PoP miner identification
        transactionBytes += "00000767000193093228BD2B4906F6B84BE5E61809C0522626145DDFB988022A0684E2110D384FE2BFD38549CB19C41893C258BA5B9CAB24060BA2D41039DFC857801424B0F5DE63992A016F5F38FEB4";

        transactionBytes += descriptorBytes;

        byte[] embeddedData = BitcoinUtilities.extractPoPData(Utility.hexToBytes(transactionBytes));
        Assert.assertTrue(Utility.byteArraysAreEqual(embeddedData, Utility.hexToBytes("00000767000193093228BD2B4906F6B84BE5E61809C0522626145DDFB988022A0684E2110D384FE2BFD38549CB19C41893C258BA5B9CAB24060BA2D41039DFC857801424B0F5DE63992A016F5F38FEB4")));


        // Random bytes
        transactionBytes += "00FFAC03B1C7";

        // And test with the end padded
        embeddedData = BitcoinUtilities.extractPoPData(Utility.hexToBytes(transactionBytes));
        Assert.assertTrue(Utility.byteArraysAreEqual(embeddedData, Utility.hexToBytes("00000767000193093228BD2B4906F6B84BE5E61809C0522626145DDFB988022A0684E2110D384FE2BFD38549CB19C41893C258BA5B9CAB24060BA2D41039DFC857801424B0F5DE63992A016F5F38FEB4")));
    }

    @Test
    public void embeddedDataUtilityDescriptorOneChunkMagicAtStartBadOffsetButPublicationContiguous() {
        String transactionBytes = "927A59"; // Magic bytes

        String descriptorBits = "0001"; // One total chunk
        descriptorBits += "01"; // Eight bits offset distance
        descriptorBits += "10"; // Six bit descriptor length (irrelevant, as the only chunk length is implied)
        descriptorBits += "00011111"; // First chunk begins 31 bytes from start
        // Implied that first chunk is 80 bytes long

        while (descriptorBits.length() % 8 != 0) {
            descriptorBits += "0";
        }

        BitString descriptor = new BitString(descriptorBits);
        BitStringReader descriptorReader = new BitStringReader(descriptor);

        transactionBytes += Utility.bytesToHex(descriptorReader.readBits(descriptorReader.remaining()));

        int totalTransactionSize = transactionBytes.length() / 2;

        // first chunk is 31 bytes from start, but get transaction to only 30 bytes (one off; should fail)
        for (int i = totalTransactionSize; i < 30; i++) {
            transactionBytes += Utility.bytesToHex(new byte[]{(byte)i});
        }

        // First chunk; 64 bytes of a header + 16 bytes PoP miner identification
        transactionBytes += "00000767000193093228BD2B4906F6B84BE5E61809C0522626145DDFB988022A0684E2110D384FE2BFD38549CB19C41893C258BA5B9CAB24060BA2D41039DFC857801424B0F5DE63992A016F5F38FEB4";

        byte[] embeddedData = BitcoinUtilities.extractPoPData(Utility.hexToBytes(transactionBytes));

        // Still picks up valid PoP publication regardless of malformatted magic data, as PoP publication is continuous
        Assert.assertTrue(Utility.byteArraysAreEqual(embeddedData, Utility.hexToBytes("00000767000193093228BD2B4906F6B84BE5E61809C0522626145DDFB988022A0684E2110D384FE2BFD38549CB19C41893C258BA5B9CAB24060BA2D41039DFC857801424B0F5DE63992A016F5F38FEB4")));

        // Random bytes
        transactionBytes += "00FFAC03B1C7";

        // And test with the end padded
        embeddedData = BitcoinUtilities.extractPoPData(Utility.hexToBytes(transactionBytes));
        Assert.assertTrue(Utility.byteArraysAreEqual(embeddedData, Utility.hexToBytes("00000767000193093228BD2B4906F6B84BE5E61809C0522626145DDFB988022A0684E2110D384FE2BFD38549CB19C41893C258BA5B9CAB24060BA2D41039DFC857801424B0F5DE63992A016F5F38FEB4")));
    }

    @Test
    public void contiguousDataPulledOutEvenWithValidChunkedDataPresent() {
        String transactionBytes = "927A59"; // Magic bytes

        String descriptorBits = "0100"; // Four total chunks
        descriptorBits += "01"; // Eight bits offset distance
        descriptorBits += "10"; // Six bit descriptor length
        descriptorBits += "00100100"; // First chunk begins 36 bytes from start
        descriptorBits += "010100"; // First chunk is 20 bytes long
        descriptorBits += "00100111"; // Second chunk begins 39 bytes after first chunk ends
        descriptorBits += "010100"; // Second chunk is 20 bytes long
        descriptorBits += "00011111"; // Third chunk begins 31 bytes after second chunk ends
        descriptorBits += "010101"; // Third chunk is 21 bytes long
        descriptorBits += "00100011"; // Fourth chunk is 35 bytes after third chunk ends
        // Implied that fourth chunk is 19 bytes long (80 - (20 + 20 + 21))

        while (descriptorBits.length() % 8 != 0) {
            descriptorBits += "0";
        }

        BitString descriptor = new BitString(descriptorBits);
        BitStringReader descriptorReader = new BitStringReader(descriptor);

        transactionBytes += Utility.bytesToHex(descriptorReader.readBits(descriptorReader.remaining()));

        int totalTransactionSize = transactionBytes.length() / 2;

        // first chunk is 36 bytes from start, so get transaction to 36 bytes
        for (int i = totalTransactionSize; i < 36; i++) {
            transactionBytes += Utility.bytesToHex(new byte[]{(byte)i});
        }

        // First chunk; 20 bytes of a header
        transactionBytes += "00000767000193093228BD2B4906F6B84BE5E618";

        // 39 bytes of arbitrary data; second chunk begins 39 bytes after first chunk ends
        for (int i = 0; i <= 38; i++) {
            transactionBytes += Utility.bytesToHex(new byte[]{(byte)(i * 2 /* Arbitrary byte */)});
        }

        // Second chunk; 20 bytes of a header
        transactionBytes += "09C0522626145DDFB988022A0684E2110D384FE2";


        // 31 bytes of arbitrary data; third chunk begins 31 bytes after second chunk ends
        for (int i = 0; i <= 30; i++) {
            transactionBytes += Utility.bytesToHex(new byte[]{(byte)(i * 4 /* Arbitrary byte */)});
        }

        // Third chunk; 21 bytes of a header
        transactionBytes += "BFD38549CB19C41893C258BA5B9CAB24060BA2D410";

        // 35 bytes of arbitrary data; fourth chunk begins 35 bytes after third chunk ends
        for (int i = 0; i <= 34; i++) {
            transactionBytes += Utility.bytesToHex(new byte[]{(byte)(i * 19 /* Arbitrary byte */)});
        }

        // Fourth chunk
        transactionBytes += "39DFC8"; // last 3 bytes of header
        transactionBytes += "57801424B0F5DE63992A016F5F38FEB4"; // PoP miner identification (V6P56zKcXNexRSeWcrtX8vpoJEZJRy)

        // Valid contiguous PoP publication data (this is what should be picked up)
        transactionBytes += "0000042D00011E1E07C7674E0426D1A17E36C0D18EEE597DD7DC18572DD0FE6A925EF8CE4DC8F11BDB07EC256C08D4F9E01E764D5B9C47CD060C88B3FDF4D3DF57801424B0F5DE63992A016F5F38FEB4";


        byte[] embeddedData = BitcoinUtilities.extractPoPData(Utility.hexToBytes(transactionBytes));
        Assert.assertFalse(Utility.byteArraysAreEqual(embeddedData, Utility.hexToBytes("00000767000193093228BD2B4906F6B84BE5E61809C0522626145DDFB988022A0684E2110D384FE2BFD38549CB19C41893C258BA5B9CAB24060BA2D41039DFC857801424B0F5DE63992A016F5F38FEB4")));
        Assert.assertTrue(Utility.byteArraysAreEqual(embeddedData, Utility.hexToBytes("0000042D00011E1E07C7674E0426D1A17E36C0D18EEE597DD7DC18572DD0FE6A925EF8CE4DC8F11BDB07EC256C08D4F9E01E764D5B9C47CD060C88B3FDF4D3DF57801424B0F5DE63992A016F5F38FEB4")));

        // And test with it padded
        for (int i = 0; i <= 38; i++) {
            transactionBytes += Utility.bytesToHex(new byte[]{(byte)(i * 11 /* Arbitrary byte */)});
        }

        embeddedData = BitcoinUtilities.extractPoPData(Utility.hexToBytes(transactionBytes));
        Assert.assertFalse(Utility.byteArraysAreEqual(embeddedData, Utility.hexToBytes("00000767000193093228BD2B4906F6B84BE5E61809C0522626145DDFB988022A0684E2110D384FE2BFD38549CB19C41893C258BA5B9CAB24060BA2D41039DFC857801424B0F5DE63992A016F5F38FEB4")));
        Assert.assertTrue(Utility.byteArraysAreEqual(embeddedData, Utility.hexToBytes("0000042D00011E1E07C7674E0426D1A17E36C0D18EEE597DD7DC18572DD0FE6A925EF8CE4DC8F11BDB07EC256C08D4F9E01E764D5B9C47CD060C88B3FDF4D3DF57801424B0F5DE63992A016F5F38FEB4")));

    }
}
