package org.veriblock.miners.pop.storage

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.statements.api.ExposedBlob
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.veriblock.miners.pop.core.OperationState

open class OperationRepository(
    protected val database: Database
) {
    fun getAllOperations(): List<OperationStateRecord> = transaction(database) {
        OperationStateTable.selectAll().map {
            it.toOperationStateRecord()
        }.toList()
    }

    fun getActiveOperations(): List<OperationStateRecord> = transaction(database) {
        OperationStateTable.select {
            (OperationStateTable.status greaterEq OperationState.INITIAL.id) and
                (OperationStateTable.status less OperationState.COMPLETED.id)
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
