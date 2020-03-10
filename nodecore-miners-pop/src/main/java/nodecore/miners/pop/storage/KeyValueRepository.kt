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
import org.veriblock.core.utilities.createLogger
import java.sql.SQLException

private val logger = createLogger {}

class KeyValueRepository(
    connectionSource: ConnectionSource
) {
    private var keyValueDataDao: Dao<KeyValueData, String>

    init {
        try {
            TableUtils.createTableIfNotExists(
                connectionSource, KeyValueData::class.java
            )
            keyValueDataDao = DaoManager.createDao(
                connectionSource, KeyValueData::class.java
            )
        } catch (e: SQLException) {
            logger.error("SQL Error: {}", e.sqlState, e)
            throw e
        }
    }

    fun insert(data: KeyValueData) {
        try {
            keyValueDataDao.createOrUpdate(data)
        } catch (e: SQLException) {
            logger.error("SQL Error: {}", e.sqlState, e)
        }
    }

    operator fun get(key: String): KeyValueData? {
        try {
            return keyValueDataDao.queryForId(key)
        } catch (e: SQLException) {
            logger.error("SQL Error: {}", e.sqlState, e)
        }
        return null
    }
}
