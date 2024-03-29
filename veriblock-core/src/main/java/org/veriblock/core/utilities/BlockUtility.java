// VeriBlock NodeCore
// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.core.utilities;

import org.jetbrains.annotations.NotNull;
import org.veriblock.core.Context;
import org.veriblock.core.SharedConstants;
import org.veriblock.core.bitcoinj.BitcoinUtilities;
import org.veriblock.core.crypto.Crypto;
import org.veriblock.core.tuweni.bytes.Bytes32;
import org.veriblock.core.tuweni.ethash.EthHash;
import org.veriblock.core.tuweni.progpow.ProgPow;
import org.veriblock.core.tuweni.progpow.ProgPowCache;
import org.veriblock.core.types.Pair;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

public final class BlockUtility {
    private BlockUtility(){}

    private static final int[] MASKS = new int[]{0xFF000000, 0x00FF0000, 0x0000FF00, 0x000000FF};

    private static final long[] NONCE_MASKS = new long[]{
        0x000000FF00000000L,
        0x00000000FF000000L,
        0x0000000000FF0000L,
        0x000000000000FF00L,
        0x00000000000000FF};

    private static final int HEADER_SIZE = 65;
    private static final int HEADER_SIZE_VBLAKE = 64;

    private static final int BLOCK_HEIGHT_START_POSITION = 0;
    private static final int BLOCK_HEIGHT_END_POSITION = 3;

    private static final int VERSION_START_POSITION = 4;
    private static final int VERSION_END_POSITION = 5;

    private static final int PREVIOUS_BLOCK_HASH_START_POSITION = 6;
    private static final int PREVIOUS_BLOCK_HASH_END_POSITION = 17;

    private static final int SECOND_PREVIOUS_BLOCK_HASH_START_POSITION = 18;
    private static final int SECOND_PREVIOUS_BLOCK_HASH_END_POSITION = 26;

    private static final int THIRD_PREVIOUS_BLOCK_HASH_START_POSITION = 27;
    private static final int THIRD_PREVIOUS_BLOCK_HASH_END_POSITION = 35;

    private static final int MERKLE_ROOT_HASH_START_POSITION = 36;
    private static final int MERKLE_ROOT_HASH_END_POSITION = 51;

    private static final int TIMESTAMP_START_POSITION = 52;
    private static final int TIMESTAMP_END_POSITION = 55;

    private static final int DIFFICULTY_START_POSITION = 56;
    private static final int DIFFICULTY_END_POSITION = 59;

    private static final int NONCE_START_POSITION = 60;
    private static final int NONCE_END_POSITION = 64;
    private static final int NONCE_END_POSITION_VBLAKE = 63;

