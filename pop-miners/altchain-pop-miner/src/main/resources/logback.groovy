// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

import ch.qos.logback.classic.filter.LevelFilter
import ch.qos.logback.classic.filter.ThresholdFilter
import ch.qos.logback.core.spi.FilterReply
import org.veriblock.shell.LoggingLineAppender

def logRootPath = System.getProperty("logging.path", System.getenv('APM_LOG_PATH')) ?: 'logs/'
def logLevel = System.getProperty("logging.level", System.getenv('APM_LOG_LEVEL')) ?: ''
def consoleLogLevel = System.getProperty("logging.level.console", System.getenv('APM_CONSOLE_LOG_LEVEL')) ?: ''

appender("TERMINAL", LoggingLineAppender) {
    filter(ThresholdFilter) {
        level = toLevel(consoleLogLevel, INFO)
    }
    encoder(PatternLayoutEncoder) {
        pattern = "%d{yyyy-MM-dd HH:mm:ss} %boldWhite(%-10.-10thread) %highlight(%-5level) %gray(%-25.-25logger{0}) - %msg%n"
    }
}

appender("FILE", RollingFileAppender) {
    file = "${logRootPath}org.veriblock.nodecore-pop.log"
    rollingPolicy(SizeAndTimeBasedRollingPolicy) {
        fileNamePattern = "${logRootPath}org.veriblock.nodecore-pop.%d{yyyy-MM-dd}.%i.log"
        maxHistory = 30
        maxFileSize = "10MB"
        totalSizeCap = "1GB"
    }
    encoder(PatternLayoutEncoder) {
        pattern = "%date{YYYY-MM-dd HH:mm:ss.SSSXX} %level [%thread] %logger{10} [%file:%line] %msg%n"
    }
}

appender("FILE-ERROR", FileAppender) {
    file = "${logRootPath}org.veriblock.nodecore-pop-error.log"
    filter(LevelFilter) {
        level = ERROR
        onMatch = FilterReply.ACCEPT
        onMismatch = FilterReply.DENY
    }

    encoder(PatternLayoutEncoder) {
        pattern = "%date{YYYY-MM-dd HH:mm:ss.SSSXX} %level [%thread] %logger{10} [%file:%line] %msg%n"
    }
}

logger("org.veriblock", toLevel(logLevel, DEBUG))

logger("shell-printing", INFO, ["FILE"], false)
logger("Exposed", ERROR)

root(ERROR, ["TERMINAL", "FILE", "FILE-ERROR"])
