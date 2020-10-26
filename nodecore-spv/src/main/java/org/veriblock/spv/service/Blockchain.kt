// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.spv.service

import org.veriblock.core.bitcoinj.BitcoinUtilities
import org.veriblock.core.crypto.AnyVbkHash
import org.veriblock.core.crypto.PreviousBlockVbkHash
import org.veriblock.core.miner.getMiningContext
import org.veriblock.core.miner.getNextWorkRequired
import org.veriblock.core.utilities.createLogger
import org.veriblock.sdk.blockchain.store.StoredVeriBlockBlock
import org.veriblock.sdk.models.Constants
import org.veriblock.sdk.models.VeriBlockBlock
import org.veriblock.sdk.services.ValidationService
import org.veriblock.spv.util.SpvEventBus
import java.util.concurrent.ConcurrentHashMap

private val logger = createLogger {}

class Blockchain(
    val blockStore: BlockStore
) {
    // in-memory block index
    val blockIndex = ConcurrentHashMap<PreviousBlockVbkHash, BlockIndex>()
    lateinit var activeChain: Chain
    val size get() = blockIndex.size
    val networkParameters get() = blockStore.networkParameters

    init {
        reindex()
    }

    /**
     * Drop existing block index, and re-generate one from disk.
     */
    fun reindex() {
        logger.info { "Reading $${blockStore.networkParameters} blocks..." }

        // drop previous block index
        blockIndex.clear()
        writeGenesisBlock(blockStore.networkParameters.genesisBlock)

        // reads blocks file and builds block index
        blockStore.forEach { position, block ->
            // we were able to read a block from blocks file.
            // this block must connect to previous block, i.e.
            // previous block must exist in index

            val prevHash = block.header.previousBlock
            val prevIndex = blockIndex[prevHash]
            // ignore genesis block
            if (block.height != 0 && prevIndex == null) {
                logger.warn { "Found block that does not connect to blockchain: height=${block.height} hash=${block.hash} " }
                return@forEach false
            }

            // validate block
            if (!ValidationService.checkBlock(block.header)) {
                return@forEach false
            }

            // prev index exists! this is valid block
            val index = appendToBlockIndex(position, block)

            // update active chain
            if (activeChain.tipWork < block.work) {
                activeChain.setTip(index, block.work)
            }

            true
        }

        logger.info { "Successfully initialized Blockchain with ${blockIndex.size} blocks" }
    }


    /**
     * Getter for block index.
     */
    fun getBlockIndex(hash: AnyVbkHash): BlockIndex? = blockIndex[hash.trimToPreviousBlockSize()]
    fun getBlockIndex(height: Int): BlockIndex? = activeChain[height]

    /**
     * Getter for tip.
     */
    fun getChainHeadBlock(): StoredVeriBlockBlock = blockStore.readBlock(activeChain.tip.position)!!
    fun getChainHeadIndex(): BlockIndex = activeChain.tip

    /**
     * Reads block with given hash.
     */
    fun getBlock(hash: AnyVbkHash): StoredVeriBlockBlock? {
        val smallHash = hash.trimToPreviousBlockSize()
        val index = blockIndex[smallHash] ?: return null
        return blockStore.readBlock(index.position)
    }

    /**
     * Reads block with given height on active chain.
     */
    fun getBlock(height: Int): StoredVeriBlockBlock? {
        val index = activeChain[height] ?: return null
        return blockStore.readBlock(index.position)
    }

    /**
     * Reads a chain of blocks from active chain of given `size` ending with `hash`
     */
    fun getChainWithTip(hash: AnyVbkHash, size: Int): ArrayList<StoredVeriBlockBlock> {
        val ret = ArrayList<StoredVeriBlockBlock>()
        var i = 0
        var cursor = getBlock(hash)
        while (cursor != null && i++ < size) {
            ret.add(cursor)
            cursor = getBlock(cursor.header.previousBlock)
        }
        ret.reverse()
        return ret
    }

    /**
     * Adds new block to Blockchain.
     * @return true if block is valid, false otherwise
     */
    fun acceptBlock(
        block: VeriBlockBlock
    ): Boolean {
        if (getBlockIndex(block.hash) != null) {
            // block is valid, we already have it
            return true
        }

        // does block connect to blockchain?
        val prevIndex = getBlockIndex(block.previousBlock)
            ?: return false

        val prev = prevIndex.readBlock(blockStore)
            ?: throw IllegalStateException("Found index with hash=${block.previousBlock} but could not read its block")

        // is block statelessly valid?
        if (!ValidationService.checkBlock(block)) {
            return false
        }

        val ctx = getMiningContext(prev.header) {
            getBlock(it.previousBlock)?.header
        }

        val expectedDifficulty = getNextWorkRequired(prev.header, networkParameters, ctx)
        if (expectedDifficulty != block.difficulty) {
            // bad difficulty
            logger.warn { "Rejecting block=$block, because of bad difficulty. Expected=$expectedDifficulty, got=${block.difficulty}" }
            return false
        }

        val median = calculateMinimumTimestamp(block)
        if (block.timestamp < median) {
            // bad median time past
            logger.warn { "Rejecting block=$block, because of bad timestamp. Expected at least=$median, got=${block.timestamp}" }
            return false
        }

        // TODO: contextually check block: validate keystones

        // all ok, we can add block to blockchain
        val stored = StoredVeriBlockBlock(
            header = block,
            work = prev.work + BitcoinUtilities.decodeCompactBits(block.difficulty.toLong()),
            hash = block.hash
        )

        // write block on disk
        val position = blockStore.appendBlock(stored)
        val index = appendToBlockIndex(position, stored)

        // do fork resolution
        if (stored.work > activeChain.tipWork) {
            // new block wins
            activeChain.setTip(index, stored.work)
            SpvEventBus.newBestBlockEvent.trigger(block)
        }

        // add new block to a queue
        SpvEventBus.newBlockChannel.offer(block)

        return true
    }

    fun getPeerQuery(): List<VeriBlockBlock> {
        return getChainWithTip(activeChain.tip.smallHash, 100)
            .map { it.header }
            .filter { it.isKeystone() }
    }

    private fun appendToBlockIndex(position: Long, block: StoredVeriBlockBlock): BlockIndex {
        val smallHash = block.hash.trimToPreviousBlockSize()
        val prev = blockIndex[block.header.previousBlock]
        val index = BlockIndex(
            smallHash = smallHash,
            position = position,
            height = block.height,
            prev = prev
        )
        blockIndex[smallHash] = index
        return index
    }

    private fun writeGenesisBlock(genesis: VeriBlockBlock) {
        val smallHash = genesis.hash.trimToPreviousBlockSize()
        val param = blockStore.networkParameters
        val work = BitcoinUtilities.decodeCompactBits(
            param.genesisBlock.difficulty.toLong()
        )

        // write gb to position 0
        blockStore.writeBlock(
            position = 0,
            block = StoredVeriBlockBlock(
                header = param.genesisBlock,
                work = work,
                hash = param.genesisBlock.hash
            )
        )

        // block index always contains genesis block on start
        val index = BlockIndex(
            smallHash = smallHash,
            position = 0, // gb is at position 0
            height = 0,
            prev = null
        )

        blockIndex[smallHash] = index
        activeChain = Chain(index, work)
    }

    private fun getContextForBlock(block: VeriBlockBlock) =
        getChainWithTip(block.hash, Constants.POP_REWARD_PAYMENT_DELAY)

    private fun calculateMinimumTimestamp(blockchain: List<StoredVeriBlockBlock>): Int {
        val count =
            if (Constants.HISTORY_FOR_TIMESTAMP_AVERAGE > blockchain.size) blockchain.size
            else Constants.HISTORY_FOR_TIMESTAMP_AVERAGE

        // Calculate the MEDIAN. If there are an even number of elements,
        // use the lower of the two.
        return blockchain.asSequence()
            .sortedByDescending { it.height }
            .take(count)
            .map { it.header.timestamp }
            .sorted()
            .drop(if (count % 2 == 0) (count / 2) - 1 else count / 2)
            .first()
    }

    private fun calculateMinimumTimestampLegacy(blockchain: List<StoredVeriBlockBlock>): Int {
        val count =
            if (Constants.HISTORY_FOR_TIMESTAMP_AVERAGE > blockchain.size) blockchain.size
            else Constants.HISTORY_FOR_TIMESTAMP_AVERAGE

        // Calculate the MEDIAN. If there are an even number of elements,
        // use the lower of the two.
        return blockchain.asSequence()
            .drop(blockchain.size - count)
            .map { it.header.timestamp }
            .sorted()
            .drop(if (count % 2 == 0) (count / 2) - 1 else count / 2)
            .first()
    }

    fun calculateMinimumTimestamp(blockToAdd: VeriBlockBlock): Int {
        val context = getContextForBlock(blockToAdd)
        return if(blockToAdd.height >= Constants.MINIMUM_TIMESTAMP_ONSET_BLOCK_HEIGHT) {
            calculateMinimumTimestamp(context)
        } else {
            calculateMinimumTimestampLegacy(context)
        }
    }
}
