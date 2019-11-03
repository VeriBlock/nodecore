import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import org.veriblock.shell.LoggingLineAppender

import static ch.qos.logback.classic.Level.INFO

def logRootPath = System.getenv('VPM_LOG_PATH') ?: 'logs/'
def logLevel = System.getenv('VPM_LOG_LEVEL') ?: ''

//appender("STDOUT", ConsoleAppender) {
//    encoder(PatternLayoutEncoder) {
//        pattern = "%date{YYYY-MM-dd HH:mm:ss.SSSXX} [%thread] %-5level %logger{36} - %msg%n"
//    }
//}
appender("TERMINAL", LoggingLineAppender) {
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

def level = toLevel(logLevel, INFO)

logger("nodecore.miners.pop", level, ["TERMINAL"], false)
logger("org.bitcoinj", INFO, ["BITCOINJ-FILE"], false)

root(DEBUG, ["FILE"])
