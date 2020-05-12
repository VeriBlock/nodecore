// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.sdk.sqlite.tables;

import org.veriblock.sdk.models.AltChainBlock;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ContainRepository {
    private Connection connectionSource;

    public static final String tableName = "contain";
    public static final String txHashColumnName = "tx_hash";
    public static final String blockHashColumnName = "block_hash";
    public static final String blockHeightColumnName = "block_height";
    public static final String blockTimestampColumnName = "block_timestamp";

    public ContainRepository(Connection connection) throws SQLException {
        this.connectionSource = connection;

        try(Statement stmt = connectionSource.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS " + tableName
                    + "(\n "
                    + txHashColumnName + " TEXT NOT NULL,\n "
                    + blockHashColumnName + " TEXT NOT NULL,\n "
                    + blockHeightColumnName + " INT NOT NULL,\n "
                    + blockTimestampColumnName + " INT NOT NULL,\n "
                    + " PRIMARY KEY (" + txHashColumnName + ", " + blockHashColumnName + ")\n "
                    + " FOREIGN KEY (" + txHashColumnName + ")\n "
                    + " REFERENCES " + PoPTransactionsRepository.tableName + " (" + PoPTransactionsRepository.txHashColumnName + ")\n "
                    + ");");

            stmt.execute(String.format("CREATE INDEX IF NOT EXISTS %s ON %s(%s)",
                                       tableName + txHashColumnName,
                                       tableName, txHashColumnName));

            stmt.execute(String.format("CREATE INDEX IF NOT EXISTS %s ON %s(%s)",
                                       tableName + blockHashColumnName,
                                       tableName, blockHashColumnName));

            stmt.execute("PRAGMA journal_mode=WAL;");
        }
    }

    public void clear() throws SQLException {
        try(Statement stmt = connectionSource.createStatement()){
            stmt.execute( "DELETE FROM " + tableName);
        }
    }

    public void save(String txHash, AltChainBlock containingBlock) throws SQLException {
        String sql = String.format("REPLACE INTO %s (%s, %s, %s, %s) VALUES(?, ?, ?, ?)", tableName,
                txHashColumnName, blockHashColumnName, blockHeightColumnName, blockTimestampColumnName);
        try(PreparedStatement stmt = connectionSource.prepareStatement(sql)) {
            int i = 0;
            stmt.setObject(++i, txHash);
            stmt.setObject(++i, containingBlock.getHash());
            stmt.setLong(++i, containingBlock.getHeight());
            stmt.setInt(++i, containingBlock.getTimestamp());
            stmt.execute();
        }
    }

    public boolean isExist(AltChainBlock altChainBlock) throws SQLException {
        return get(altChainBlock.getHash()) != null;
    }

    public AltChainBlock get(String hash) throws SQLException {
        AltChainBlock altChainBlock = null;

        String sql = "SELECT * FROM " + tableName + " WHERE " + blockHashColumnName + "  = ?";
        try (PreparedStatement stmt = connectionSource.prepareStatement(sql)) {
            int i = 0;
            stmt.setObject(++i, hash);

            try (ResultSet resultSet = stmt.executeQuery()) {
                if (!resultSet.isClosed()) {
                    altChainBlock = mapper(resultSet);
                }
            }
        }
        return altChainBlock;
    }

    public List<AltChainBlock> getAllFromHeight(Long height) throws SQLException {
        List<AltChainBlock> altChainBlock = new ArrayList<>();

        String sql = "SELECT * FROM " + tableName + " WHERE " + blockHeightColumnName + " >= ?";
        try (PreparedStatement stmt = connectionSource.prepareStatement(sql)) {
            int i = 0;
            stmt.setLong(++i, height);

            try (ResultSet resultSet = stmt.executeQuery()) {
                while (resultSet.next()) {
                    altChainBlock.add(mapper(resultSet));
                }
            }
        }
        return altChainBlock;
    }

    private AltChainBlock mapper(ResultSet resultSet) throws SQLException {
        return new AltChainBlock(resultSet.getString(blockHashColumnName), resultSet.getLong(blockHeightColumnName), resultSet.getInt(blockTimestampColumnName));
    }
}
