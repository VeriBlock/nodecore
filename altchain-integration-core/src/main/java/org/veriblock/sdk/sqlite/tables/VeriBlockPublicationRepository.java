// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.sdk.sqlite.tables;

import org.veriblock.sdk.models.Sha256Hash;
import org.veriblock.sdk.models.VeriBlockPublication;
import org.veriblock.sdk.services.SerializeDeserializeService;
import org.veriblock.sdk.util.Utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public class VeriBlockPublicationRepository {


    private Connection connectionSource;

    public final static String tableName = "VeriBlockPublication";
    public final static String veriBlockPublicationHashColumnName = "veriBlockPublicationHash";
    public final static String veriBlockPublicationDataColumnName = "veriBlockPublicationData";

    public VeriBlockPublicationRepository(Connection connection) throws SQLException
    {
        this.connectionSource = connection;
        Statement stmt = null;
        try{
            stmt = connectionSource.createStatement();
            stmt.execute("CREATE TABLE IF NOT EXISTS " + tableName
                    + "(\n "
                    + veriBlockPublicationHashColumnName + " TEXT PRIMARY KEY,\n "
                    + veriBlockPublicationDataColumnName + " BLOB NOT NULL\n "
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
        }
    }

    public String save(VeriBlockPublication publication) throws SQLException
    {
        String hash = "";
        PreparedStatement stmt = null;
        try{
            stmt = connectionSource.prepareStatement(" REPLACE INTO " + tableName + " ('" + veriBlockPublicationHashColumnName + "', '" + veriBlockPublicationDataColumnName + "') " +
                    "VALUES(?, ?) ", Statement.RETURN_GENERATED_KEYS);
            byte[] bytes = SerializeDeserializeService.serialize(publication);
            hash = Utils.encodeHex(Sha256Hash.hash(bytes));
            stmt.setString(1, hash);
            stmt.setBytes(2, bytes);
            stmt.executeUpdate();
        }
        finally {
            if(stmt != null) stmt.close();
        }

        return hash;
    }
}
