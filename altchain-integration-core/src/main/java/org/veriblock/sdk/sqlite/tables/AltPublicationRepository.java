// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.


package org.veriblock.sdk.sqlite.tables;

import org.veriblock.sdk.models.AltPublication;
import org.veriblock.sdk.models.Sha256Hash;
import org.veriblock.sdk.services.SerializeDeserializeService;
import org.veriblock.sdk.util.Utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public class AltPublicationRepository {
    private Connection connectionSource;

    public final static String tableName = "alt_publication";
    public final static String altPublicationHash = "hash";
    public final static String altPublicationDataColumnName = "data";

    public AltPublicationRepository(Connection connection) throws SQLException {
        this.connectionSource = connection;
        try(Statement stmt = connectionSource.createStatement()){
            stmt.execute("CREATE TABLE IF NOT EXISTS " + tableName
                    + "(\n "
                    + altPublicationHash + " TEXT PRIMARY KEY,\n "
                    + altPublicationDataColumnName + " BLOB NOT NULL\n "
                    + ");");
        }

        try(Statement stmt = connectionSource.createStatement()) {
            stmt.execute("PRAGMA journal_mode=WAL;");
        }
    }

    public void clear() throws SQLException {
        try(Statement stmt = connectionSource.createStatement()){
            stmt.execute( "DELETE FROM " + tableName);
        }
    }

    public String save(AltPublication publication) throws SQLException {
        String hash;
        String sql = " REPLACE INTO " + tableName + " ('" + altPublicationHash + "' , '" + altPublicationDataColumnName + "') " +
                "VALUES(?, ?) ";
        try(PreparedStatement stmt = connectionSource.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)){
            byte[] bytes = SerializeDeserializeService.serialize(publication);
            hash = Utils.encodeHex(Sha256Hash.hash(bytes));
            stmt.setString(1, hash);
            stmt.setBytes(2, bytes);
            stmt.executeUpdate();
        }
        return hash;
    }


}
