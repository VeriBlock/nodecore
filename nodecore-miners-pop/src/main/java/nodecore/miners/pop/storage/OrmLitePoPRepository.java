// VeriBlock PoP Miner
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.storage;

import com.google.inject.Inject;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import nodecore.miners.pop.contracts.PoPRepository;
import nodecore.miners.pop.contracts.TransactionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.Iterator;

public class OrmLitePoPRepository implements PoPRepository {
    private static final Logger logger = LoggerFactory.getLogger(OrmLitePoPRepository.class);

    private ConnectionSource connectionSource;
    private Dao<OperationStateData, String> operationStateDataDao;

    @Inject
    public OrmLitePoPRepository(ConnectionSource connectionSource) {
        try {
            this.connectionSource = connectionSource;
            TableUtils.createTableIfNotExists(connectionSource, OperationStateData.class);
            operationStateDataDao = DaoManager.createDao(connectionSource, OperationStateData.class);

            operationStateDataDao.executeRaw("PRAGMA journal_mode=WAL;");
        } catch (SQLException e) {
            logger.error("SQL Error: {}", e.getSQLState(), e);
        }
    }

    @Override
    public Iterator<OperationStateData> getActiveOperations() {
        try {
            return operationStateDataDao.queryBuilder()
                    .where()
                    .eq("is_done", false)
                    .iterator();
        } catch (SQLException e) {
            logger.error("SQL Error: {}", e.getSQLState(), e);
        }

        return null;
    }

    @Override
    public OperationStateData getOperation(String id) {
        try {
            return operationStateDataDao.queryForId(id);
        } catch (SQLException e) {
            logger.error("SQL Error: {}", e.getSQLState(), e);
        }

        return null;
    }

    @Override
    public long getUnconfirmedTransactionCount() {
        try {
            return operationStateDataDao.queryBuilder()
                    .where()
                    .eq("transaction_status", TransactionStatus.UNCONFIRMED.name())
                    .countOf();
        } catch (SQLException e) {
            logger.error("SQL Error: {}", e.getSQLState(), e);
        }

        return -1L;
    }

    @Override
    public void saveOperationState(OperationStateData stateData) {
        try {
            operationStateDataDao.createOrUpdate(stateData);
        } catch (SQLException e) {
            logger.error("SQL Error: {}", e.getSQLState(), e);
        }
    }
}