    /**
     * Assembles the compact-format block header, which is used for PoW mining and is published during the PoP process
     * to Bitcoin.
     * <p>
     * The block header is 64 bytes which to fit comfortably into an 80-byte OP_RETURN on Bitcoin; remaining 16 bytes
     * will be PoP miner identification data
     *
     * @param blockHeight              4-byte block height
     * @param version                  2-byte block version
     * @param previousBlockHash        12-byte previous block hash encoded as hexadecimal, hash of the previous block which this block builds upon
     * @param secondPreviousBlockHash  9-byte second previous block hash encoded as hexadecimal
     * @param thirdPreviousBlockHash   9-byte third previous block hash encoded as hexadecimal
     * @param merkleRootHash           16-byte merkle root hash encoded as hexadecimal
     * @param timestamp                4-byte timestamp in Unix epoch format (with second granularity)
     * @param difficulty               4-byte difficulty
     * @param nonce                    4/5-byte nonce used to achieve a block hash under target
     * @return The block header created from all provided parameters
     */
    public static byte[] assembleBlockHeader(int blockHeight,
                                             short version,
                                             String previousBlockHash,
                                             String secondPreviousBlockHash,
                                             String thirdPreviousBlockHash,
                                             String merkleRootHash,
                                             int timestamp,
                                             int difficulty,
                                             long nonce) {
        byte[] header;
        if (blockHeight >= Context.get().getNetworkParameters().getProgPowForkHeight()) {
            header = new byte[NONCE_END_POSITION + 1];
        } else {
            header = new byte[NONCE_END_POSITION_VBLAKE + 1];
        }

        int shift = 24;
        for (int i = BLOCK_HEIGHT_START_POSITION; i <= BLOCK_HEIGHT_END_POSITION; i++) {
            header[i] = (byte)((blockHeight & MASKS[i - BLOCK_HEIGHT_START_POSITION]) >> shift);
            shift -= 8;
        }

        shift = 8;
        for (int i = VERSION_START_POSITION; i <= VERSION_END_POSITION; i++) {
            header[i] = (byte)((version & MASKS[i - VERSION_START_POSITION + 2]) >> shift);
            shift -= 8;
        }


        previousBlockHash = previousBlockHash.substring(previousBlockHash.length() - (2 * (1 + PREVIOUS_BLOCK_HASH_END_POSITION - PREVIOUS_BLOCK_HASH_START_POSITION)));

        secondPreviousBlockHash = secondPreviousBlockHash.substring(secondPreviousBlockHash.length() - (2 * (1 + SECOND_PREVIOUS_BLOCK_HASH_END_POSITION - SECOND_PREVIOUS_BLOCK_HASH_START_POSITION)));

        thirdPreviousBlockHash = thirdPreviousBlockHash.substring(thirdPreviousBlockHash.length() - (2 * (1 + THIRD_PREVIOUS_BLOCK_HASH_END_POSITION - THIRD_PREVIOUS_BLOCK_HASH_START_POSITION)));


        for (int i = 0; i <= (PREVIOUS_BLOCK_HASH_END_POSITION - PREVIOUS_BLOCK_HASH_START_POSITION); i++)
            header[i + PREVIOUS_BLOCK_HASH_START_POSITION] = (byte) (Integer.parseInt(previousBlockHash.substring(i * 2, (i * 2) + 2), 16));

        for (int i = 0; i <= (SECOND_PREVIOUS_BLOCK_HASH_END_POSITION - SECOND_PREVIOUS_BLOCK_HASH_START_POSITION); i++)
            header[i + SECOND_PREVIOUS_BLOCK_HASH_START_POSITION] = (byte) (Integer.parseInt(secondPreviousBlockHash.substring(i * 2, (i * 2) + 2), 16));

        for (int i = 0; i <= (THIRD_PREVIOUS_BLOCK_HASH_END_POSITION - THIRD_PREVIOUS_BLOCK_HASH_START_POSITION); i++)
            header[i + THIRD_PREVIOUS_BLOCK_HASH_START_POSITION] = (byte) (Integer.parseInt(thirdPreviousBlockHash.substring(i * 2, (i * 2) + 2), 16));


        for (int i = 0; i <= (MERKLE_ROOT_HASH_END_POSITION - MERKLE_ROOT_HASH_START_POSITION); i++)
            header[i + MERKLE_ROOT_HASH_START_POSITION] = (byte) (Integer.parseInt(merkleRootHash.substring(i * 2, (i * 2) + 2), 16));

        shift = 24;
        for (int i = TIMESTAMP_START_POSITION; i <= TIMESTAMP_END_POSITION; i++) {
            header[i] = (byte)((timestamp & MASKS[i - TIMESTAMP_START_POSITION]) >> shift);
            shift -= 8;
        }

        shift = 24;
        for (int i = DIFFICULTY_START_POSITION; i <= DIFFICULTY_END_POSITION; i++) {
            header[i] = (byte)((difficulty & MASKS[i - DIFFICULTY_START_POSITION]) >> shift);
            shift -= 8;
        }

        if (blockHeight >= Context.get().getNetworkParameters().getProgPowForkHeight()) {
            shift = 32;
            for (int i = NONCE_START_POSITION; i <= NONCE_END_POSITION; i++) {
                header[i] = (byte)((nonce & NONCE_MASKS[i - NONCE_START_POSITION]) >> shift);
                shift -= 8;
            }
        } else {
            shift = 24;
            for (int i = NONCE_START_POSITION; i <= NONCE_END_POSITION_VBLAKE; i++) {
                header[i] = (byte)((nonce & (MASKS[i - NONCE_START_POSITION])) >> shift);
                shift -= 8;
            }
        }

        return header;
    }

