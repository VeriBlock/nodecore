// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

import ch.qos.logback.classic.filter.LevelFilter
import ch.qos.logback.classic.filter.ThresholdFilter
import ch.qos.logback.contrib.jackson.JacksonJsonFormatter
import ch.qos.logback.contrib.json.classic.JsonLayout
import ch.qos.logback.core.spi.FilterReply
import org.veriblock.shell.LoggingLineAppender
import org.veriblock.core.utilities.LogLevelColorsConverter

def logRootPath = System.getProperty("logging.path", System.getenv('APM_LOG_PATH')) ?: 'logs'
def logLevel = System.getProperty("logging.level", System.getenv('APM_LOG_LEVEL')) ?: ''
def consoleLogLevel = System.getProperty("logging.level.console", System.getenv('APM_CONSOLE_LOG_LEVEL')) ?: ''
boolean addJsonLogs = System.getenv('APM_ENABLE_JSON_LOG')?.toBoolean() ?: false

statusListener(NopStatusListener)

appender("TERMINAL", LoggingLineAppender) {
    filter(ThresholdFilter) {
        level = toLevel(consoleLogLevel, INFO)
    }
    conversionRule("highlightex", LogLevelColorsConverter)
    encoder(PatternLayoutEncoder) {
        pattern = "%d{yyyy-MM-dd HH:mm:ss} %boldWhite(%-10.-10thread) %highlightex(%-5level) %gray(%-25.-25logger{0}) - %msg%n"
    }
}

appender("FILE", RollingFileAppender) {
    file = "${logRootPath}/apm" + (addJsonLogs ? ".json" : ".log")
    rollingPolicy(SizeAndTimeBasedRollingPolicy) {
        fileNamePattern = "${logRootPath}/apm.%d{yyyy-MM-dd}.%i" + (addJsonLogs ? ".json" : ".log")
        maxHistory = 30
        maxFileSize = "10MB"
        totalSizeCap = "1GB"
    }
    if (addJsonLogs) {
        layout(JsonLayout) {
            jsonFormatter(JacksonJsonFormatter)
            appendLineSeparator = true
        }
    } else {
        encoder(PatternLayoutEncoder) {
            pattern = "%date{YYYY-MM-dd HH:mm:ss.SSSXX} %level [%thread] %logger{10} [%file:%line] %msg%n"
        }
    }
}

appender("FILE-ERROR", FileAppender) {
    file = "${logRootPath}/apm-error" + (addJsonLogs ? ".json" : ".log")
    filter(LevelFilter) {
        level = ERROR
        onMatch = FilterReply.ACCEPT
        onMismatch = FilterReply.DENY
    }
    if (addJsonLogs) {
        layout(JsonLayout) {
            jsonFormatter(JacksonJsonFormatter)
            appendLineSeparator = true
        }
    } else {
        encoder(PatternLayoutEncoder) {
            pattern = "%date{YYYY-MM-dd HH:mm:ss.SSSXX} %level [%thread] %logger{10} [%file:%line] %msg%n"
        }
    }
}

logger("org.veriblock", toLevel(logLevel, DEBUG))

logger("shell-printing", INFO, ["FILE"], false)
logger("Exposed", ERROR)

root(ERROR, ["TERMINAL", "FILE", "FILE-ERROR"])
