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
import java.sql.SQLException;
import java.sql.Statement;

public class PoPTransactionsVeriblockPublicationRefRepository {

    private Connection connectionSource;

    public static final String tableName = "PoPTransactionsVeriblockPublicationRef";
    public static final String txHashColumnName = "txHash";
    public static final String veriBlockPublicationHashColumnName = "veriBlockPublicationHash";

    public PoPTransactionsVeriblockPublicationRefRepository(Connection connection) throws SQLException
    {
        this.connectionSource = connection;

        Statement stmt = null;
        try{
            stmt = connectionSource.createStatement();
            stmt.execute("CREATE TABLE IF NOT EXISTS " + tableName
                    + "(\n "
                    + txHashColumnName + " TEXT NOT NULL,\n "
                    + veriBlockPublicationHashColumnName + " TEXT NOT NULL,\n "
                    + " PRIMARY KEY (" + txHashColumnName + "," + veriBlockPublicationHashColumnName + ")\n "
                    + " FOREIGN KEY (" + txHashColumnName + ")\n "
                    + " REFERENCES " + PoPTransactionsRepository.tableName + " (" + PoPTransactionsRepository.txHashColumnName + ")\n "
                    + " FOREIGN KEY (" + veriBlockPublicationHashColumnName + ")\n "
                    + " REFERENCES " + VeriBlockPublicationRepository.tableName + " (" + VeriBlockPublicationRepository.veriBlockPublicationHashColumnName + ")\n "
                    + ");");
        }
        finally{
            if(stmt != null) stmt.close();
            stmt = null;
        }

        try {
            stmt = connectionSource.createStatement();
            stmt.execute("PRAGMA journal_mode=WAL;");
        } finally {
            if(stmt != null) stmt.close();
            stmt = null;
        }

    }

    public void clear() throws SQLException
    {
        Statement stmt = null;
        try{
            stmt = connectionSource.createStatement();
            stmt.execute( "DELETE FROM " + tableName);
        }
        finally {
            if(stmt != null) stmt.close();
            stmt = null;
        }
    }

    public void save(String txHash, String veriBlockPublicationId) throws SQLException
    {
        PreparedStatement stmt = null;
        try {
            stmt = connectionSource.prepareStatement("REPLACE INTO " + tableName + " ('" + txHashColumnName + "', '" + veriBlockPublicationHashColumnName + "') " +
                    "VALUES(?, ?)");
            int i = 0;
            stmt.setObject(++i, txHash);
            stmt.setObject(++i, veriBlockPublicationId);
            stmt.execute();
        } finally {
            if(stmt != null) stmt.close();
            stmt = null;
        }
    }
}