    /**
     * Determine whether the provided byte[] is a plausible block header, meaning it:
     * - Is the correct length
     * - vBlake hashes to fit under it's embedded difficulty
     */
    public static boolean isPlausibleBlockHeader(byte[] blockHeader) {
        if (blockHeader.length != NONCE_END_POSITION + 1 && blockHeader.length != NONCE_END_POSITION_VBLAKE + 1) {
            return false;
        }

        int height = extractBlockHeightFromBlockHeader(blockHeader);

        if (height < 0) {
            // Cannot have height below 0!
            return false;
        }

        int progPowForkHeight = Context.get().getNetworkParameters().getProgPowForkHeight();
        if (height >= progPowForkHeight) {
            if (blockHeader.length != NONCE_END_POSITION + 1) {
                return false;
            }
            // 20M block limit for ProgPoW hash evaluation
            if (height <= 20000000) {
                // Check if embedded block height is reasonable considering its timestamp.
                // This avoids checking ProgPoW hash for a block hash which is obviously contextually invalid.
                // TODO: Update with new parameters on fork.

                int blocktimeSeconds = Context.get().getNetworkParameters().getBlockTimeSeconds();
                long startTimeEpoch = Context.get().getNetworkParameters().getProgPowStartTimeEpoch();
                int gracePeriodDays = 5;

                int timestamp = extractTimestampFromBlockHeader(blockHeader);

                if (timestamp < startTimeEpoch) {
                    // Timestamp is before starting timestamp, invalid
                    return false;
                }

                long upperBound = startTimeEpoch + (int)(((double)blocktimeSeconds * (double)(height-progPowForkHeight) * 1.2)) + (86400 * gracePeriodDays);
                long lowerBound = (startTimeEpoch) + (int)(((double)blocktimeSeconds * (double)(height-progPowForkHeight) / 1.2));
                lowerBound -= (86400 * gracePeriodDays);
                if (lowerBound < startTimeEpoch) {
                    lowerBound = startTimeEpoch;
                }

                if (timestamp > upperBound) {
                    // Timestamp is more than upper bound, invalid
                    return false;
                }

                if (timestamp < lowerBound) {
                    // Timestamp is less than upper bound, invalid
                    return false;
                }

                return isMinerHashBelowTarget(blockHeader);
            } else {
                return false;
            }
        } else {
            if (blockHeader.length != NONCE_END_POSITION_VBLAKE + 1) {
                return false;
            }
            return isMinerHashBelowTarget(blockHeader);
        }
    }

    /**
     * Determine whether a block header's PoW hash is below it's embedded difficulty.
     * NOTE: This method only ensures that the block is self-consistent, but not that it is at the appropriate difficulty
     * given the context of the blocks before it.
     * @param blockHeader Block header to calculate the PoW hash of and compare to it's embedded difficulty
     * @return Whether or not this block header's hash meets it's embedded difficulty
     */
    public static boolean isMinerHashBelowTarget(byte[] blockHeader) {
        String blockHash = hashBlock(blockHeader);
        BigInteger difficulty = BitcoinUtilities.decodeCompactBits(extractDifficultyFromBlockHeader(blockHeader));
        if (difficulty.compareTo(BigInteger.ZERO) < 0 || !isEmbeddedDifficultyValid(difficulty) ) {
            return false;
        }

        BigInteger target = SharedConstants.DIFFICULTY_CALCULATOR_MAXIMUM_TARGET.divide(difficulty.multiply(BigInteger.valueOf(1)));
        return new BigInteger(blockHash, 16).compareTo(target) < 0;
    }

    public static boolean isEmbeddedDifficultyValid(BigInteger embeddedDifficulty) {
        if (embeddedDifficulty == null) {
            return false;
        }

        return embeddedDifficulty.compareTo(Context.get().getNetworkParameters().getMinimumDifficulty()) >= 0;
    }

