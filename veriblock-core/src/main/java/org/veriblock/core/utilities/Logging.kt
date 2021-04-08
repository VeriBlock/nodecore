// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.core.utilities

import mu.KLogger
import mu.KotlinLogging

fun createLogger(context: () -> Unit) = KotlinLogging.logger(context)

inline fun KLogger.debugInfo(t: Throwable, crossinline msg: () -> String) {
    info { "${msg()}: ${t.niceMessage()}" }
    debug(t) { "Stack Trace:" }
}

inline fun KLogger.debugWarn(t: Throwable, crossinline msg: () -> String) {
    warn { "${msg()}: ${t.niceMessage()}" }
    debug(t) { "Stack Trace:" }
}

inline fun KLogger.debugError(t: Throwable, crossinline msg: () -> String) {
    error { "${msg()}: ${t.niceMessage()}" }
    debug(t) { "Stack Trace:" }
}

fun Throwable.niceMessage(recursionLevel: Int = 0): String {
    val baseMessage = (message ?: toString())
    val cause = cause
    return if (cause != null && recursionLevel < 5) {
        baseMessage + "\nCaused by: ${cause.niceMessage(recursionLevel + 1)}"
    } else {
        baseMessage
    }
}


