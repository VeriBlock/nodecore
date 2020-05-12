// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.sdk.sqlite.tables;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class KeyValueRepository {

    private Connection connectionSource;

    public KeyValueRepository(Connection connection) throws SQLException {
        this.connectionSource = connection;

        try (Statement stmt = connectionSource.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS genericCache (\n"
                    + " key TEXT PRIMARY KEY,\n"
                    + " value TEXT\n"
                    + ");");
        }

        try (Statement stmt = connectionSource.createStatement()) {
            stmt.execute("PRAGMA journal_mode=WAL;");
        }
    }

    public void clear() throws SQLException {
        try (Statement stmt = connectionSource.createStatement()) {
            stmt.execute("DELETE FROM genericCache");
        }
    }

    public void save(String key, String value) throws SQLException {
        String statement = "REPLACE INTO genericCache ('key', 'value') VALUES(?, ?)";
        try (PreparedStatement stmt = connectionSource.prepareStatement(statement)) {
            int i = 0;
            stmt.setObject(++i, key);
            stmt.setObject(++i, value);
            stmt.execute();
        }
    }

    public String getValue(String key) throws SQLException {
        String statement = "SELECT key, value FROM genericCache WHERE key = ?";
        try (PreparedStatement stmt = connectionSource.prepareStatement(statement)) {
            int i = 0;
            stmt.setObject(++i, key);
            try (ResultSet resultSet = stmt.executeQuery()) {
                if (!resultSet.next()) return null;

                String value = resultSet.getString("value");

                if (!resultSet.next()) return value;

                throw new SQLException("Not an unique id: " + key);
            }
        }
    }
}