    /**
     * Encodes a BigInteger value in compact bits form
     * @param value BigInteger
     * @return Compacted value as a long
     */
    public static long encodeCompactBits(BigInteger value) {
        long result;
        int size = value.toByteArray().length;
        if (size <= 3)
            result = value.longValue() << 8 * (3 - size);
        else
            result = value.shiftRight(8 * (size - 3)).longValue();
        // The 0x00800000 bit denotes the sign.
        // Thus, if it is already set, divide the mantissa by 256 and increase the exponent.
        if ((result & 0x00800000L) != 0) {
            result >>= 8;
            size++;
        }
        result |= size << 24;
        result |= value.signum() == -1 ? 0x00800000 : 0;
        return result;
    }

    /**
     * Extracts the integer block height from a given block header
     * @param blockHeader Block header to extract the block height from
     * @return Extracted integer block height
     */
    public static int extractBlockHeightFromBlockHeader(byte[] blockHeader) {
        byte[] blockHeightBytes = extractFromBlockHeader(blockHeader, BLOCK_HEIGHT_START_POSITION,
                BLOCK_HEIGHT_END_POSITION - BLOCK_HEIGHT_START_POSITION + 1);

        int blockHeight = 0;
        int bytePointer = 0;
        for (int shift = 24; shift >= 0; shift-=8) {
            blockHeight += (0xFF & (blockHeightBytes[bytePointer])) << shift;
            bytePointer++;
        }

        return blockHeight;
    }

    /**
     * Extracts the short version from a given block header
     * @param blockHeader Block header to extract the version from
     * @return Extracted integer version
     */
    public static short extractVersionFromBlockHeader(byte[] blockHeader) {
        byte[] versionBytes = extractFromBlockHeader(blockHeader, VERSION_START_POSITION,
                VERSION_END_POSITION - VERSION_START_POSITION + 1);

        short version = 0;
        int bytePointer = 0;
        for (int shift = 8; shift >= 0; shift-=8) {
            version += (0xFF & (versionBytes[bytePointer])) << shift;
            bytePointer++;
        }

        return version;
    }

    /**
     * Extracts the bytes of the previous block hash from a given block header
     * @param blockHeader Block header to extract the previous block hash from
     * @return Extracted previous block hash
     */
    public static byte[] extractPreviousBlockHashFromBlockHeader(byte[] blockHeader) {
        return extractFromBlockHeader(blockHeader, PREVIOUS_BLOCK_HASH_START_POSITION,
                PREVIOUS_BLOCK_HASH_END_POSITION - PREVIOUS_BLOCK_HASH_START_POSITION + 1);
    }

    /**
     * Extracts the bytes of the second previous block hash from a given block header
     * @param blockHeader Block header to extract the second previous block hash from
     * @return Extracted second previous block hash
     */
    public static byte[] extractSecondPreviousBlockHashFromBlockHeader(byte[] blockHeader) {
        return extractFromBlockHeader(blockHeader, SECOND_PREVIOUS_BLOCK_HASH_START_POSITION,
                SECOND_PREVIOUS_BLOCK_HASH_END_POSITION - SECOND_PREVIOUS_BLOCK_HASH_START_POSITION + 1);
    }

    /**
     * Extracts the bytes of the third previous block hash from a given block header
     * @param blockHeader Block header to extract the third previous block hash from
     * @return Extracted third previous block hash
     */
    public static byte[] extractThirdPreviousBlockHashFromBlockHeader(byte[] blockHeader) {
        return extractFromBlockHeader(blockHeader, THIRD_PREVIOUS_BLOCK_HASH_START_POSITION,
                THIRD_PREVIOUS_BLOCK_HASH_END_POSITION - THIRD_PREVIOUS_BLOCK_HASH_START_POSITION + 1);
    }

