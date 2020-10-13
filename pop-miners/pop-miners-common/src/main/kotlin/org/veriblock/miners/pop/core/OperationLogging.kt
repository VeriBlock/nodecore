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
    if (operation.isLoggingEnabled(level)) {
        val loggerMsg = "[${operation.id}] $msg"
        when (level) {
            TRACE -> trace(loggerMsg)
            DEBUG -> debug(loggerMsg)
            INFO -> info(loggerMsg)
            WARN -> warn(loggerMsg)
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
