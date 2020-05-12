// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.sdk.sqlite;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

public class SqliteDefaultConnectorTest {

    @Test
    public void createConnectionTest() throws IOException, SQLException {
        Connection connection = ConnectionSelector.setConnectionInMemory();
        Assert.assertTrue(connection != null);
        connection.close();
    }
}