    /**
     * Extracts the bytes of the merkle root from a given block header
     * @param blockHeader Block header to extract the merkle root from
     * @return Extracted merkle root
     */
    public static byte[] extractMerkleRootFromBlockHeader(byte[] blockHeader) {
        return extractFromBlockHeader(blockHeader, MERKLE_ROOT_HASH_START_POSITION,
                MERKLE_ROOT_HASH_END_POSITION - MERKLE_ROOT_HASH_START_POSITION + 1);
    }

    /**
     * Extracts the integer timestamp from a given block header
     * @param blockHeader Block header to extract the timestamp from
     * @return Extracted integer timestamp
     */
    public static int extractTimestampFromBlockHeader(byte[] blockHeader) {
        byte[] timestampBytes = extractFromBlockHeader(blockHeader, TIMESTAMP_START_POSITION,
                TIMESTAMP_END_POSITION - TIMESTAMP_START_POSITION + 1);

        int timestamp = 0;
        int bytePointer = 0;
        for (int shift = 24; shift >= 0; shift-=8) {
            timestamp += (0xFF & (timestampBytes[bytePointer])) << shift;
            bytePointer++;
        }

        return timestamp;
    }

    /**
     * Extracts the integer difficulty from a given block header
     * @param blockHeader Block header to extract the difficulty from
     * @return Extracted integer difficulty
     */
    public static int extractDifficultyFromBlockHeader(byte[] blockHeader) {
        byte[] difficultyBytes = extractFromBlockHeader(blockHeader, DIFFICULTY_START_POSITION,
                DIFFICULTY_END_POSITION - DIFFICULTY_START_POSITION + 1);

        int difficulty = 0;
        int bytePointer = 0;

        for (int shift = 24; shift >= 0; shift-=8) {
            difficulty += (0xFF & (difficultyBytes[bytePointer])) << shift;
            bytePointer++;
        }

        return difficulty;
    }

    /**
     * Extracts the integer nonce from a given block header
     * @param blockHeader Block header to extract the nonce from
     * @return Extracted integer nonce
     */
    public static long extractNonceFromBlockHeader(byte[] blockHeader) {
        int blockHeight = extractBlockHeightFromBlockHeader(blockHeader);
        byte[] nonceBytes;
        int initialShift;
        if (blockHeight >= Context.get().getNetworkParameters().getProgPowForkHeight()) {
            nonceBytes = extractFromBlockHeader(blockHeader, NONCE_START_POSITION,
                NONCE_END_POSITION - NONCE_START_POSITION + 1);
            initialShift = 32;

            long nonce = 0;
            int bytePointer = 0;
            for (int shift = initialShift; shift >= 0; shift-=8) {
                nonce += (0xFFL & (nonceBytes[bytePointer])) << shift;
                bytePointer++;
            }

            return nonce;
        } else {
            nonceBytes = extractFromBlockHeader(blockHeader, NONCE_START_POSITION,
                NONCE_END_POSITION_VBLAKE - NONCE_START_POSITION + 1);
            initialShift = 24;

            int nonce = 0;
            int bytePointer = 0;
            for (int shift = initialShift; shift >= 0; shift-=8) {
                nonce += (0xFF & (nonceBytes[bytePointer])) << shift;
                bytePointer++;
            }

            return nonce;
        }

    }

    private static byte[] extractFromBlockHeader(byte[] blockHeader, int offset, int length) throws IllegalArgumentException {
        if (blockHeader == null) {
            throw new IllegalArgumentException("extractFromBlockHeader cannot be called with a null block header!");
        }

        if (blockHeader.length != HEADER_SIZE && blockHeader.length != HEADER_SIZE_VBLAKE) {
            throw new IllegalArgumentException("extractFromBlockHeader cannot be called with a block header that is "
                    + blockHeader.length + " bytes, must be " + HEADER_SIZE + "! (or " + HEADER_SIZE_VBLAKE + " if VBlake)");
        }

        byte[] extracted = new byte[length];
        System.arraycopy(blockHeader, offset, extracted, 0, length);
        return extracted;
    }

    public static byte[] getProgPowHeaderHash(byte[] header) {
        Crypto crypto = new Crypto();

        byte[] choppedHeaderToHash = extractHeaderBytesForProgPowHeaderHashCalculation(header);
        byte[] headerHash = crypto.SHA256D(choppedHeaderToHash);
        return headerHash;
    }

