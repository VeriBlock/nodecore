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

package org.veriblock.core.tuweni.progpow;

final class ProgPowMath {
    static int math(int a, int b, int r) {
        int rMod = (int)((((long)r) & 0x00000000FFFFFFFFL) % 11);
        switch (rMod) {
            case 0:
                return rotl32(a, b);
            case 1:
                return a & b;
            case 2:
                return a + b;
            case 3:
                return popcount(a) + (popcount(b));
            case 4:
                return clz(a) + (clz(b));
            case 5:
                return rotr32(a, b);
            case 6:
                return mul_hi(a, b);
            case 7:
                return a | b;
            case 8:
                return a * b;
            case 9:
                return a ^ b;
            case 10:
                return (((long)a) & 0x00000000FFFFFFFFL) > (((long)b) & 0x00000000FFFFFFFFL) ? b : a;
            default:
                throw new IllegalArgumentException(
                    "Value " + r + " has mod larger than 11 " + rMod);
        }
    }

    private static int mul_hi(int x, int y) {
        long multiplied = (((long)x) & 0x00000000FFFFFFFFL) * (((long)y) & 0x00000000FFFFFFFFL);
        long high = (multiplied & 0xFFFFFFFF00000000L);
        return (int)((high >>> 32));
    }

    private static int clz(int value) {
        return Integer.numberOfLeadingZeros(value);
    }

    private static int popcount(int value) {
        return Integer.bitCount(value);
    }

    static int rotl32(int var, int hops) {
        int hopsMod32 = (int)((((long)hops) & 0x00000000FFFFFFFFL) % 32);
        return ((var << (hopsMod32))) | (var >>> (32 - hopsMod32));
    }

    static int rotr32(int var, int hops) {
        int hopsMod32 = (int)((((long)hops) & 0x00000000FFFFFFFFL) % 32);
        return ((var >>> (hopsMod32))) | (var << (32 - hopsMod32));
    }
}

