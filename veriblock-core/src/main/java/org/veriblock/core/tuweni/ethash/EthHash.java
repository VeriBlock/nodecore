/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE
 * file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file
 * to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.veriblock.core.tuweni.ethash;

import org.veriblock.core.tuweni.bytes.Bytes;
import org.veriblock.core.tuweni.bytes.Bytes32;
import org.veriblock.core.tuweni.crypto.Hash;
import org.veriblock.core.tuweni.units.bigints.UInt32;

import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Implementation of EthHash utilities for Ethereum mining algorithms.
 */
public class EthHash {

    /**
     * Bytes in word.
     */
    public static int WORD_BYTES = 4;
    /**
     * bytes in dataset at genesis
     */
    public static long DATASET_BYTES_INIT = (long) Math.pow(2, 30);
    /**
     * dataset growth per epoch
     */
    public static long DATASET_BYTES_GROWTH = (long) Math.pow(2, 23);
    /**
     * bytes in cache at genesis
     */
    public static long CACHE_BYTES_INIT = (long) Math.pow(2, 24);

    /**
     * cache growth per epoch
     */
    public static long CACHE_BYTES_GROWTH = (long) Math.pow(2, 17);
    /**
     * Size of the DAG relative to the cache
     */
    public static int CACHE_MULTIPLIER = 1024;
    /**
     * blocks per epoch (~2.777 days with 30-second block time)
     */
    public static int EPOCH_LENGTH = 8000;
    /**
     * Initial EPOCH offset (323=2.584GB added)
     */
    public static int EPOCH_OFFSET = 323;
    /**
     * width of mix
     */
    public static int MIX_BYTES = 128;

    /**
     * hash length in bytes
     */
    public static int HASH_BYTES = 64;
    /**
     * Number of words in a hash
     */
    private static int HASH_WORDS = HASH_BYTES / WORD_BYTES;
    /**
     * number of parents of each dataset element
     */
    public static int DATASET_PARENTS = 256;
    /**
     * number of rounds in cache production
     */
    public static int CACHE_ROUNDS = 3;
    /**
     * number of accesses in hashimoto loop
     */
    public static int ACCESSES = 64;

    public static int FNV_PRIME = 0x01000193;

    /**
     * Calculates the EthHash Epoch for a given block number.
     *
     * @param block Block Number
     * @return EthHash Epoch
     */
    public static long epoch(long block) {
        return (block / EPOCH_LENGTH) + EPOCH_OFFSET;
    }

    /**
     * Provides the size of the cache at a given block number
     *
     * @param block_number the block number
     * @return the size of the cache at the block number, in bytes
     */
    public static int getCacheSize(long block_number) {
        long sz = CACHE_BYTES_INIT + CACHE_BYTES_GROWTH * (epoch(block_number));
        sz -= HASH_BYTES;
        while (!isPrime(sz / HASH_BYTES)) {
            sz -= 2 * HASH_BYTES;
        }
        return (int) sz;
    }

    /**
     * Provides the size of the full dataset at a given block number
     *
     * @param block_number the block number
     * @return the size of the full dataset at the block number, in bytes
     */
    public static long getFullSize(long block_number) {
        long sz = DATASET_BYTES_INIT + DATASET_BYTES_GROWTH * (epoch(block_number));
        sz -= MIX_BYTES;
        while (!isPrime(sz / MIX_BYTES)) {
            sz -= 2 * MIX_BYTES;
        }
        return sz;
    }

    /**
     * Generates the EthHash cache for given parameters.
     *
     * @param cacheSize Size of the cache to generate
     * @param block Block Number to generate cache for
     * @return EthHash Cache
     */
    public static UInt32[] mkCache(int cacheSize, long block) {
        int rows = cacheSize / HASH_BYTES;
        List<Bytes> cache = new ArrayList<>(rows);
        cache.add(Hash.keccak512(dagSeed(block)));

        for (int i = 1; i < rows; ++i) {
            cache.add(Hash.keccak512(cache.get(i - 1)));
        }

        Bytes completeCache = Bytes.concatenate(cache.toArray(new Bytes[cache.size()]));

        byte[] temp = new byte[HASH_BYTES];
        for (int i = 0; i < CACHE_ROUNDS; ++i) {
            for (int j = 0; j < rows; ++j) {
                int offset = j * HASH_BYTES;
                for (int k = 0; k < HASH_BYTES; ++k) {
                    temp[k] = (byte) (completeCache.get((j - 1 + rows) % rows * HASH_BYTES + k)
                        ^ (completeCache.get(
                        Integer.remainderUnsigned(completeCache.getInt(offset, ByteOrder.LITTLE_ENDIAN), rows) * HASH_BYTES
                            + k)));
                }
                temp = Hash.keccak512(temp);
                System.arraycopy(temp, 0, completeCache.toArrayUnsafe(), offset, HASH_BYTES);
            }
        }
        UInt32[] result = new UInt32[completeCache.size() / 4];
        for (int i = 0; i < result.length; i++) {
            result[i] = UInt32.fromBytes(completeCache.slice(i * 4, 4).reverse());
        }

        return result;
    }

