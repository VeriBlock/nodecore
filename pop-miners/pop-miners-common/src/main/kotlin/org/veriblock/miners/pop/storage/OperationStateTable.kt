package org.veriblock.miners.pop.storage

import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table

object OperationStateTable : Table("operation_state") {
    val id = varchar("id", 20).primaryKey()
    val status = integer("status")
    val state = blob("state")
}

data class OperationStateRecord(
    val id: String,
    val status: Int,
    val state: ByteArray
)

fun ResultRow.toOperationStateRecord() = OperationStateRecord(
    this[OperationStateTable.id],
    this[OperationStateTable.status],
    this[OperationStateTable.state].binaryStream.readBytes()
)



