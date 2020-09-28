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
import org.veriblock.sdk.blockchain.store.VeriBlockStore
import org.veriblock.core.crypto.VBlakeHash
import org.veriblock.sdk.models.VeriBlockBlock
import java.math.BigInteger
import java.sql.SQLException
import java.util.ArrayList
import java.util.HashMap
import java.util.HashSet
import kotlin.math.pow

private val logger = createLogger {}

class Blockchain(
    private val genesisBlock: VeriBlockBlock,
    val blockStore: VeriBlockStore
) {
    //TODO SPV-124
    private val blocksCache = EvictingQueue.create<StoredVeriBlockBlock>(1000)

    fun getChainHead(): VeriBlockBlock {
        try {
            return blockStore.getChainHead()!!.block
        } catch (e: SQLException) {
            throw IllegalStateException("Unable to get chain head", e)
        }
    }

    @Throws(SQLException::class)
    fun get(hash: VBlakeHash): StoredVeriBlockBlock? {
        return blockStore.get(hash)
    }

    @Throws(SQLException::class)
    fun add(block: VeriBlockBlock) {
        val previous = blockStore.get(block.previousBlock)
            ?: // Nothing to build on
            return
        val storedBlock = StoredVeriBlockBlock(
            block, previous.work.add(BitcoinUtilities.decodeCompactBits(block.difficulty.toLong()))
        )

        // TODO: Make the put(...) and setChainHead(...) atomic
        blockStore.put(storedBlock)
        blocksCache.add(storedBlock)

        // TODO: PoP fork resolution additional
        if (storedBlock.work.compareTo(blockStore.getChainHead()!!.work) > 0) {
            blockStore.setChainHead(storedBlock)
        }

        // TODO: Broadcast events: new best block, reorganize, new block
    }

    @Throws(SQLException::class)
    fun addAll(blocks: List<VeriBlockBlock>) {
        val sortedBlock = blocks.sortedBy { it.height }
        logger.debug { "Adding ${sortedBlock.size} blocks, height ${sortedBlock.first().height} - ${sortedBlock.last().height}" }
        if (!areBlocksSequential(sortedBlock)) {
            // todo throw Exception
            return
        }
        val listToStore = listToStore(sortedBlock)
        if (listToStore.isEmpty()) {
            // todo throw Exception
            // Nothing to build on
            return
        }
        val storedBlocks = convertToStoreVeriBlocks(listToStore)
        blockStore.put(storedBlocks)
        blocksCache.addAll(storedBlocks)

        // TODO: PoP fork resolution additional
        if (storedBlocks[storedBlocks.size - 1].work > blockStore.getChainHead()!!.work) {
            blockStore.setChainHead(storedBlocks[storedBlocks.size - 1])
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
            val previous = blockStore.get(veriBlockBlocks[i].previousBlock)
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
        val commonBlock = blockStore.get(veriBlockBlocks[0].previousBlock)!!
        blockWorks[commonBlock.hash.toString().substring(24)] = commonBlock.work
        for (veriBlockBlock in veriBlockBlocks) {
            var work = blockWorks[veriBlockBlock.previousBlock.toString()]
            //This block is from fork, our Blockchain doesn't have this previousBlock.
            if (work == null) {
                val storedVeriBlockBlock = blockStore.get(veriBlockBlock.previousBlock)
                    ?: //There is no such block.
                    continue
                work = storedVeriBlockBlock.work
            }
            val workOfCurrentBlock = work.add(BitcoinUtilities.decodeCompactBits(veriBlockBlock.difficulty.toLong()))
            blockWorks[veriBlockBlock.hash.toString().substring(24)] = workOfCurrentBlock
            val block = StoredVeriBlockBlock(
                veriBlockBlock, workOfCurrentBlock
            )
            storedBlocks.add(block)
        }
        return storedBlocks
    }

    fun getPeerQuery(): List<VeriBlockBlock> {
        val blocks: MutableList<VeriBlockBlock> = ArrayList(16)
        try {
            var cursor = blockStore.getChainHead()
                ?: return listOf(genesisBlock)

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
                    cursor = blockStore.get(cursor.block.previousBlock)
                        ?: break@outer
                }
                blocks.add(cursor.block)
            }
        } catch (e: Exception) {
            logger.error("Unable to build peer query", e)
        }
        blocks.add(genesisBlock)
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
