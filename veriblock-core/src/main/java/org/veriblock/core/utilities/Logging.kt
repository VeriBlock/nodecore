// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.core.utilities

import mu.KLogger
import mu.KotlinLogging

fun createLogger(context: () -> Unit) = KotlinLogging.logger(context)

fun KLogger.debugInfo(t: Throwable, msg: () -> String) = if (isDebugEnabled) {
    info(t, msg)
} else {
    info { "${msg()}: ${t.message}" }
}

fun KLogger.debugWarn(t: Throwable, msg: () -> String) = if (isDebugEnabled) {
    warn(t, msg)
} else {
    warn { "${msg()}: ${t.message}" }
}

fun KLogger.debugError(t: Throwable, msg: () -> String) = if (isDebugEnabled) {
    error(t, msg)
} else {
    error { "${msg()}: ${t.message}" }
}
