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
import org.veriblock.sdk.blockchain.store.StoredVeriBlockBlock;
import org.veriblock.sdk.models.Sha256Hash;
import org.veriblock.sdk.services.SerializeDeserializeService;
import org.veriblock.sdk.sqlite.ConnectionSelector;
import org.veriblock.sdk.util.Utils;

import java.io.IOException;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class VeriBlockBlockRepositoryTest {
    private StoredVeriBlockBlock newBlock;
    private StoredVeriBlockBlock newBlock2;
    private StoredVeriBlockBlock newBlock3;

    private Connection connection;
    private VeriBlockBlockRepository repo;

    @Before
    public void init() throws SQLException {
        byte[] raw =  Utils.decodeHex("0001998300029690ACA425987B8B529BEC04654A16FCCE708F3F0DEED25E1D2513D05A3B17C49D8B3BCFEFC10CB2E9C4D473B2E25DB7F1BD040098960DE0E313");
        newBlock = new StoredVeriBlockBlock(SerializeDeserializeService.parseVeriBlockBlock(raw), BigInteger.TEN);

        byte[] raw2 = Utils.decodeHex("000199840002A69BF9FE9B06E641B61699A9654A16FCCE708F3F0DEED25E1D2513D05A3B7D7F80EB5E94D01C6B3796DDE5647F135DB7F1DD040098960EA12045");
        newBlock2 = new StoredVeriBlockBlock(SerializeDeserializeService.parseVeriBlockBlock(raw2), BigInteger.ONE);

        byte[] raw3 = Utils.decodeHex("000199850002461DB458CD6258D3571D4A2A654A16FCCE708F3F0DEED25E1D2513D05A3BB0B8A658CBFFCFBE9185AFDE789841EC5DB7F2360400989610B1662B");
        newBlock3 = new StoredVeriBlockBlock(SerializeDeserializeService.parseVeriBlockBlock(raw3), BigInteger.ZERO);

        connection = ConnectionSelector.setConnectionInMemory();
        repo = new VeriBlockBlockRepository(connection);
        repo.clear();
    }

    @After
    public void closeConnection() throws SQLException {
        if (connection != null)
            connection.close();
    }
    
    @Test
    public void getEndsWithIdNonexistentBlockTest() throws SQLException, IOException {
        List<StoredVeriBlockBlock> blocks = repo.getEndsWithId(newBlock.getHash());
        Assert.assertTrue(blocks.isEmpty());
    }

    @Test
    public void getNonexistentBlockTest() throws SQLException, IOException {
        StoredVeriBlockBlock block = repo.get(newBlock.getHash());
        Assert.assertEquals(block, null);
    }

    @Test
    public void deleteNonexistentBlockTest() throws SQLException, IOException {
        repo.delete(newBlock.getHash());
    }

    @Test
    public void deleteBlockTest() throws SQLException, IOException {
        repo.save(newBlock);
        repo.delete(newBlock.getHash());

        List<StoredVeriBlockBlock> blocks = repo.getEndsWithId(newBlock.getHash());
        Assert.assertTrue(blocks.isEmpty());
    }

    @Test
    public void addGetBlockTest() throws SQLException, IOException {
        repo.save(newBlock);
        StoredVeriBlockBlock block = repo.get(newBlock.getHash());
        Assert.assertEquals(block, newBlock);
    }

    @Test
    public void addGetEndsWithIdBlockTest() throws SQLException, IOException {
        repo.save(newBlock);
        List<StoredVeriBlockBlock> blocks = repo.getEndsWithId(newBlock.getHash());
        Assert.assertFalse(blocks.isEmpty());
        Assert.assertEquals(blocks.get(0), newBlock);
    }

    @Test
    public void addBlockWithBlockOfProofTest() throws SQLException, IOException {
        Sha256Hash blockOfProof = Sha256Hash.wrap("00000000000000b345b7bbf29bda1507a679b97967f99a10ab0088899529def7");
        newBlock.setBlockOfProof(blockOfProof);

        repo.save(newBlock);
        List<StoredVeriBlockBlock> blocks = repo.getEndsWithId(newBlock.getHash());
        Assert.assertFalse(blocks.isEmpty());
        Assert.assertEquals(blocks.get(0), newBlock);
    }

    @Test
    public void clearTest() throws SQLException, IOException {
        repo.save(newBlock);
        repo.save(newBlock2);
        repo.save(newBlock3);

        repo.clear();

        List<StoredVeriBlockBlock> blocks = repo.getAll();
        Assert.assertTrue(blocks.isEmpty());
    }

    @Test
    public void getAllEmptyRepoTest() throws SQLException, IOException {
        List<StoredVeriBlockBlock> blocks = repo.getAll();
        Assert.assertTrue(blocks.isEmpty());
    }

    @Test
    public void getAllTest() throws SQLException, IOException {
        Comparator<StoredVeriBlockBlock> comparator = (b1, b2) -> b1.getHash().toString().compareTo(b2.getHash().toString());

        repo.save(newBlock);
        repo.save(newBlock2);
        repo.save(newBlock3);

        List<StoredVeriBlockBlock> expectedBlocks = new ArrayList<StoredVeriBlockBlock>();
        expectedBlocks.add(newBlock);
        expectedBlocks.add(newBlock2);
        expectedBlocks.add(newBlock3);
        expectedBlocks.sort(comparator);

        List<StoredVeriBlockBlock> blocks = repo.getAll();
        blocks.sort(comparator);

        Assert.assertEquals(blocks, expectedBlocks);
    }

    @Test
    public void addGetBlockTrimmedTest() throws SQLException, IOException {
        repo.save(newBlock);
        List<StoredVeriBlockBlock> blocks = repo.getEndsWithId(newBlock.getHash().trimToPreviousBlockSize());
        Assert.assertFalse(blocks.isEmpty());
        Assert.assertEquals(blocks.get(0), newBlock);

        blocks = repo.getEndsWithId(newBlock.getHash().trimToPreviousKeystoneSize());
        Assert.assertFalse(blocks.isEmpty());
        Assert.assertEquals(blocks.get(0), newBlock);
    }

    //FIXME: fix and test IsInUse()
}
