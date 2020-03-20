// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.p2p;

import java.util.concurrent.atomic.AtomicLong;

public class MessageIdGenerator {
    private static AtomicLong identity = new AtomicLong(0);

    public static String next() {
        return Long.toString(identity.incrementAndGet());
    }
}
