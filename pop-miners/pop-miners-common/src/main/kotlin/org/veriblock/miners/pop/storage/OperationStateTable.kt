package org.veriblock.miners.pop.storage

import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.`java-time`.datetime
import java.time.LocalDateTime

object OperationStateTable : Table("operation_state") {
    val id = varchar("id", 32).primaryKey()
    val status = integer("status")
    val state = blob("state")
    val createdAt = datetime("created_at")
}

data class OperationStateRecord(
    val id: String,
    val status: Int,
    val state: ByteArray,
    val createdAt: LocalDateTime
)

fun ResultRow.toOperationStateRecord() = OperationStateRecord(
    this[OperationStateTable.id],
    this[OperationStateTable.status],
    this[OperationStateTable.state].bytes,
    this[OperationStateTable.createdAt]
)
