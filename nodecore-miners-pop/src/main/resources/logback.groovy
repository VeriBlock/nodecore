import ch.qos.logback.classic.encoder.PatternLayoutEncoder

import static ch.qos.logback.classic.Level.INFO

def logRootPath = System.getenv('NODECORE_LOG_PATH') ?: ''

appender("STDOUT", ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        pattern = "%date{YYYY-MM-dd HH:mm:ss.SSSXX} [%thread] %-5level %logger{36} - %msg%n"
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

logger("nodecore.miners.pop", INFO)
logger("org.bitcoinj", INFO, ["BITCOINJ-FILE"], false)

root(INFO, ["FILE"])