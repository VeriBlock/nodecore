// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package veriblock.util;

public class ArrayUtils {
    public static boolean matches(byte[] a, byte[] b) {
        if (a == b) return true;
        if (a == null || b == null)return false;

        int limit = Math.min(a.length, b.length);

        for (int i = 0; i < limit; i++) {
            if (a[a.length - 1 - i] != b[b.length - 1 - i]) {
                return false;
            }
        }

        return true;
    }
}
