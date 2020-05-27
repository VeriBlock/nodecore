// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.core.utilities

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.pattern.color.ANSIConstants
import ch.qos.logback.core.pattern.color.ForegroundCompositeConverterBase
import mu.KLogger
import mu.KotlinLogging

fun createLogger(context: () -> Unit) = KotlinLogging.logger(context)

inline fun KLogger.debugInfo(t: Throwable, crossinline msg: () -> String) {
    info { "${msg()}: ${t.message}" }
    debug(t) { "Stack Trace:" }
}

inline fun KLogger.debugWarn(t: Throwable, crossinline msg: () -> String) {
    warn { "${msg()}: ${t.message}" }
    debug(t) { "Stack Trace:" }
}

inline fun KLogger.debugError(t: Throwable, crossinline msg: () -> String) {
    error { "${msg()}: ${t.message}" }
    debug(t) { "Stack Trace:" }
}

/**
 * Utility composite converter for clearer log level colors.
 * Imported in most leaf projects' logback.groovy files.
 */
class LogLevelColorsConverter : ForegroundCompositeConverterBase<ILoggingEvent>() {
    override fun getForegroundColorCode(event: ILoggingEvent) = when (event.level.toInt()) {
        Level.ERROR_INT -> ANSIConstants.BOLD + ANSIConstants.RED_FG
        Level.WARN_INT -> ANSIConstants.YELLOW_FG
        Level.INFO_INT -> ANSIConstants.BLUE_FG
        else -> ANSIConstants.DEFAULT_FG
    }
}
