package org.veriblock.miners.pop.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import mu.KLogger
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.Level.DEBUG
import org.apache.logging.log4j.Level.ERROR
import org.apache.logging.log4j.Level.INFO
import org.apache.logging.log4j.Level.TRACE
import org.apache.logging.log4j.Level.WARN
import java.io.PrintWriter
import java.io.StringWriter
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@Serializable
class OperationLog(
    val timestamp: Long,
    val level: String,
    val msg: String
) {
    override fun toString(): String {
        val time = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault())
        return "$time $level - $msg"
    }
}

private val jsonSerialization = Json {}
private val logSerializer = ListSerializer(OperationLog.serializer())

fun List<OperationLog>.toJson() = jsonSerialization.encodeToString(logSerializer, this)
fun String.parseOperationLogs() = jsonSerialization.decodeFromString(logSerializer, this)

private fun KLogger.log(
    operation: MiningOperation,
    level: Level,
    msg: String,
    t: Throwable? = null
) {
    val log = OperationLog(System.currentTimeMillis(), level.name(), msg)
    operation.addLog(log)
    if (t != null) {
        val errorLog = OperationLog(
            System.currentTimeMillis(),
            level.name(),
            StringWriter().also {
                t.printStackTrace(PrintWriter(it))
            }.toString()
        )
        operation.addLog(errorLog)
    }
    if (operation.isLoggingEnabled(level)) {
        val loggerMsg = "[${operation.id}] $msg"
        when (level) {
            TRACE -> if (t != null) trace(loggerMsg, t) else trace(loggerMsg)
            DEBUG -> if (t != null) debug(loggerMsg, t) else debug(loggerMsg)
            INFO -> if (t != null) info(loggerMsg, t) else info(loggerMsg)
            WARN -> if (t != null) warn(loggerMsg, t) else warn(loggerMsg)
            ERROR -> if (t != null) error(loggerMsg, t) else error(loggerMsg)
        }
    }
}

// Utility functions for logging on an operation
fun KLogger.trace(
    operation: MiningOperation, msg: String
) = log(operation, TRACE, msg)

fun KLogger.debug(
    operation: MiningOperation, msg: String
) = log(operation, DEBUG, msg)

fun KLogger.debug(
    operation: MiningOperation, t: Throwable, msg: String
) = log(operation, DEBUG, msg, t)

fun KLogger.info(
    operation: MiningOperation, msg: String
) = log(operation, INFO, msg)

fun KLogger.warn(
    operation: MiningOperation, msg: String
) = log(operation, WARN, msg)

fun KLogger.warn(
    operation: MiningOperation, t: Throwable, msg: String
) = log(operation, WARN, msg, t)

fun KLogger.error(
    operation: MiningOperation, msg: String
) = log(operation, ERROR, msg)

fun KLogger.error(
    operation: MiningOperation, t: Throwable, msg: String
) = log(operation, ERROR, msg, t)

fun KLogger.debugInfo(operation: MiningOperation, t: Throwable, msg: String) {
    info(operation, "$msg: ${t.message}")
    debug(operation, t, "Stack Trace:")
}

fun KLogger.debugWarn(operation: MiningOperation, t: Throwable, msg: String) {
    warn(operation, "$msg: ${t.message}")
    debug(operation, t, "Stack Trace:")
}

fun KLogger.debugError(operation: MiningOperation, t: Throwable, msg: String) {
    error(operation, "$msg: ${t.message}")
    debug(operation, t, "Stack Trace:")
}
