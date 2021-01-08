package org.veriblock.miners.pop.storage

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Expression
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.Query
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.statements.api.ExposedBlob
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.veriblock.miners.pop.core.MiningOperationState
import org.veriblock.miners.pop.core.MiningOperationStatus

open class ApmOperationRepository(
    protected val database: Database
) {
    fun getOperations(
        chainId: String? = null,
        status: MiningOperationStatus = MiningOperationStatus.ACTIVE,
        limit: Int = 50,
        offset: Int = 0
    ): List<ApmOperationStateRecord> = transaction(database) {
        operationsFilterSelect(chainId, status).orderBy(
            ApmOperationStateTable.createdAt
        ).limit(
            limit, offset.toLong()
        ).map {
            it.toApmOperationStateRecord()
        }.toList()
    }

    fun getOperationsCount(
        chainId: String? = null,
        status: MiningOperationStatus = MiningOperationStatus.ACTIVE
    ): Int = transaction(database) {
        operationsFilterSelect(chainId, status).count().toInt()
    }

    private fun operationsFilterSelect(chainId: String?, status: MiningOperationStatus): Query {
        val chainFilter: SqlExpressionBuilder.() -> Op<Boolean> = {
            if (chainId != null) {
                ApmOperationStateTable.chainId eq chainId
            } else {
                Op.TRUE
            }
        }
        return when (status) {
            MiningOperationStatus.ACTIVE -> ApmOperationStateTable.select {
                chainFilter() and
                    (ApmOperationStateTable.status greaterEq MiningOperationState.INITIAL_ID) and
                    (ApmOperationStateTable.status less MiningOperationState.COMPLETED_ID)
            }
            MiningOperationStatus.COMPLETED -> ApmOperationStateTable.select {
                chainFilter() and
                    (ApmOperationStateTable.status eq MiningOperationState.COMPLETED_ID)
            }
            MiningOperationStatus.FAILED -> ApmOperationStateTable.select {
                chainFilter() and
                    (ApmOperationStateTable.status eq MiningOperationState.FAILED_ID)
            }
            MiningOperationStatus.ALL ->
                ApmOperationStateTable.select {
                    chainFilter()
                }
        }
    }

    fun getActiveOperations(): List<ApmOperationStateRecord> = transaction(database) {
        ApmOperationStateTable.select {
            (ApmOperationStateTable.status greaterEq MiningOperationState.INITIAL_ID) and
                (ApmOperationStateTable.status less MiningOperationState.COMPLETED_ID)
        }.map {
            it.toApmOperationStateRecord()
        }.toList()
    }

    fun getOperation(id: String): ApmOperationStateRecord? = transaction(database) {
        ApmOperationStateTable.select {
            ApmOperationStateTable.id eq id
        }.firstOrNull()?.toApmOperationStateRecord()
    }

    fun saveOperationState(record: ApmOperationStateRecord) {
        if (getOperation(record.id) != null) {
            transaction(database) {
                ApmOperationStateTable.update({
                    ApmOperationStateTable.id eq record.id
                }) {
                    it[status] = record.status
                    it[state] = ExposedBlob(record.state)
                    it[logs] = record.logs
                }
            }
        } else {
            transaction(database) {
                ApmOperationStateTable.insert {
                    it[id] = record.id
                    it[chainId] = record.chainId
                    it[status] = record.status
                    it[state] = ExposedBlob(record.state)
                    it[createdAt] = record.createdAt
                    it[logs] = record.logs
                }
            }
        }
    }
}
