package org.veriblock.spv.blockchain

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.*
import org.veriblock.core.Context
import org.veriblock.core.miner.vbkBlockGenerator
import org.veriblock.core.params.getDefaultNetworkParameters
import org.veriblock.sdk.models.VeriBlockBlock
import org.veriblock.spv.service.BlockStore
import org.veriblock.spv.service.Blockchain

class BlockchainTest {
    val regtest = getDefaultNetworkParameters("regtest")
    val tmpdir = createTempDir()

    init {
        Context.create(regtest)
    }

    @After
    fun after() {
        tmpdir.deleteRecursively()
    }

    val blockStore = BlockStore(regtest, tmpdir)
    val blockchain = Blockchain(blockStore)

    fun generateBlock(prev: VeriBlockBlock) = vbkBlockGenerator(prev, regtest) {
        // return previous block header
        blockStore.readBlock(it.previousBlock)?.header
    }

    @Test
    fun emptyBlockchainHasGenesis() {
        // upon start there must be at least 1 block - genesis block
        blockchain.size shouldBe 1
        blockchain.activeChain.tip.smallHash shouldBe regtest.genesisBlock.hash.trimToPreviousBlockSize()
        blockchain.getBlock(blockchain.activeChain.tip.smallHash)!!.header shouldBe regtest.genesisBlock
    }

    @Test
    fun `mine single chain`() {
        var lastBlock: VeriBlockBlock? = null
        val lastBlock = generateBlock(blockchain.getChainHeadBlock().header).take(2100).onEach {
          blockchain.acceptBlock(it) shouldBe true
        }.last()
            lastBlock = it
            val isValid = blockchain.acceptBlock(it)
            Assert.assertTrue(isValid)
        }

        blockchain.size shouldBe 2100 + 1 /*genesis*/
        blockStore.getChainHeadBlock().header shouldBe lastBlock!!

        // blocks are accessible by height in O(1)
        (0..2100).forEach {
            val index = blockchain.activeChain.get(it)!!
            index.height shouldBe it
        }
    }

    @Test
    fun `reorg with fork block LESS than 2000 blocks behind`() {
        var lastBlock: VeriBlockBlock? = null
        // initially, have 2100 blocks
        generateBlock(blockchain.getChainHeadBlock().header)
            .take(2100)
            .forEach {
                lastBlock = it
                val isValid = blockchain.acceptBlock(it)
                Assert.assertTrue(isValid)
            }

        blockchain.activeChain.first.height shouldBe 0
        blockchain.activeChain.tip.height shouldBe 2100

        val tipA = blockchain.activeChain.tip
        tipA.height shouldBe 2100
        tipA.smallHash shouldBe lastBlock!!.hash.trimToPreviousBlockSize()

        // starting from height 1000, generate 2001 blocks. Chain B should win, as it has
        // higher chainwork
        generateBlock(blockchain.activeChain[1000]!!.readBlock(blockStore)!!.header)
            .take(2001)
            .forEach {
                lastBlock = it
                val isValid = blockchain.acceptBlock(it)
                Assert.assertTrue(isValid)
            }

        val tipB = blockStore.readBlock(lastBlock!!.hash)!!

        blockchain.size shouldBe /*genesis=*/1 + 2100 + 2001
        blockchain.activeChain.tip.readBlock(blockStore)!!.header shouldBe tipB.header
    }
}
