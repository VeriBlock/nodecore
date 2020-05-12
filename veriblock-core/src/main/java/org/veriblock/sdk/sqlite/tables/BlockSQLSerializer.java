// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.sdk.sqlite.tables;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public interface BlockSQLSerializer<Block, Id> {

    // populate a prepared statement with block data
    void toStmt(Block block, PreparedStatement stmt) throws SQLException;

    // convert an SQL row to a block
    Block fromResult(ResultSet result)  throws SQLException;

    // get the SQL database schema to use to store block data
    String getSchema();

    String addIndexes();

    String removeIndexes();

    // get the list of column names that match the schema
    List<String> getColumns();

    // convert an id to a string
    String idToString(Id id);
}
