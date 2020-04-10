import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.filter.ThresholdFilter
import org.veriblock.shell.LoggingLineAppender

import static ch.qos.logback.classic.Level.INFO

def logRootPath = System.getenv('VPM_LOG_PATH') ?: 'logs/'
def logLevel = System.getenv('VPM_LOG_LEVEL') ?: ''
def consoleLogLevel = System.getenv('VPM_CONSOLE_LOG_LEVEL') ?: ''

appender("TERMINAL", LoggingLineAppender) {
    filter(ThresholdFilter) {
        level = toLevel(consoleLogLevel, INFO)
    }
    encoder(PatternLayoutEncoder) {
        pattern = "%d{yyyy-MM-dd HH:mm:ss} %boldWhite(%-10.-10thread) %highlight(%-5level) %gray(%-25.-25logger{0}) - %msg%n"
    }
}
appender("FILE", RollingFileAppender) {
    file = "${logRootPath}veriblock.nodecore-pop.log"
    rollingPolicy(SizeAndTimeBasedRollingPolicy) {
        fileNamePattern = "${logRootPath}veriblock.nodecore-pop.%d{yyyy-MM-dd}.%i.log"
        maxHistory = 30
        maxFileSize = "10MB"
        totalSizeCap = "1GB"
    }
    encoder(PatternLayoutEncoder) {
        pattern = "%date{YYYY-MM-dd HH:mm:ss.SSSXX} %level [%thread] %logger{10} [%file:%line] %msg%n"
    }
}

appender("ERROR-FILE", FileAppender) {
    file = "${logRootPath}veriblock.nodecore-pop-error.log"
    filter(ThresholdFilter) {
        level = ERROR
    }
    encoder(PatternLayoutEncoder) {
        pattern = "%date{YYYY-MM-dd HH:mm:ss.SSSXX} %level [%thread] %logger{10} [%file:%line] %msg%n"
    }
}

appender("BITCOINJ-FILE", RollingFileAppender) {
    file = "${logRootPath}bitcoinj.nodecore-pop.log"
    rollingPolicy(SizeAndTimeBasedRollingPolicy) {
        fileNamePattern = "${logRootPath}bitcoinj.nodecore-pop.%d{yyyy-MM-dd}.%i.log"
        maxHistory = 10
        maxFileSize = "10MB"
        totalSizeCap = "100MB"
    }
    encoder(PatternLayoutEncoder) {
        pattern = "%date{YYYY-MM-dd HH:mm:ss.SSSXX} %level [%thread] %logger{10} [%file:%line] %msg%n"
    }
}

logger("org.veriblock.miners.pop", toLevel(logLevel, DEBUG))

logger("org.bitcoinj", INFO, ["BITCOINJ-FILE"], false)
logger("shell-printing", INFO, ["FILE"], false)
logger("Exposed", ERROR)

root(ERROR, ["TERMINAL", "FILE", "ERROR-FILE"])
