// VeriBlock PoP Miner
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.miners.pop.storage

import com.j256.ormlite.dao.Dao
import com.j256.ormlite.dao.DaoManager
import com.j256.ormlite.support.ConnectionSource
import com.j256.ormlite.table.TableUtils
import nodecore.miners.pop.model.TransactionStatus
import org.veriblock.core.utilities.createLogger
import java.sql.SQLException

private val logger = createLogger {}

class PopRepository(
    connectionSource: ConnectionSource
) {
    private var operationStateDataDao: Dao<OperationStateData, String>

    init {
        try {
            TableUtils.createTableIfNotExists(
                connectionSource, OperationStateData::class.java
            )
            operationStateDataDao = DaoManager.createDao(
                connectionSource, OperationStateData::class.java
            )
            operationStateDataDao.executeRaw("PRAGMA journal_mode=WAL;")
        } catch (e: SQLException) {
            logger.error("SQL Error: {}", e.sqlState, e)
            throw e
        }
    }

    fun getActiveOperations(): Iterator<OperationStateData>? {
        try {
            return operationStateDataDao.queryBuilder().where().eq("is_done", false).iterator()
        } catch (e: SQLException) {
            logger.error("SQL Error: {}", e.sqlState, e)
        }
        return null
    }

    fun getOperation(id: String): OperationStateData? {
        try {
            return operationStateDataDao.queryForId(id)
        } catch (e: SQLException) {
            logger.error("SQL Error: {}", e.sqlState, e)
        }
        return null
    }

    fun getUnconfirmedTransactionCount(): Long {
        try {
            return operationStateDataDao.queryBuilder().where().eq("transaction_status", TransactionStatus.UNCONFIRMED.name).countOf()
        } catch (e: SQLException) {
            logger.error("SQL Error: {}", e.sqlState, e)
        }
        return -1L
    }

    fun saveOperationState(stateData: OperationStateData) {
        try {
            operationStateDataDao.createOrUpdate(stateData)
        } catch (e: SQLException) {
            logger.error("SQL Error: {}", e.sqlState, e)
        }
    }
}
