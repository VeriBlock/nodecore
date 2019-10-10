// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

import org.veriblock.shell.LoggingLineAppender

def logRootPath = System.getProperty("logging.path", System.getenv('APM_LOG_PATH')) ?: 'logs/'
def logLevel = System.getProperty("logging.level", System.getenv('APM_LOG_LEVEL')) ?: ''

//appender("STDOUT", ConsoleAppender) {
//    encoder(PatternLayoutEncoder) {
//        pattern = "%d{yyyy-MM-dd HH:mm:ss.SSSXX} %boldWhite(%-16.-16thread) %highlight(%-5level) %gray(%-16.-16logger{0}) - %msg%n"
//    }
//}

appender("TERMINAL", LoggingLineAppender) {
    encoder(PatternLayoutEncoder) {
        pattern = "%d{yyyy-MM-dd HH:mm:ss} %boldWhite(%-10.-10thread) %highlight(%-5level) %gray(%-20.-20logger{0}) - %msg%n"
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

def level = toLevel(logLevel, INFO)
logger("org.veriblock", level)
logger("org.veriblock.shell", ERROR)

//root(ERROR, ["STDOUT"])
root(ERROR, ["TERMINAL", "FILE"])
