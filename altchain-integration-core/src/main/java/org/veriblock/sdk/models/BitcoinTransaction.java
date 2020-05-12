// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.sdk.models;

import org.veriblock.sdk.util.Preconditions;
import org.veriblock.sdk.util.Utils;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.BitSet;

public class BitcoinTransaction {
    private final byte[] raw;

    public BitcoinTransaction(byte[] raw) {
        Preconditions.notNull(raw, "Raw bitcoin transaction cannot be null");
        Preconditions.argument(raw.length > 0, "Raw bitcoin transaction cannot be empty");

        this.raw = raw;
    }

    public byte[] getRawBytes() {
        return raw;
    }


    public boolean contains(byte[] value) {
        // Search for the whole thing
        for(int i = 0; i < raw.length - value.length + 1; ++i) {
            boolean found = true;
            for(int j = 0; j < value.length; ++j) {
                if (raw[i+j] != value[j]) {
                    found = false;
                    break;
                }
            }
            if (found) return true;
        }

        // Search the parts
        return containsSplit(value);
    }

    // As of 12/2/2018, these three bytes in order represented the least-common 3-byte value in the Bitcoin blockchain,
    // Only occurring 6273 times in roughly 552,000 blocks (minimizes the "false-positive" rate)
    private static final byte[] magicBytes = new byte[]{(byte)0x92, (byte)0x7A, (byte)0x59};

    /**
     * Extracts (if it exists) a valid endorsement embedded in a transaction in a "split" fashion.
     *
     * A transaction containing split data must have a "embedded descriptor" which describes exactly how the data
     * is embedded in the transaction. This descriptor gives parsing instructions for extracting the data, and can
     * exist anywhere in the transaction (before any of the embedded chunks, between embedded chunks, after all
     * embedded chunks). Technically it could also exist within a chunk or partially within a chunk as well (so a user
     * could theoretically brute-force VBK reward addresses to have their partial representation end with some of the
     * starting bytes of the descriptor, such as the magic bytes).
     *
     * The format of the descriptor is as follows:
     * Magic Bytes: 0x927A59
     * 4 bits: number of chunks "num_chunks" (max of 15)
     * 2 bits: bit-length of each offset "offset_bit_length" (0x00 = 4 bits, 0x01 = 8 bits, 0x10 = 12 bits, 0x11 = 16 bits)
     * 2 bits: bit-length of each section "section_bit_length" (0x00 = 4 bits, 0x01 = 5 bits, 0x10 = 6 bits, 0x11 = 7 bits)
     * (num_chunks * 2 * offset_bit_length + (num_chunks - 1) * 2 * section_bit_length) bits: offset:length pairs (last length implied)
     *
     * For example:
     *
     * 0100     // Number of chunks (4)
     * 01       // Each offset distance is 8 bits
     * 10       // Each section length is 6 bits
     * 00101001 // First chunk starts 41 bytes from beginning of transaction
     * 010100   // First chunk is 20 bytes long
     * 01000111 // Second chunk starts 71 bytes from end of first chunk
     * 010101   // Second chunk is 21 bytes long
     * 01000100 // Third chunk is 68 bytes from end of second chunk
     * 010101   // Third chunk is 21 bytes long
     * 01010011 // Fourth chunk is 83 bytes from end of third chunk
     * // NOTE: No need to specify length of last section, as it is assumed to be the remaining length (80 - (20 + 21 + 21)) = 18
     *
     *
     * The above bit string serializes to:
     * 01000110 00101001 01010001 00011101 01010100 01000101 01010100 11000000
     * (an extra 8 bytes + 3 bytes for the magic number, or 16% overhead, unless the VBK payout address is brute-forced
     * to already embed some of the bytes, which could reduce overhead below 10%).
     *
     * @param value Embedded data to lookup
     * @return true if the raw transaction contains the supplied value, false otherwise
     */
    public boolean containsSplit(byte[] value) {
        ByteBuffer buffer = ByteBuffer.wrap(this.raw);

        final int LENGTH = value.length;

        try {
            int lastPos = 0;
            // Need at least 6 bytes to be a descriptor
            while (buffer.remaining() > 5) {
                if (!(buffer.get() == magicBytes[0])) continue;
                if (!(buffer.get() == magicBytes[1])) continue;
                if (!(buffer.get() == magicBytes[2])) continue;

                lastPos = buffer.position();

                // Parse the first byte to get the number of chunks, their positions and lengths
                byte[] descriptorBytes = new byte[1];
                buffer.get(descriptorBytes);
                BitSet descriptor = BitSet.valueOf(descriptorBytes);

                int chunks = 0;
                int offsetLength = 4;
                int sectionLength = 4;

                for (int i = 0; i < 8; i++) {
                    if (descriptor.get(i)) {
                        if (i < 2) {
                            sectionLength += 1 << i;
                        } else if (i < 4) {
                            offsetLength += 1 << i;
                        } else {
                            chunks += 1 << (i -4);
                        }
                    }
                }

                // Parse the actual chunk descriptors now that we know the sizes
                int chunkDescriptorBitLength = (chunks * offsetLength) + (sectionLength * (chunks - 1));
                int chunkDescriptorBytesLength = (chunkDescriptorBitLength + 8 - (chunkDescriptorBitLength % 8)) / 8;
                int waste = chunkDescriptorBytesLength * 8 - chunkDescriptorBitLength;

                byte[] chunkDescriptorBytes = new byte[chunkDescriptorBytesLength];
                buffer.get(chunkDescriptorBytes);
                BitSet chunkDescriptor = BitSet.valueOf(Utils.reverseBytes(chunkDescriptorBytes));

                int totalBytesRead = 0;
                byte[] extracted = new byte[LENGTH];
                buffer.position(0);
                for (int i = chunks - 1; i >= 0; i--) {
                    int chunkOffset = waste + (i * (offsetLength + sectionLength));
                    int sectionOffsetValue = Utils.toInt(chunkDescriptor.get(chunkOffset, Math.min(chunkDescriptor.length(), chunkOffset + offsetLength)));

                    int sectionLengthValue;
                    if (i == 0) {
                        sectionLengthValue = LENGTH - totalBytesRead;
                    } else {
                        sectionLengthValue = Utils.toInt(chunkDescriptor.get(chunkOffset - sectionLength, chunkOffset));
                    }
                    buffer.position(buffer.position() + sectionOffsetValue);
                    buffer.get(extracted, totalBytesRead, sectionLengthValue);
                    totalBytesRead += sectionLengthValue;
                }

                if (Arrays.equals(value, extracted)) {
                    return true;
                }

                buffer.position(lastPos);
            }
        } catch (Exception e) {
            // Fall through to returning null
        }

        return false;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o != null && getClass() == o.getClass() &&
                Arrays.equals(getRawBytes(), ((BitcoinTransaction) o).getRawBytes());
    }
}
