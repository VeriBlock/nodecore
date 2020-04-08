package org.veriblock.miners.pop.core

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Level.DEBUG
import ch.qos.logback.classic.Level.ERROR
import ch.qos.logback.classic.Level.INFO
import ch.qos.logback.classic.Level.TRACE
import ch.qos.logback.classic.Level.WARN
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import mu.KLogger
import org.veriblock.core.contracts.MiningInstruction
import org.veriblock.core.contracts.WithDetailedInfo
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

private val jsonSerialization = Json(JsonConfiguration.Stable)
private val logSerializer = ListSerializer(OperationLog.serializer())

fun List<OperationLog>.toJson() = jsonSerialization.stringify(logSerializer, this)
fun String.parseOperationLogs() = jsonSerialization.parse(logSerializer, this)

private fun <MI : MiningInstruction, SPT : SpTransaction, SPB : SpBlock, MP : MerklePath, CD : WithDetailedInfo> KLogger.log(
    operation: MiningOperation<MI, SPT, SPB, MP, CD>,
    level: Level,
    msg: String,
    t: Throwable? = null
) {
    val log = OperationLog(System.currentTimeMillis(), level.levelStr, msg)
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
fun <MI : MiningInstruction, SPT : SpTransaction, SPB : SpBlock, MP : MerklePath, CD : WithDetailedInfo> KLogger.trace(
    operation: MiningOperation<MI, SPT, SPB, MP, CD>, msg: String
) = log(operation, TRACE, msg)

fun <MI : MiningInstruction, SPT : SpTransaction, SPB : SpBlock, MP : MerklePath, CD : WithDetailedInfo> KLogger.debug(
    operation: MiningOperation<MI, SPT, SPB, MP, CD>, msg: String
) = log(operation, DEBUG, msg)

fun <MI : MiningInstruction, SPT : SpTransaction, SPB : SpBlock, MP : MerklePath, CD : WithDetailedInfo> KLogger.info(
    operation: MiningOperation<MI, SPT, SPB, MP, CD>, msg: String
) = log(operation, INFO, msg)

fun <MI : MiningInstruction, SPT : SpTransaction, SPB : SpBlock, MP : MerklePath, CD : WithDetailedInfo> KLogger.warn(
    operation: MiningOperation<MI, SPT, SPB, MP, CD>, msg: String
) = log(operation, WARN, msg)

fun <MI : MiningInstruction, SPT : SpTransaction, SPB : SpBlock, MP : MerklePath, CD : WithDetailedInfo> KLogger.error(
    operation: MiningOperation<MI, SPT, SPB, MP, CD>, msg: String
) = log(operation, ERROR, msg)

fun <MI : MiningInstruction, SPT : SpTransaction, SPB : SpBlock, MP : MerklePath, CD : WithDetailedInfo> KLogger.error(
    operation: MiningOperation<MI, SPT, SPB, MP, CD>, t: Throwable, msg: String
) = log(operation, TRACE, msg, t)
