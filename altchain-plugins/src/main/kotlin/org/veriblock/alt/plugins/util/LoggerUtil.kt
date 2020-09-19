package org.veriblock.alt.plugins.util

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.rolling.RollingFileAppender
import ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy
import ch.qos.logback.core.util.FileSize
import mu.KLogger
import mu.KotlinLogging
import org.slf4j.LoggerFactory

fun createLoggerFor(loggerFolder: String, name: String): KLogger {
    val loggerContext = LoggerFactory.getILoggerFactory() as LoggerContext
    val fileAppender = RollingFileAppender<ILoggingEvent>().apply {
        context = loggerContext
        file = "$loggerFolder/$name.log"
        rollingPolicy = SizeAndTimeBasedRollingPolicy<ILoggingEvent>().also {
            it.context = loggerContext
            it.fileNamePattern = "$loggerFolder/$name.%d{yyyy-MM-dd}.%i.log"
            it.maxHistory = 30
            it.setMaxFileSize(FileSize.valueOf("10MB"))
            it.setTotalSizeCap(FileSize.valueOf("1GB"))
            it.setParent(this@apply)
            it.start()
        }
        encoder = PatternLayoutEncoder().apply {
            context = loggerContext
            pattern = "%date{YYYY-MM-dd HH:mm:ss.SSSXX} %level [%thread] %msg%n"
            start()
        }
        start()
    }
    val logger = KotlinLogging.logger(name)
    (logger.underlyingLogger as Logger).apply {
        detachAndStopAllAppenders()
        addAppender(fileAppender)
        level = Level.DEBUG
        isAdditive = false
    }
    return logger
}
