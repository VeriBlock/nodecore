// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.sdk.sqlite.tables;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.veriblock.sdk.sqlite.ConnectionSelector;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

public class KeyValueRepositoryTest {
    
    private Connection connection;
    private KeyValueRepository repository;

    @Before
    public void setUp() throws SQLException {
        connection = ConnectionSelector.setConnectionInMemory();
        repository = new KeyValueRepository(connection);
        repository.clear();
    }
    
    @After
    public void tearDown() throws IOException, SQLException {
        if(connection != null) connection.close();
    }

    @Test
    public void createMultipleKVTest() throws IOException, SQLException {
        {
            String key = "1";
            String value = "test";
            repository.save(key, value);

            Assert.assertEquals(value, repository.getValue(key));
        }
        {
            String key = "2";
            String value = "should be different from the first KV";
            repository.save(key, value);

            Assert.assertEquals(value, repository.getValue(key));
        }
    }

    @Test
    public void updateKVTest() throws IOException, SQLException {
        String key = "1";
        String value = "test";
        repository.save(key, value);

        Assert.assertEquals(value, repository.getValue(key));
        
        String newValue = "different";
        repository.save(key, newValue);

        Assert.assertEquals(newValue, repository.getValue(key));
    }

    @Test
    public void getNonexistentKVTest() throws IOException, SQLException {
        String key = "1";

        Assert.assertEquals(null, repository.getValue(key));
    }

    @Test
    public void clearTest() throws IOException, SQLException {
        String key = "1";
        String value = "test";
        repository.save(key, value);

        Assert.assertEquals(value, repository.getValue(key));

        repository.clear();

        Assert.assertEquals(null, repository.getValue(key));
    }

}
