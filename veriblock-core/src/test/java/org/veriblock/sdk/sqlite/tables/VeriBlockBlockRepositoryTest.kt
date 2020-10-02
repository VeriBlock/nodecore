// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.sdk.sqlite.tables

import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.veriblock.core.crypto.Sha256Hash
import org.veriblock.core.utilities.Utility
import org.veriblock.sdk.blockchain.store.StoredVeriBlockBlock
import org.veriblock.sdk.services.SerializeDeserializeService
import org.veriblock.sdk.sqlite.ConnectionSelector
import java.io.IOException
import java.math.BigInteger
import java.sql.Connection
import java.sql.SQLException
import java.util.ArrayList
import java.util.Comparator

class VeriBlockBlockRepositoryTest {
    private lateinit var newBlock: StoredVeriBlockBlock
    private lateinit var newBlock2: StoredVeriBlockBlock
    private lateinit var newBlock3: StoredVeriBlockBlock
    private lateinit var connection: Connection
    private lateinit var repo: VeriBlockBlockRepository
    
    @Before
    @Throws(SQLException::class)
    fun init() {
        val raw = Utility.hexToBytes(
            "0001998300029690ACA425987B8B529BEC04654A16FCCE708F3F0DEED25E1D2513D05A3B17C49D8B3BCFEFC10CB2E9C4D473B2E25DB7F1BD040098960DE0E313"
        )
        val block = SerializeDeserializeService.parseVeriBlockBlock(raw)
        newBlock = StoredVeriBlockBlock(
            block, BigInteger.TEN, block.hash
        )
        val raw2 = Utility.hexToBytes(
            "000199840002A69BF9FE9B06E641B61699A9654A16FCCE708F3F0DEED25E1D2513D05A3B7D7F80EB5E94D01C6B3796DDE5647F135DB7F1DD040098960EA12045"
        )
        val block2 = SerializeDeserializeService.parseVeriBlockBlock(raw2)
        newBlock2 = StoredVeriBlockBlock(
            block2, BigInteger.ONE, block2.hash
        )
        val raw3 = Utility.hexToBytes(
            "000199850002461DB458CD6258D3571D4A2A654A16FCCE708F3F0DEED25E1D2513D05A3BB0B8A658CBFFCFBE9185AFDE789841EC5DB7F2360400989610B1662B"
        )
        val block3 = SerializeDeserializeService.parseVeriBlockBlock(raw3)
        newBlock3 = StoredVeriBlockBlock(
            block3, BigInteger.ZERO, block3.hash
        )
        connection = ConnectionSelector.setConnectionInMemory()
        repo = VeriBlockBlockRepository(connection)
        repo.clear()
    }

    @After
    @Throws(SQLException::class)
    fun closeConnection() {
        connection.close()
    }

    @Throws(SQLException::class, IOException::class)
    @Test
    fun getEndsWithIdNonexistentBlockTest() {
        val blocks = repo.getEndsWithId(newBlock.hash)
        Assert.assertTrue(blocks.isEmpty())
    }

    @Throws(SQLException::class, IOException::class)
    @Test
    fun getNonexistentBlockTest() {
        val block = repo[newBlock.hash]
        Assert.assertEquals(block, null)
    }

    @Test
    @Throws(SQLException::class, IOException::class)
    fun deleteNonexistentBlockTest() {
        repo.delete(newBlock.hash)
    }

    @Test
    @Throws(SQLException::class, IOException::class)
    fun deleteBlockTest() {
        repo.save(newBlock)
        repo.delete(newBlock.hash)
        val blocks = repo.getEndsWithId(newBlock.hash)
        Assert.assertTrue(blocks.isEmpty())
    }

    @Test
    @Throws(SQLException::class, IOException::class)
    fun addGetBlockTest() {
        repo.save(newBlock)
        val block = repo[newBlock.hash]
        Assert.assertEquals(block, newBlock)
    }

    @Test
    @Throws(SQLException::class, IOException::class)
    fun addGetEndsWithIdBlockTest() {
        repo.save(newBlock)
        val blocks = repo.getEndsWithId(newBlock.hash)
        Assert.assertFalse(blocks.isEmpty())
        Assert.assertEquals(blocks[0], newBlock)
    }

    @Test
    @Throws(SQLException::class, IOException::class)
    fun addBlockWithBlockOfProofTest() {
        val blockOfProof = Sha256Hash.wrap(
            "00000000000000b345b7bbf29bda1507a679b97967f99a10ab0088899529def7"
        )
        newBlock.setBlockOfProof(blockOfProof)
        repo.save(newBlock)
        val blocks = repo.getEndsWithId(newBlock.hash)
        Assert.assertFalse(blocks.isEmpty())
        Assert.assertEquals(blocks[0], newBlock)
    }

    @Test
    @Throws(SQLException::class, IOException::class)
    fun clearTest() {
        repo.save(newBlock)
        repo.save(newBlock2)
        repo.save(newBlock3)
        repo.clear()
        val blocks = repo.all
        Assert.assertTrue(blocks.isEmpty())
    }

    @Throws(SQLException::class, IOException::class)
    @Test
    fun getAllEmptyRepoTest() {
        val blocks = repo.all
        Assert.assertTrue(blocks.isEmpty())
    }

    @Throws(SQLException::class, IOException::class)
    @Test
    fun getAllTest() {
        repo.save(newBlock)
        repo.save(newBlock2)
        repo.save(newBlock3)
        val expectedBlocks: MutableList<StoredVeriBlockBlock> = ArrayList()
        expectedBlocks.add(newBlock)
        expectedBlocks.add(newBlock2)
        expectedBlocks.add(newBlock3)
        expectedBlocks.sortBy { it.hash.toString() }
        val blocks = repo.all
        blocks.sortBy { it.hash.toString() }
        Assert.assertEquals(blocks, expectedBlocks)
    }

    @Test
    @Throws(SQLException::class, IOException::class)
    fun addGetBlockTrimmedTest() {
        repo.save(newBlock)
        var blocks = repo.getEndsWithId(
            newBlock.hash.trimToPreviousBlockSize()
        )
        Assert.assertFalse(blocks.isEmpty())
        Assert.assertEquals(blocks[0], newBlock)
        blocks = repo.getEndsWithId(newBlock.hash.trimToPreviousKeystoneSize())
        Assert.assertFalse(blocks.isEmpty())
        Assert.assertEquals(blocks[0], newBlock)
    } //FIXME: fix and test IsInUse()
}