    /**
     * Calculate a data set item based on the previous cache for a given index
     *
     * @param cache the DAG cache
     * @param index the current index
     * @return a new DAG item to append to the DAG
     */
    public static Bytes calcDatasetItem(int[] cache, int index) {
        int rows = cache.length / HASH_WORDS;
        int[] mixInts = new int[HASH_BYTES / 4];
        int offset = index % rows * HASH_WORDS;
        mixInts[0] = cache[offset] ^ (index);
        System.arraycopy(cache, offset + 1, mixInts, 1, HASH_WORDS - 1);

        byte[] bytesOfMixInts = new byte[mixInts.length * 4];
        for (int i = 0; i < mixInts.length; i++) {
            bytesOfMixInts[i*4]  = ((byte)(mixInts[i] & 0x000000FF));
            bytesOfMixInts[i*4+1]  = ((byte)((mixInts[i] & 0x0000FF00) >>> 8));
            bytesOfMixInts[i*4+2]  = ((byte)((mixInts[i] & 0x00FF0000) >>> 16));
            bytesOfMixInts[i*4+3]  = ((byte)((mixInts[i] & 0xFF000000) >>> 24));
        }

        Bytes buffer = Bytes.wrap(bytesOfMixInts); // intToByte(mixInts);

        buffer = Hash.keccak512(buffer);
        for (int i = 0; i < mixInts.length; i++) {
            mixInts[i] = (buffer.slice(i * 4, 4).reverse()).toInt();
        }
        for (int i = 0; i < DATASET_PARENTS; ++i) {
            fnvHash(
                mixInts,
                cache,
                (int)(((((long)fnv((index) ^ (i), mixInts[i % 16])) & 0x00000000FFFFFFFFL) % (rows)) * HASH_WORDS));
        }

        bytesOfMixInts = new byte[mixInts.length * 4];
        for (int i = 0; i < mixInts.length; i++) {
            bytesOfMixInts[i*4]  = ((byte)(mixInts[i] & 0x000000FF));
            bytesOfMixInts[i*4+1]  = ((byte)((mixInts[i] & 0x0000FF00) >>> 8));
            bytesOfMixInts[i*4+2]  = ((byte)((mixInts[i] & 0x00FF0000) >>> 16));
            bytesOfMixInts[i*4+3]  = ((byte)((mixInts[i] & 0xFF000000) >>> 24));
        }

        Bytes result = Hash.keccak512(Bytes.wrap(bytesOfMixInts));
        return result;
    }

    public static Bytes dagSeed(long block) {
        Bytes32 seed = Bytes32.wrap(new byte[32]);
        if (Long.compareUnsigned(block + (EPOCH_OFFSET * EPOCH_LENGTH), EPOCH_LENGTH) >= 0) {
            for (int i = 0; i < epoch(block); i++) {
                seed = Hash.keccak256(seed);
            }
        }
        return seed;
    }

    private static int fnv(int v1, int v2) {
        return (v1 * (FNV_PRIME)) ^ (v2);
    }

    private static void fnvHash(int[] mix, int[] cache, int offset) {
        for (int i = 0; i < mix.length; i++) {
            mix[i] = fnv(mix[i], cache[offset + i]);
        }
    }

    private static Bytes intToByte(UInt32[] ints) {
        return Bytes.concatenate(Stream.of(ints).map(i -> i.toBytes().reverse()).toArray(Bytes[]::new));
    }

    private static boolean isPrime(long number) {
        return number > 2 && IntStream.rangeClosed(2, (int) Math.sqrt(number)).noneMatch(n -> (number % n == 0));
    }
}
