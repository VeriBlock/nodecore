// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.lite.util

import org.veriblock.sdk.createLogger
import java.util.concurrent.locks.Lock

private val logger = createLogger {}

operator fun <T> Lock.invoke(block: () -> T): T {
    //logger.trace { "Acquiring lock $this..." }
    lock()
    //logger.trace { "Lock $this acquired!" }
    try {
        return block()
    } finally {
        unlock()
        //logger.trace { "Lock $this released!" }
    }
}
