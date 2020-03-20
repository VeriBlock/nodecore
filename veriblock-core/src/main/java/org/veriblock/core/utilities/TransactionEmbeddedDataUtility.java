// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.core.utilities;

import org.veriblock.core.types.BitString;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class TransactionEmbeddedDataUtility {
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
     * @param transaction Transaction to attempt to extract split embedded data from
     * @return The embedded split data (or null if the transaction does not contain split data)
     */
    public static byte[] extractEmbeddedSplitData(byte[] transaction) {
        try {
            for (int magicOffset = 0; magicOffset < transaction.length - 2; magicOffset++) {
                if (transaction[magicOffset] == magicBytes[0] &&
                        transaction[magicOffset + 1] == magicBytes[1] &&
                        transaction[magicOffset + 2] == magicBytes[2]) {
                    // Found magic bytes!
                    byte[] bytesContainingDescriptor = new byte[transaction.length - magicOffset - 3];
                    System.arraycopy(transaction, magicOffset + 3, bytesContainingDescriptor, 0, bytesContainingDescriptor.length);
                    BitStringReader reader = new BitStringReader(new BitString(bytesContainingDescriptor));

                    if (reader.remaining() < 8) {
                        // Needs at least one byte to tell us how much additional data in the descriptor must exist.
                        // A magicOffset even further into the transaction would have no chance of having more bytes following it.
                        return null;
                    }

                    byte numChunks = reader.readBits(4)[0];
                    byte offsetBitLength = (byte)((1 + reader.readBits(2)[0]) * 4);
                    byte sectionBitLength = (byte)(4 + reader.readBits(2)[0]);

                    int requiredAdditionalDescriptorBits = (offsetBitLength * numChunks) + (sectionBitLength * (numChunks - 1));
                    if (reader.remaining() < requiredAdditionalDescriptorBits) {
                        // Does not contain enough bits to account for the rest of the described descriptor.
                        // Continue, as it's possible that a future descriptor could describe a smaller length
                        continue;
                    }

                    try {
                        ByteArrayInputStream inputStream = new ByteArrayInputStream(transaction);
                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                        for (int i = 0; i < numChunks; i++) {
                            byte[] offsetBytes = reader.readBits(offsetBitLength);
                            byte sectionLength;

                            if (i != numChunks - 1) {
                                sectionLength = reader.readBits(sectionBitLength)[0];
                            } else {
                                sectionLength = (byte)(80 - outputStream.size()); // Remaining size
                            }

                            byte[] offsetBytesPadded = new byte[4];
                            System.arraycopy(offsetBytes, 0, offsetBytesPadded, offsetBytesPadded.length - offsetBytes.length, offsetBytes.length);
                            int offset = Utility.byteArrayToInt(offsetBytesPadded);

                            byte[] trash = new byte[offset];
                            int readBytes = inputStream.read(trash);
                            if (readBytes != trash.length) {
                                throw new Exception("Did not read the expected number of bytes!");
                            }

                            byte[] section = new byte[sectionLength];
                            readBytes = inputStream.read(section);
                            if (readBytes != section.length) {
                                throw new Exception("Did not read the expected number of bytes!");
                            }
                            outputStream.write(section);
                        }

                        byte[] vBlakeHeader = new byte[64];
                        byte[] extractedData = outputStream.toByteArray();
                        System.arraycopy(extractedData, 0, vBlakeHeader, 0, vBlakeHeader.length);
                        if (BlockUtility.isPlausibleBlockHeader(vBlakeHeader)) {
                            return extractedData;
                        }
                    } catch (Exception e) {
                        // Invalid descriptor, continue in an attempt to find another valid descriptor
                    }
                }
            }
        } catch (Exception e) {
            // Fall through to returning null
        }

        return null;
    }
}
