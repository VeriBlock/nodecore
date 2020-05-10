// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package veriblock.util;

import java.util.concurrent.atomic.AtomicLong;

public class MessageIdGenerator {
    private static AtomicLong identity = new AtomicLong(0);

    public static String next() {
        return Long.toString(identity.incrementAndGet());
    }
}
