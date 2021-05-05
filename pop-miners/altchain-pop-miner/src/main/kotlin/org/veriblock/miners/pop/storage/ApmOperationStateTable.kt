package org.veriblock.miners.pop.storage

import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.`java-time`.datetime
import java.time.LocalDateTime

object ApmOperationStateTable : Table("operation_state") {
    val id = varchar("id", 32)
    val chainId = varchar("chain_id", 10).default("none")
    val status = integer("status")
    val state = blob("state")
    val createdAt = datetime("created_at")
    val logs = text("logs").default("[]")

    override val primaryKey = PrimaryKey(id)
}

data class ApmOperationStateRecord(
    val id: String,
    val chainId: String,
    val status: Int,
    val state: ByteArray,
    val createdAt: LocalDateTime,
    val logs: String
)

fun ResultRow.toApmOperationStateRecord() = ApmOperationStateRecord(
    this[ApmOperationStateTable.id],
    this[ApmOperationStateTable.chainId],
    this[ApmOperationStateTable.status],
    this[ApmOperationStateTable.state].bytes,
    this[ApmOperationStateTable.createdAt],
    this[ApmOperationStateTable.logs]
)
