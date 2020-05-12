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
import java.util.ArrayList;
import java.util.List;

public class GenericBlockRepository<Block, Id> {
    protected Connection connectionSource;
    protected String tableBlocks;
    protected BlockSQLSerializer<Block, Id> serializer;

    public GenericBlockRepository(Connection connection, String tableName, BlockSQLSerializer<Block, Id> serializer) throws SQLException {
        this.connectionSource = connection;
        this.tableBlocks = tableName;
        this.serializer = serializer;

        try (Statement stmt = connectionSource.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS "
                    + tableBlocks
                    + " (\n"
                    + serializer.getSchema()
                    + ");");

            stmt.execute(String.format("CREATE INDEX IF NOT EXISTS %s ON %s(%s)",
                    tableName + "previousId",
                    tableName, "previousId"));

            stmt.execute("PRAGMA journal_mode=WAL;");
            stmt.execute("PRAGMA case_sensitive_like=ON;");
        }
        addIndexes();
    }

    public void clear() throws SQLException {
        try (Statement stmt = connectionSource.createStatement()) {
            stmt.execute("DELETE FROM " + tableBlocks);
        }
    }

    private String getColumnsString() {
        List<String> columns = serializer.getColumns();
        List<String> quotedColumns = new ArrayList<String>();
        for (String col : columns)
            quotedColumns.add("'" + col + "'");

        return String.join(", ", quotedColumns);
    }

    private String getValuesString() {
        int columnCount = serializer.getColumns().size();

        String values = columnCount == 0 ? "" : "?";
        for (int i = 1; i < columnCount; i++)
            values += ", ?";

        return values;
    }

    public void save(Block block) throws SQLException {
        String statement = "REPLACE INTO "
                + tableBlocks
                + " (" + getColumnsString() + ") "
                + "VALUES(" + getValuesString() + ")";
        try (PreparedStatement stmt = connectionSource.prepareStatement(statement)) {
            serializer.toStmt(block, stmt);
            stmt.execute();
        }
    }

    public void saveAll(List<Block> blocks) throws SQLException {
        String statement = "REPLACE INTO "
                + tableBlocks
                + " (" + getColumnsString() + ") "
                + "VALUES(" + getValuesString() + ")";

        boolean autoCommit = connectionSource.getAutoCommit();
        connectionSource.setAutoCommit(false);
        try (PreparedStatement stmt = connectionSource.prepareStatement(statement)) {
            for (Block block : blocks) {
                serializer.toStmt(block, stmt);
                stmt.addBatch();
            }
            stmt.executeBatch();
        } finally {
            connectionSource.setAutoCommit(autoCommit);
        }
    }

    public Block get(Id id) throws SQLException {
        String statement = "SELECT * FROM " + tableBlocks + " WHERE id = ?";
        try (PreparedStatement stmt = connectionSource.prepareStatement(statement)) {
            int i = 0;
            stmt.setObject(++i, serializer.idToString(id));

            List<Block> values = new ArrayList<Block>();
            try (ResultSet resultSet = stmt.executeQuery()) {
                while (resultSet.next())
                    values.add(serializer.fromResult(resultSet));
            }

            if (values.size() > 1) throw new SQLException("Not an unique id: " + id);

            return values.size() == 0 ? null : values.get(0);
        }
    }

    public List<Block> getEndsWithId(Id id) throws SQLException {
        String statement = "SELECT * FROM " + tableBlocks + " WHERE id LIKE ?";
        try (PreparedStatement stmt = connectionSource.prepareStatement(statement)) {
            int i = 0;
            stmt.setObject(++i, serializer.idToString(id) + "%");

            List<Block> values = new ArrayList<Block>();
            try (ResultSet resultSet = stmt.executeQuery()) {
                while (resultSet.next())
                    values.add(serializer.fromResult(resultSet));
            }

            return values;
        }
    }

    public boolean isInUse(Id id) throws SQLException {
        String statement = "SELECT * FROM " + tableBlocks + " WHERE previousId = ?";
        try (PreparedStatement stmt = connectionSource.prepareStatement(statement)) {
            int i = 0;
            stmt.setObject(++i, serializer.idToString(id));

            try (ResultSet result = stmt.executeQuery()) {
                return result.next();
            }
        }
    }

    public List<Block> getAll() throws SQLException {
        try (Statement stmt = connectionSource.createStatement()) {
            List<Block> values = new ArrayList<Block>();
            try (ResultSet resultSet = stmt.executeQuery("SELECT * FROM " + tableBlocks)) {
                while (resultSet.next())
                    values.add(serializer.fromResult(resultSet));

            }
            return values;
        }
    }

    public void delete(Id id) throws SQLException {
        String statement = "DELETE FROM " + tableBlocks + " WHERE id = ?";
        try (PreparedStatement stmt = connectionSource.prepareStatement(statement)) {
            int i = 0;
            stmt.setObject(++i, serializer.idToString(id));
            stmt.execute();
        }
    }

    public void addIndexes() throws SQLException {
        try (Statement stmt = connectionSource.createStatement()) {
            stmt.execute(serializer.addIndexes());
        }
    }

    public void removeIndexes() throws SQLException {
        try (Statement stmt = connectionSource.createStatement()) {
            stmt.execute(serializer.removeIndexes());
        }
    }

}
