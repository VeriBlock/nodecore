// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package veriblock.service

import com.google.common.collect.EvictingQueue
import org.veriblock.core.bitcoinj.BitcoinUtilities
import org.veriblock.core.utilities.createLogger
import org.veriblock.sdk.blockchain.store.StoredVeriBlockBlock
import org.veriblock.core.crypto.VBlakeHash
import org.veriblock.sdk.models.VeriBlockBlock
import veriblock.util.SpvEventBus
import java.math.BigInteger
import java.sql.SQLException
import java.util.ArrayList
import java.util.HashMap
import java.util.HashSet
import kotlin.jvm.Throws
import kotlin.math.pow

private val logger = createLogger {}

class Blockchain(
    private val genesisBlock: VeriBlockBlock,
    private val blockStore: BlockStore
) {
    //TODO SPV-124
    private val blocksCache = EvictingQueue.create<StoredVeriBlockBlock>(1000)

    fun getChainHead(): VeriBlockBlock {
        try {
            return blockStore.getTip().block
        } catch (e: SQLException) {
            throw IllegalStateException("Unable to get chain head", e)
        }
    }

    @Throws(SQLException::class)
    fun get(hash: VBlakeHash): StoredVeriBlockBlock? {
        return blockStore.readBlock(hash)
    }

    @Throws(SQLException::class)
    fun addAll(blocks: List<VeriBlockBlock>) {
        val sortedBlocks = blocks.sortedBy { it.height }
        logger.debug { "Adding ${sortedBlocks.size} blocks, height ${sortedBlocks.first().height} - ${sortedBlocks.last().height}" }
        if (!areBlocksSequential(sortedBlocks)) {
            // todo throw Exception
            return
        }
        val listToStore = listToStore(sortedBlocks)
        if (listToStore.isEmpty()) {
            // todo throw Exception
            // Nothing to build on
            return
        }
        val storedBlocks = convertToStoreVeriBlocks(listToStore)
        blockStore.writeBlocks(storedBlocks)
        blocksCache.addAll(storedBlocks)

        // TODO: PoP fork resolution additional
        val lastBlock = storedBlocks[storedBlocks.size - 1]
        if (lastBlock.work > blockStore.getTip().work) {
            // TODO: Verify they were stored
            blockStore.setTip(lastBlock)
            SpvEventBus.newBestBlockEvent.trigger(lastBlock.block)
        }

        for (block in blocks) {
            SpvEventBus.newBlockChannel.offer(block)
        }
    }

    fun getBlockByHeight(height: Int): StoredVeriBlockBlock? {
        return blocksCache.find {
            it.height == height
        }
    }

    @Throws(SQLException::class)
    private fun listToStore(veriBlockBlocks: List<VeriBlockBlock>): List<VeriBlockBlock> {
        for (i in veriBlockBlocks.indices) {
            val previous = blockStore.readBlock(veriBlockBlocks[i].previousBlock)
            if (previous != null) {
                return veriBlockBlocks.subList(i, veriBlockBlocks.size)
            }
        }
        return emptyList()
    }

    @Throws(SQLException::class)
    private fun convertToStoreVeriBlocks(veriBlockBlocks: List<VeriBlockBlock>): List<StoredVeriBlockBlock> {
        val blockWorks: MutableMap<String, BigInteger> = HashMap()
        val storedBlocks: MutableList<StoredVeriBlockBlock> = ArrayList()
        val commonBlock = blockStore.readBlock(veriBlockBlocks[0].previousBlock)!!
        blockWorks[commonBlock.hash.toString().substring(24)] = commonBlock.work
        for (veriBlockBlock in veriBlockBlocks) {
            var work = blockWorks[veriBlockBlock.previousBlock.toString()]
            //This block is from fork, our Blockchain doesn't have this previousBlock.
            if (work == null) {
                val storedVeriBlockBlock = blockStore.readBlock(veriBlockBlock.previousBlock)
                    ?: //There is no such block.
                    continue
                work = storedVeriBlockBlock.work
            }
            val workOfCurrentBlock = work.add(BitcoinUtilities.decodeCompactBits(veriBlockBlock.difficulty.toLong()))
            blockWorks[veriBlockBlock.hash.toString().substring(24)] = workOfCurrentBlock
            val block = StoredVeriBlockBlock(
                veriBlockBlock, workOfCurrentBlock, veriBlockBlock.hash
            )
            storedBlocks.add(block)
        }
        return storedBlocks
    }

    fun getPeerQuery(): List<VeriBlockBlock> {
        val blocks: MutableList<VeriBlockBlock> = ArrayList(16)
        blocks.add(genesisBlock)
        try {
            var cursor = blockStore.getTip()

            blocks.add(cursor.block)
            // TODO 16 is too much for bigDb with current approach. It takes a lot of time. Try to get amount of blocks by height and process in memory.
            // for (int i = 0; i < 16; i++) {
            outer@
            for (i in 0..4) {
                val seek = cursor.block.height - 2.0.pow(i.toDouble()).toInt()
                if (seek < 0) {
                    break
                }

                while (cursor.block.height != seek) {
                    cursor = blockStore.readBlock(cursor.block.previousBlock)
                        ?: break@outer
                }
                blocks.add(cursor.block)
            }
        } catch (e: Exception) {
            logger.error("Unable to build peer query", e)
        }
        return blocks
    }

    private fun remove(block: VeriBlockBlock) {
        try {
            // TODO: Update affected transactions
        } catch (e: Exception) {
            // TODO: Handle
        }
    }

    private fun areBlocksSequential(blocks: List<VeriBlockBlock>): Boolean {
        if (blocks.isEmpty()) {
            return false
        }
        val hashes: MutableSet<String> = HashSet()
        val firstHeight = blocks[0].height
        for (veriBlockBlock in blocks) {
            if (firstHeight < veriBlockBlock.height) {
                if (!hashes.contains(veriBlockBlock.previousBlock.toString())) {
                    return false
                }
            }
            hashes.add(veriBlockBlock.hash.toString().substring(24))
        }
        return true
    }
}
