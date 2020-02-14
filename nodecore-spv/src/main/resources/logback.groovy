import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.filter.LevelFilter
import ch.qos.logback.core.spi.FilterReply

def logRootPath = System.getProperty("logging.path", System.getenv('NODECORE_LOG_PATH')) ?: 'logs/'
def logLevel = System.getProperty("logging.level", System.getenv('NODECORE_LOG_LEVEL')) ?: ''

appender("STDOUT", ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        //pattern = "%date{YYYY-MM-dd HH:mm:ss.SSSXX} [%thread] %level %logger{36} - %msg%n"
        pattern = "%d{yyyy-MM-dd HH:mm:ss.SSSXX} %boldWhite(%-16.-16thread) %highlight(%-5level) %gray(%-16.-16logger{0}) - %msg%n"
    }
}
appender("FILE", RollingFileAppender) {
    file = "${logRootPath}veriblock.nodecore.log"

    rollingPolicy(SizeAndTimeBasedRollingPolicy) {
        fileNamePattern = "${logRootPath}archive/veriblock.nodecore-%d{yyyy-MM-dd}.%i.log"
        maxHistory = 30
        maxFileSize = "10MB"
        totalSizeCap = "1GB"
    }
    encoder(PatternLayoutEncoder) {
        pattern = "%date{YYYY-MM-dd HH:mm:ss.SSSXX} %level [%thread] %logger{10} [%file:%line] %msg%n"
    }
}

appender("FILE-ERROR", FileAppender) {
    file = "${logRootPath}veriblock.nodecore-error.log"
    filter(LevelFilter) {
        level = ERROR
        onMatch = FilterReply.ACCEPT
        onMismatch = FilterReply.DENY
    }

    encoder(PatternLayoutEncoder) {
        pattern = "%date{YYYY-MM-dd HH:mm:ss.SSSXX} %level [%thread] %logger{10} [%file:%line] %msg%n"
    }
}

appender("FILE-STATUS", RollingFileAppender) {
    file = "${logRootPath}veriblock.nodecore-status.log"

    rollingPolicy(TimeBasedRollingPolicy) {
        fileNamePattern = "${logRootPath}archive/veriblock.nodecore-status-%d{yyyy-MM-dd}.log"
        maxHistory = 5
        totalSizeCap = "50MB"
    }
    encoder(PatternLayoutEncoder) {
        pattern = "%date{YYYY-MM-dd HH:mm:ss.SSSXX} %msg%n"
    }
}

appender("FILE-THREADS", RollingFileAppender) {
    file = "${logRootPath}veriblock.nodecore-threads.log"

    rollingPolicy(TimeBasedRollingPolicy) {
        fileNamePattern = "${logRootPath}archive/veriblock.nodecore-threads-%d{yyyy-MM-dd}.log"
        maxHistory = 1
        totalSizeCap = "50MB"
    }
    encoder(PatternLayoutEncoder) {
        pattern = "%date{YYYY-MM-dd HH:mm:ss.SSSXX} %msg%n"
    }
}

appender("FILE-UCP", RollingFileAppender) {
    file = "${logRootPath}veriblock.nodecore-ucp.log"

    rollingPolicy(TimeBasedRollingPolicy) {
        fileNamePattern = "${logRootPath}archive/veriblock.nodecore-ucp-%d{yyyy-MM-dd}.log"
        maxHistory = 1
        totalSizeCap = "50MB"
    }
    encoder(PatternLayoutEncoder) {
        pattern = "%date{YYYY-MM-dd HH:mm:ss.SSSXX} %level [%thread] %logger{10} [%file:%line] %msg%n"
    }
}

def level = toLevel(logLevel, INFO)

logger("status-log", DEBUG, ["FILE-STATUS"])
logger("ucp-log", DEBUG, ["FILE-UCP"])
logger("status-threads", DEBUG, ["FILE-THREADS"], false)
logger("com.j256.ormlite.table.TableUtils", WARN)
root(level, ["STDOUT", "FILE", "FILE-ERROR"])
