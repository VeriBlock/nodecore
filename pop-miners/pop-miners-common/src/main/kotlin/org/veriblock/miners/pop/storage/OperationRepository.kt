package org.veriblock.miners.pop.storage

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.statements.api.ExposedBlob
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.veriblock.miners.pop.core.MiningOperationState

open class OperationRepository(
    protected val database: Database
) {
    fun getAllOperations(): List<OperationStateRecord> = transaction(database) {
        OperationStateTable.selectAll().map {
            it.toOperationStateRecord()
        }.toList()
    }

    fun getOperationsByState(state: Int, limit: Int): List<OperationStateRecord> = transaction(database) {
        OperationStateTable.select {
            if (state == -2) {
                (OperationStateTable.status greater state)
            } else {
                (OperationStateTable.status eq state)
            }
        }.orderBy(OperationStateTable.createdAt).limit(limit).map {
            it.toOperationStateRecord()
        }.toList()
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
