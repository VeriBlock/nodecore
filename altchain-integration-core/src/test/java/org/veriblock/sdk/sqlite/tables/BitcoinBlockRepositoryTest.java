// Bitcoin Blockchain Project
// Copyright 2017-2018 Bitcoin, Inc
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
import org.veriblock.sdk.blockchain.store.StoredBitcoinBlock;
import org.veriblock.sdk.models.BitcoinBlock;
import org.veriblock.sdk.models.Sha256Hash;
import org.veriblock.sdk.sqlite.ConnectionSelector;

import java.io.IOException;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class BitcoinBlockRepositoryTest {
    private StoredBitcoinBlock newBlock;
    private StoredBitcoinBlock newBlock2;
    private StoredBitcoinBlock newBlock3;

    private Connection connection;
    private BitcoinBlockRepository repo;

    @Before
    public void init() throws SQLException {
        BitcoinBlock block  = new BitcoinBlock(766099456,
                                    Sha256Hash.wrap("00000000000000000004dc9c42c22f489ade54a9349e3a47aee5b55069062afd"),
                                    Sha256Hash.wrap("87839c0e4c6771557ef02a5076c8b46a7157e5532eff7153293791ca852d2e58"),
                                    1572336145, 0x17148edf, 790109764);
        Assert.assertEquals(block.getHash(),
                            Sha256Hash.wrap("0000000000000000000faad7ae177b313ee4e3f1da519dbbf5b3ab58ccff6338"));

        BitcoinBlock block2 = new BitcoinBlock(1073733632,
                                    Sha256Hash.wrap("0000000000000000000faad7ae177b313ee4e3f1da519dbbf5b3ab58ccff6338"),
                                    Sha256Hash.wrap("902e5a70c8fa99fb9ba6d0f855f5e84b8ffc3fe56b694889d07031d8adb6a0f8"),
                                    1572336708, 0x17148edf, 344118374);
        Assert.assertEquals(block2.getHash(),
                            Sha256Hash.wrap("00000000000000000001163c9e1130c26984d831cb16c16f994945a197550897"));

        BitcoinBlock block3 = new BitcoinBlock(536870912,
                                    Sha256Hash.wrap("00000000000000000001163c9e1130c26984d831cb16c16f994945a197550897"),
                                    Sha256Hash.wrap("2dfad61070eeea30ee035cc58ac20a325292802f9445851d14f23b4e71ddee61"),1572337243, 0x17148edf, 2111493782);
        Assert.assertEquals(block3.getHash(),
                            Sha256Hash.wrap("0000000000000000000e008052ab86a7b0c20e46b29c54658b066d471022503f"));

        newBlock = new StoredBitcoinBlock(block, BigInteger.TEN, 0);
        newBlock2 = new StoredBitcoinBlock(block2, BigInteger.ONE, 0);
        newBlock3 = new StoredBitcoinBlock(block3, BigInteger.ZERO, 0);

        connection = ConnectionSelector.setConnectionInMemory();
        repo = new BitcoinBlockRepository(connection);
        repo.clear();
    }

    @After
    public void closeConnection() throws SQLException {
        if (connection != null)
            connection.close();
    }
    
    @Test
    public void getNonexistentBlockTest() throws SQLException, IOException {
        StoredBitcoinBlock block = repo.get(newBlock.getHash());
        Assert.assertNull(block);
    }

    @Test
    public void deleteNonexistentBlockTest() throws SQLException, IOException {
        repo.delete(newBlock.getHash());
    }

    @Test
    public void deleteBlockTest() throws SQLException, IOException {
        repo.save(newBlock);
        repo.delete(newBlock.getHash());

        StoredBitcoinBlock block = repo.get(newBlock.getHash());
        Assert.assertNull(block);
    }

    @Test
    public void addGetBlockTest() throws SQLException, IOException {
        repo.save(newBlock);
        StoredBitcoinBlock block = repo.get(newBlock.getHash());
        Assert.assertEquals(block, newBlock);
    }

    @Test
    public void addGetEndsWithIdBlockTest() throws SQLException, IOException {
        repo.save(newBlock);
        List<StoredBitcoinBlock> blocks = repo.getEndsWithId(newBlock.getHash());
        Assert.assertFalse(blocks.isEmpty());
        Assert.assertEquals(blocks.get(0), newBlock);
    }

    @Test
    public void clearTest() throws SQLException, IOException {
        repo.save(newBlock);
        repo.save(newBlock2);
        repo.save(newBlock3);

        repo.clear();

        List<StoredBitcoinBlock> blocks = repo.getAll();
        Assert.assertTrue(blocks.isEmpty());
    }

    @Test
    public void getAllEmptyRepoTest() throws SQLException, IOException {
        List<StoredBitcoinBlock> blocks = repo.getAll();
        Assert.assertTrue(blocks.isEmpty());
    }

    @Test
    public void getAllTest() throws SQLException, IOException {
        Comparator<StoredBitcoinBlock> comparator = (b1, b2) -> b1.getHash().toString().compareTo(b2.getHash().toString());

        repo.save(newBlock);
        repo.save(newBlock2);
        repo.save(newBlock3);

        List<StoredBitcoinBlock> expectedBlocks = new ArrayList<StoredBitcoinBlock>();
        expectedBlocks.add(newBlock);
        expectedBlocks.add(newBlock2);
        expectedBlocks.add(newBlock3);
        expectedBlocks.sort(comparator);

        List<StoredBitcoinBlock> blocks = repo.getAll();
        blocks.sort(comparator);

        Assert.assertEquals(blocks, expectedBlocks);
    }

    @Test
    public void isInUseTest() throws SQLException, IOException {
        repo.save(newBlock2);
        repo.save(newBlock3);

        Assert.assertFalse(repo.isInUse(newBlock3.getHash()));
        Assert.assertTrue(repo.isInUse(newBlock.getHash()));
        Assert.assertTrue(repo.isInUse(newBlock2.getHash()));

    }

    //NOTE: can't test getEndsWithId() with a trimmed hash, as there's no trimming support in Sha256Hash
}
