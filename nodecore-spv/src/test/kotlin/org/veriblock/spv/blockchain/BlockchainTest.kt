package org.veriblock.spv.blockchain

import org.junit.Assert
import org.junit.Test
import org.veriblock.core.Context
import org.veriblock.core.miner.vbkBlockGenerator
import org.veriblock.core.params.getDefaultNetworkParameters
import org.veriblock.sdk.models.VeriBlockBlock
import org.veriblock.spv.service.BlockStore
import org.veriblock.spv.service.Blockchain

class BlockchainTest {
    val regtest = getDefaultNetworkParameters("regtest")

    init {
        Context.create(regtest)
    }

    val tmpDir = createTempDir()
    val blockStore = BlockStore(regtest, tmpDir)
    val blockchain = Blockchain(regtest, blockStore)

    fun generateBlock(prev: VeriBlockBlock) = vbkBlockGenerator(prev, regtest) {
        // return previous block header
        blockStore.readBlock(it.previousBlock)?.header
    }

    @Test
    fun emptyBlockchainHasGenesis() {
        // upon start there must be at least 1 block - genesis block
        Assert.assertEquals(blockchain.activeChain.tip().header, regtest.genesisBlock)
        Assert.assertEquals(blockchain.getBlockByHeight(0)!!.header, regtest.genesisBlock)
        Assert.assertEquals(blockchain.getChainHead().header, regtest.genesisBlock)
    }

    @Test
    fun `mine single chain`() {
        var lastBlock: VeriBlockBlock? = null
        generateBlock(blockchain.activeChain.tip().header).take(2100).forEach {
            lastBlock = it
            val isValid = blockchain.acceptBlock(it)
            Assert.assertTrue(isValid)
        }

        Assert.assertEquals(blockchain.size, 2100 + 1 /*genesis*/)
        Assert.assertEquals(blockchain.activeChain.tip().header, lastBlock!!)
        Assert.assertEquals(blockStore.getTip().header, lastBlock!!)
        // since we store last 2001 blocks, block 2000 will be in activeChain
        Assert.assertNotNull(blockchain.activeChain.get(2000))
        // and block 5 will not be in activeChain
        Assert.assertNull(blockchain.activeChain.get(5))
    }

    @Test
    fun `reorg with fork block LESS than 2000 blocks behind`() {
        var lastBlock: VeriBlockBlock? = null
        // initially, have 2100 blocks
        generateBlock(blockchain.activeChain.tip().header)
            .take(2100)
            .forEach {
                lastBlock = it
                val isValid = blockchain.acceptBlock(it)
                Assert.assertTrue(isValid)
            }

        // total size - elements stored in active chain + genesis
        Assert.assertEquals(2100 - 2001 + 1, blockchain.activeChain.first().height)

        val tipA = blockchain.activeChain.tip()
        Assert.assertEquals(tipA.height, 2100)
        Assert.assertEquals(tipA.header, lastBlock)

        // starting from height 100, generate 2001 blocks. Chain B should win, as it has
        // higher chainwork
        generateBlock(blockchain.activeChain.first().header)
            .take(2001)
            .forEach {
                lastBlock = it
                val isValid = blockchain.acceptBlock(it)
                Assert.assertTrue(isValid)
            }

        val tipB = blockStore.readBlock(lastBlock!!.hash)!!

        Assert.assertEquals(/*genesis=*/1 + 2100 + 2001, blockchain.size)
        Assert.assertEquals(tipB.header, blockchain.activeChain.tip().header)
    }

    @Test
    fun `reorg with fork block MORE than 2000 blocks behind`() {
        var lastBlock: VeriBlockBlock? = null
        // initially, have 2100 blocks
        generateBlock(blockchain.activeChain.tip().header)
            .take(2100)
            .forEach {
                lastBlock = it
                val isValid = blockchain.acceptBlock(it)
                Assert.assertTrue(isValid)
            }

        // total size - elements stored in active chain + genesis
        Assert.assertEquals(2100 - 2001 + 1, blockchain.activeChain.first().height)

        val tipA = blockchain.activeChain.tip()
        Assert.assertEquals(tipA.height, 2100)
        Assert.assertEquals(tipA.header, lastBlock)

        // starting from height 99, generate 2001 blocks. Chain B should win, as it has
        // higher chainwork
        val blockBeforeFirst = blockStore.readBlock(blockchain.activeChain.first().header.previousBlock)!!
        generateBlock(blockBeforeFirst.header)
            .take(2500) // mine significantly larger chain
            .forEach {
                lastBlock = it
                val isValid = blockchain.acceptBlock(it)
                Assert.assertTrue(isValid)
            }

        Assert.assertEquals(/*genesis=*/1 + 2100 + 2500, blockchain.size)

        // tipA is still a winner
        Assert.assertEquals(tipA.header, blockchain.activeChain.tip().header)
    }
}
