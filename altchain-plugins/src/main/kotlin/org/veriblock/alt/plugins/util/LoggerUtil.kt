package org.veriblock.alt.plugins.util

import mu.KLogger
import mu.KotlinLogging
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.core.appender.RollingFileAppender
import org.apache.logging.log4j.core.appender.rolling.SizeBasedTriggeringPolicy
import org.apache.logging.log4j.core.config.AppenderRef
import org.apache.logging.log4j.core.config.Configuration
import org.apache.logging.log4j.core.config.LoggerConfig
import org.apache.logging.log4j.core.layout.PatternLayout

class RollingFileBuilder : RollingFileAppender.Builder<RollingFileBuilder>()

fun createLoggerFor(name: String): KLogger {
    return KotlinLogging.logger("altchain-http-calls.$name")
}
