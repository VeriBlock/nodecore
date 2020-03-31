package org.veriblock.miners.pop.storage

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import javax.sql.rowset.serial.SerialBlob

class OperationRepository(
    private val database: Database
) {
    fun getActiveOperations(): List<OperationStateRecord> = transaction(database) {
        OperationStateTable.select {
            OperationStateTable.status eq 1
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
                    it[state] = SerialBlob(record.state)
                }
            }
        } else {
            transaction(database) {
                OperationStateTable.insert {
                    it[id] = record.id
                    it[status] = record.status
                    it[state] = SerialBlob(record.state)
                }
            }
        }
    }
}
