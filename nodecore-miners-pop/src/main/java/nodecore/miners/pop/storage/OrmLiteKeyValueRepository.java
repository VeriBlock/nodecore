// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.storage;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import nodecore.miners.pop.contracts.KeyValueData;
import nodecore.miners.pop.contracts.KeyValueRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

public class OrmLiteKeyValueRepository implements KeyValueRepository {
    private static final Logger logger = LoggerFactory.getLogger(OrmLiteKeyValueRepository.class);

    private Dao<KeyValueData, String> keyValueDataDao;

    public OrmLiteKeyValueRepository(ConnectionSource connectionSource) {
        try {
            TableUtils.createTableIfNotExists(connectionSource, KeyValueData.class);
            keyValueDataDao = DaoManager.createDao(connectionSource, KeyValueData.class);
        } catch (SQLException e) {
            logger.error("SQL Error: {}", e.getSQLState(), e);
        }
    }

    @Override
    public void insert(KeyValueData data) {
        try {
            keyValueDataDao.createOrUpdate(data);
        } catch (SQLException e) {
            logger.error("SQL Error: {}", e.getSQLState(), e);
        }
    }

    @Override
    public KeyValueData get(String key) {
        try {
            return keyValueDataDao.queryForId(key);
        } catch (SQLException e) {
            logger.error("SQL Error: {}", e.getSQLState(), e);
        }

        return null;
    }
}
