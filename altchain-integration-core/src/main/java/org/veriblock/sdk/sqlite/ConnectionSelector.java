// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.sdk.sqlite;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.JDBC;

import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectionSelector {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionSelector.class);

    public static final String defaultDatabaseName = "database.sqlite";
    public static final String testDatabaseName = "database-test.sqlite";

    public interface Factory {
        Connection createConnection() throws SQLException;
    }

    static {
        try {
            DriverManager.registerDriver(new JDBC());
        } catch (SQLException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private ConnectionSelector() { }

    public static Connection setConnection(String databasePath) throws SQLException
    {
        if (databasePath == null) {
            return setConnectionInMemory();
        }

        logger.info("SqlLite path: '{}'", databasePath);

        String url = String.format("jdbc:sqlite:%s", databasePath);
        return DriverManager.getConnection(url);
    }

    public static Connection setConnectionInMemory(String databaseName) throws SQLException
    {
        databaseName = databaseName == null || databaseName.isEmpty()
                     ? "default"
                     : databaseName;

        logger.info("Using SqlLite in-memory store '{}'", databaseName);

        String url = String.format("jdbc:sqlite:file:%s?mode=memory&cache=shared", databaseName);
        return DriverManager.getConnection(url);
    }

    public static Connection setConnectionInMemory() throws SQLException
    {
        return setConnectionInMemory(null);
    }

    public static Connection setConnectionDefault() throws SQLException
    {
        String databasePath = Paths.get(FileManager.getDataDirectory(), defaultDatabaseName).toString();
        return setConnection(databasePath);
    }

    public static Connection setConnectionTestnet() throws SQLException
    {
        String databasePath = Paths.get(FileManager.getDataDirectory(), testDatabaseName).toString();
        return setConnection(databasePath);
    }
}
