// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.storage.ormlite

import com.j256.ormlite.dao.Dao
import com.j256.ormlite.dao.DaoManager
import com.j256.ormlite.support.ConnectionSource
import com.j256.ormlite.table.TableUtils
import org.veriblock.miners.pop.storage.OperationStateData
import org.veriblock.miners.pop.storage.OperationRepository
import org.veriblock.core.utilities.createLogger
import java.sql.SQLException

private val logger = createLogger {}

class OrmLitePoPRepository(
    connectionSource: ConnectionSource
) : OperationRepository {

    private var connectionSource: ConnectionSource? = null
    private var operationStateDataDao: Dao<OperationStateData, String>? = null

    init {
        try {
            this.connectionSource = connectionSource
            TableUtils.createTableIfNotExists(connectionSource, OperationStateData::class.java)
            operationStateDataDao = DaoManager.createDao(connectionSource, OperationStateData::class.java)

            operationStateDataDao!!.executeRaw("PRAGMA journal_mode=WAL;")
        } catch (e: SQLException) {
            logger.error(e) { "SQL Error: ${e.sqlState}" }
        }
    }

    override fun getActiveOperations(): Iterator<OperationStateData> {
        try {
            return operationStateDataDao!!.queryBuilder()
                .where()
                .eq("status", 1)
                .iterator()
        } catch (e: SQLException) {
            logger.error(e) { "SQL Error: ${e.sqlState}" }
            throw e
        }
    }

    override fun getOperation(id: String): OperationStateData? {
        try {
            return operationStateDataDao!!.queryForId(id)
        } catch (e: SQLException) {
            logger.error(e) { "SQL Error: ${e.sqlState}" }
        }

        return null
    }

    override fun saveOperationState(stateData: OperationStateData) {
        try {
            operationStateDataDao!!.createOrUpdate(stateData)
        } catch (e: SQLException) {
            logger.error(e) { "SQL Error: ${e.sqlState}" }
        }

    }
}
