package org.veriblock.spv.blockchain

import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Test
import org.veriblock.core.Context
import org.veriblock.core.crypto.PreviousBlockVbkHash
import org.veriblock.core.miner.vbkBlockGenerator
import org.veriblock.core.params.getDefaultNetworkParameters
import org.veriblock.sdk.models.VeriBlockBlock
import org.veriblock.spv.model.StoredVeriBlockBlock
import org.veriblock.spv.service.BlockStore
import org.veriblock.spv.service.Blockchain
import java.math.BigInteger

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
        blockchain.getBlock(it.previousBlock)?.header
    }

    @Test
    fun emptyBlockchainHasGenesis() {
        // upon start there must be at least 1 block - genesis block
        blockchain.size shouldBe 1
        blockchain.activeChain.tip.smallHash shouldBe regtest.genesisBlock.hash.trimToPreviousBlockSize()
        blockchain.getBlock(0)?.header shouldBe regtest.genesisBlock
        blockchain.getBlock(blockchain.activeChain.tip.smallHash)?.header shouldBe regtest.genesisBlock
    }

    @Test
    fun `mine single chain`() {
        val lastBlock = generateBlock(blockchain.getChainHeadBlock().header).take(2100).onEach {
            blockchain.acceptBlock(it) shouldBe true
        }.last()


        blockchain.size shouldBe 2100 + 1 /*genesis*/
        blockchain.getChainHeadBlock().header shouldBe lastBlock

        // blocks are accessible by height in O(1)
        (0..2100).forEach {
            val index = blockchain.activeChain[it]!!
            index.height shouldBe it
        }
    }

    @Test
    fun `reorg of size 1100`() {
        // initially, have 2100 blocks
        val lastBlock1 = generateBlock(blockchain.getChainHeadBlock().header)
            .take(2100)
            .onEach {
                blockchain.acceptBlock(it) shouldBe true
            }
            .last()

        blockchain.activeChain.first.height shouldBe 0
        blockchain.activeChain.tip.height shouldBe 2100

        val tipA = blockchain.activeChain.tip
        tipA.height shouldBe 2100
        tipA.smallHash shouldBe lastBlock1.hash.trimToPreviousBlockSize()

        // starting from height 1000, generate 2001 blocks. Chain B should win, as it has
        // higher chainwork
        val lastBlock2 = generateBlock(blockchain.activeChain[1000]!!.readBlock(blockStore)!!.header)
            .take(2001)
            .onEach { blockchain.acceptBlock(it) shouldBe true }
            .last()

        val tipB = blockchain.getBlock(lastBlock2.hash)!!

        blockchain.size shouldBe /*genesis=*/1 + 2100 + 2001
        blockchain.activeChain.tip.readBlock(blockStore)!!.header shouldBe tipB.header
    }

    @Test
    fun `during blockchain loading, invalid block found`() {
        val chainGen = generateBlock(regtest.genesisBlock)
            .take(100)
            .onEach { blockchain.acceptBlock(it) shouldBe true }
            .toList()
        chainGen.size shouldBe 100
        blockchain.activeChain.tip.height shouldBe 100

        val headerGenerated = chainGen.take(1).last()
        val headerCorrupted = VeriBlockBlock(
            headerGenerated.height,
            headerGenerated.version,
            PreviousBlockVbkHash.EMPTY_HASH,
            headerGenerated.previousKeystone,
            headerGenerated.secondPreviousKeystone,
            headerGenerated.merkleRoot,
            headerGenerated.timestamp,
            headerGenerated.difficulty,
            headerGenerated.nonce
        )

        val positionAfterLastValidBlock = blockStore.appendBlock(
            StoredVeriBlockBlock(
                header = headerCorrupted,
                work = BigInteger.valueOf(999999999L),
                hash = headerCorrupted.hash
            )
        )

        val store = BlockStore(regtest, tmpdir)
        val bchain2 = Blockchain(store)

        bchain2.getBlock(headerCorrupted.hash) shouldBe null
        blockStore.size shouldBe positionAfterLastValidBlock
        bchain2.size shouldBe 101
    }
}
