package org.veriblock.miners.pop.storage

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Query
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.statements.api.ExposedBlob
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.veriblock.miners.pop.core.MiningOperationState
import org.veriblock.miners.pop.core.MiningOperationStatus

open class OperationRepository(
    protected val database: Database
) {
    fun getOperations(
        status: MiningOperationStatus = MiningOperationStatus.ACTIVE,
        limit: Int = 50,
        offset: Int = 0
    ): List<OperationStateRecord> = transaction(database) {
        operationsFilterSelect(status).orderBy(
            OperationStateTable.createdAt
        ).limit(
            limit, offset.toLong()
        ).map {
            it.toOperationStateRecord()
        }.toList()
    }

    fun getOperationsCount(
        status: MiningOperationStatus = MiningOperationStatus.ACTIVE
    ): Int = transaction(database) {
        operationsFilterSelect(status).count().toInt()
    }

    private fun operationsFilterSelect(status: MiningOperationStatus): Query {
        return when (status) {
            MiningOperationStatus.ACTIVE -> OperationStateTable.select {
                (OperationStateTable.status greaterEq MiningOperationState.INITIAL_ID) and
                    (OperationStateTable.status less MiningOperationState.COMPLETED_ID)
            }
            MiningOperationStatus.COMPLETED -> OperationStateTable.select {
                OperationStateTable.status eq MiningOperationState.COMPLETED_ID
            }
            MiningOperationStatus.FAILED -> OperationStateTable.select {
                OperationStateTable.status eq MiningOperationState.FAILED_ID
            }
            MiningOperationStatus.ALL ->
                OperationStateTable.selectAll()
        }
    }

    fun getActiveOperations(): List<OperationStateRecord> = transaction(database) {
        OperationStateTable.select {
            (OperationStateTable.status greaterEq MiningOperationState.INITIAL_ID) and
                (OperationStateTable.status less MiningOperationState.COMPLETED_ID)
        }.map {
            it.toOperationStateRecord()
        }.toList()
    }

    fun getOperation(id: String): OperationStateRecord? = transaction(database) {
        OperationStateTable.select {
            OperationStateTable.id eq id
        }.firstOrNull()?.toOperationStateRecord()
    }

    fun saveOperationState(record: OperationStateRecord) {
        if (getOperation(record.id) != null) {
            transaction(database) {
                OperationStateTable.update({
                    OperationStateTable.id eq record.id
                }) {
                    it[status] = record.status
                    it[state] = ExposedBlob(record.state)
                    it[logs] = record.logs
                }
            }
        } else {
            transaction(database) {
                OperationStateTable.insert {
                    it[id] = record.id
                    it[status] = record.status
                    it[state] = ExposedBlob(record.state)
                    it[createdAt] = record.createdAt
                    it[logs] = record.logs
                }
            }
        }
    }
}