    public static byte[] extractHeaderBytesForProgPowHeaderHashCalculation(byte[] header) {
        // Chop off the last 5 bytes of the header (nonce)
        byte[] chopped = new byte[header.length - 5];
        System.arraycopy(header, 0, chopped, 0, chopped.length);

        return chopped;
    }

    // The same block header is hashed many times in different operations, cache out expensive hash calculations
    static final int MAX_CACHE_SIZE = 100000 + new Random().nextInt(100000);
    static final LinkedHashMap<String, String> hashCache = new LinkedHashMap<String, String>() {
        @Override
        protected boolean removeEldestEntry(final Map.Entry eldest) {
            return size() > MAX_CACHE_SIZE;
        }
    };

    public static void cacheExternalHash(String header, String hash) {
        hashCache.put(header, hash);
    }

    public static String hashBlock(byte[] blockHeader) {
        int blockNum = BlockUtility.extractBlockHeightFromBlockHeader(blockHeader);
        int progPowForkHeight = Context.get().getNetworkParameters().getProgPowForkHeight();
        if (blockNum < progPowForkHeight) {
            return hashVBlakeBlock(blockHeader);
        } else {
            return hashProgPowBlock(blockHeader, blockNum);
        }
    }

    @NotNull
    public static String hashVBlakeBlock(byte[] blockHeader) {
        Crypto crypto = new Crypto();
        String blockHash = crypto.vBlakeReturnHex(blockHeader);
        return blockHash.substring(0, SharedConstants.VBLAKE_HASH_OUTPUT_SIZE_BYTES * 2); // *2 to account for Hex
    }

    @NotNull
    public static String hashProgPowBlock(byte[] blockHeader, int blockNum) {
        String blockHeaderHex = Utility.bytesToHex(blockHeader);
        if (hashCache.containsKey(blockHeaderHex)) {
            return hashCache.get(blockHeaderHex);
        }

        // Generate header hash...
        byte[] headerHash = getProgPowHeaderHash(blockHeader);
        long extractedNonce = BlockUtility.extractNonceFromBlockHeader(blockHeader);

        // Nonce in VeriBlock is only 40 bits (5 bytes)
        long converted = (extractedNonce & 0x0000_00FF_FFFF_FFFFL);

        // TODO: Move to crypto
        Pair<int[], int[]> cachePair = ProgPowCache.getDAGCache(blockNum);
        int[] cache = cachePair.getFirst();

        int[] cDag = cachePair.getSecond();
        Bytes32 digest = ProgPow.progPowHash(
            blockNum,
            converted,
            Bytes32.wrap(headerHash),
            cDag,
            (ind) -> EthHash.calcDatasetItem(cache, ind)
        );

        String blockHash = digest.toUnprefixedHexString().toUpperCase();
        String hash = blockHash.substring(0, SharedConstants.VBLAKE_HASH_OUTPUT_SIZE_BYTES * 2); // *2 to account for Hex
        hashCache.put(blockHeaderHex, hash);
        return hash;
    }

    public static boolean isProgPow(int blockHeight) {
        int progPowForkHeight = Context.get().getNetworkParameters().getProgPowForkHeight();
        return blockHeight >= progPowForkHeight;
    }

    public static int getBlockHeaderLength(int blockHeight) {
        if (isProgPow(blockHeight)) {
            return HEADER_SIZE;
        } else {
            return HEADER_SIZE_VBLAKE;
        }
    }

    public static byte[] getBlockHeader(ByteBuffer buffer) {
        byte[] blockHeader = new byte[HEADER_SIZE_VBLAKE];
        buffer.get(blockHeader);
        int height = extractBlockHeightFromBlockHeader(blockHeader);
        if (isProgPow(height)) {
            buffer.position(buffer.position() - blockHeader.length);
            blockHeader = new byte[HEADER_SIZE];
            buffer.get(blockHeader);
        }
        return blockHeader;
    }
}
