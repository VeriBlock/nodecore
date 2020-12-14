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
import org.veriblock.core.miner.getNextWorkRequired
import org.veriblock.core.utilities.createLogger
import org.veriblock.sdk.blockchain.VeriBlockDifficultyCalculator
import org.veriblock.sdk.models.Constants
import org.veriblock.sdk.models.VeriBlockBlock
import org.veriblock.sdk.models.VerificationException
import org.veriblock.sdk.services.ValidationService
import org.veriblock.spv.model.StoredVeriBlockBlock
import org.veriblock.spv.util.SpvEventBus
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

private val logger = createLogger {}

class Blockchain(
    val blockStore: BlockStore
) {
    // in-memory block index
    val blockIndex = ConcurrentHashMap<PreviousBlockVbkHash, BlockIndex>()
    lateinit var activeChain: Chain
    val size get() = blockIndex.size
    val networkParameters get() = blockStore.networkParameters
    val lock = ReentrantLock()

    init {
        reindex()
    }

    /**
     * Drop existing block index, and re-generate one from disk.
     */
    private fun reindex() {
        logger.info { "Reading ${blockStore.networkParameters} blocks..." }

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
            try {
                ValidationService.verify(block.header)
            } catch (e: Exception) {
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
    private fun getChainWithTip(hash: AnyVbkHash, size: Int): List<StoredVeriBlockBlock> {
        val ret = ArrayList<StoredVeriBlockBlock>()
        var i = 0
        var cursor = getBlock(hash)
        while (cursor != null && i++ < size) {
            ret.add(0, cursor)
            cursor = getBlock(cursor.header.previousBlock)
        }
        return ret
    }

    /**
     * Adds new block to Blockchain.
     * @return true if block is valid, false otherwise
     */
    fun acceptBlock(
        block: VeriBlockBlock
    ): Boolean = lock.withLock {
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
        try {
            ValidationService.verify(block)
        } catch (e: VerificationException) {
            logger.warn { "Rejecting block $block: ${e.message}" }
            return false
        }

        val ctxValidation = getValidationContextForBlock(block)
        val expectedDifficulty = getNextWorkRequired(prev.header, networkParameters, ctxValidation)
        if (expectedDifficulty != block.difficulty) {
            // bad difficulty
            logger.warn { "Rejecting block=$block, because of bad difficulty. Expected=$expectedDifficulty, got=${block.difficulty}" }
            return false
        }

        val median = calculateMinimumTimestamp(block, ctxValidation)
        if (block.timestamp < median) {
            // bad median time past
            logger.warn { "Rejecting block=$block, because of bad timestamp. Expected at least=$median, got=${block.timestamp}" }
            return false
        }

        if (!validateFit(block, ctxValidation)) {
            logger.warn { "Rejecting block=$block, because of bad keystones (${block.previousKeystone}, ${block.secondPreviousKeystone})" }
            return false
        }

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
        SpvEventBus.newBlockFlow.tryEmit(block)

        return true
    }

    fun isOnActiveChain(hash: AnyVbkHash): Boolean {
        val index = getBlockIndex(hash)
            ?: return false
        return activeChain.contains(index)
    }

    fun getPeerQuery(): List<VeriBlockBlock> {
        return getChainWithTip(activeChain.tip.smallHash, 100).asSequence()
            .map { it.header }
            .filter { it.isKeystone() }
            .toList()
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

    private fun getValidationContextForBlock(block: VeriBlockBlock): List<VeriBlockBlock> {
        // prepare enough context for difficulty, timestamp and keystones validation
        val legacyContextSize = if (block.height >= Constants.MINIMUM_TIMESTAMP_ONSET_BLOCK_HEIGHT) {
            Constants.HISTORY_FOR_TIMESTAMP_AVERAGE
        } else {
            Constants.POP_REWARD_PAYMENT_DELAY
        }
        val contextSize = maxOf(
            VeriBlockDifficultyCalculator.RETARGET_PERIOD,
            legacyContextSize,
            Constants.KEYSTONE_INTERVAL * 3
        )
        return getChainWithTip(block.previousBlock, contextSize)
            .map { it.header }
            .reversed()
    }

    private fun calculateMinimumTimestamp(validationContext: List<VeriBlockBlock>): Int {
        val count = Constants.HISTORY_FOR_TIMESTAMP_AVERAGE.coerceAtMost(validationContext.size)

        // Calculate the MEDIAN. If there are an even number of elements,
        // use the lower of the two.
        return validationContext.asSequence()
            .sortedByDescending { it.height }
            .take(count)
            .map { it.timestamp }
            .sorted()
            .elementAt(count / 2 + count % 2 - 1)
    }

    private fun calculateMinimumTimestampLegacy(validationContext: List<VeriBlockBlock>): Int {
        val count = Constants.HISTORY_FOR_TIMESTAMP_AVERAGE.coerceAtMost(validationContext.size)

        // Calculate the MEDIAN. If there are an even number of elements,
        // use the lower of the two.
        // Expects validationContext to be in reversed order (descending block height)
        return validationContext.asSequence()
            .drop(validationContext.size - count)
            .map { it.timestamp }
            .sorted()
            .elementAt(count / 2 + count % 2 - 1)
    }

    private fun calculateMinimumTimestamp(blockToAdd: VeriBlockBlock, validationContext: List<VeriBlockBlock>): Int {
        require(validationContext.isNotEmpty()) {
            "No previous blocks for median time calculation found for $blockToAdd"
        }
        return if (blockToAdd.height >= Constants.MINIMUM_TIMESTAMP_ONSET_BLOCK_HEIGHT) {
            calculateMinimumTimestamp(validationContext)
        } else {
            calculateMinimumTimestampLegacy(validationContext)
        }
    }

    private fun validateFit(blockToAdd: VeriBlockBlock, validationContext: List<VeriBlockBlock>): Boolean {
        if (validationContext.isEmpty()) {
            logger.warn { "Block ($blockToAdd) builds upon an invalid tree!" }
            return false
        }
        val bestBlock = validationContext.first()
        if (bestBlock.height + 1 != blockToAdd.height || bestBlock.hash.trimToPreviousBlockSize() != blockToAdd.previousBlock) {
            logger.warn { "Block ($blockToAdd) builds upon an invalid tree!" }
            return false
        }

        val remainderOverKeystone = blockToAdd.height % Constants.KEYSTONE_INTERVAL
        val (indexOfSecondPreviousBlock, indexOfThirdPreviousBlock) = when (remainderOverKeystone) {
            0 -> listOf(Constants.KEYSTONE_INTERVAL - 1, Constants.KEYSTONE_INTERVAL * 2 - 1)
            1 -> listOf(Constants.KEYSTONE_INTERVAL, Constants.KEYSTONE_INTERVAL * 2)
            else -> listOf(remainderOverKeystone - 1, remainderOverKeystone - 1 + Constants.KEYSTONE_INTERVAL)
        }

        if (indexOfSecondPreviousBlock < validationContext.size) {
            val secondPreviousBlock = validationContext[indexOfSecondPreviousBlock]
            val calculatedToAddBlockNum = when (remainderOverKeystone) {
                0 -> secondPreviousBlock.height + Constants.KEYSTONE_INTERVAL
                1 -> secondPreviousBlock.height + Constants.KEYSTONE_INTERVAL + 1
                else -> secondPreviousBlock.height + remainderOverKeystone
            }
            val hash = secondPreviousBlock.hash
            if (calculatedToAddBlockNum != blockToAdd.height || hash.trimToPreviousKeystoneSize() != blockToAdd.previousKeystone) {
                logger.warn { "Block ($blockToAdd) builds upon an invalid tree!" }
                logger.warn { "second previous block hash or index thereof (${blockToAdd.previousKeystone}) does not match $hash" }
                return false
            }
        }

        if (indexOfThirdPreviousBlock < validationContext.size) {
            val thirdPreviousBlock = validationContext[indexOfThirdPreviousBlock]
            val calculatedToAddBlockNum = when (remainderOverKeystone) {
                0 -> thirdPreviousBlock.height + Constants.KEYSTONE_INTERVAL * 2
                1 -> thirdPreviousBlock.height + Constants.KEYSTONE_INTERVAL * 2 + 1
                else -> thirdPreviousBlock.height + Constants.KEYSTONE_INTERVAL + remainderOverKeystone
            }
            val hash = thirdPreviousBlock.hash
            if (calculatedToAddBlockNum != blockToAdd.height || hash.trimToPreviousKeystoneSize() != blockToAdd.secondPreviousKeystone) {
                logger.warn { "Block ($blockToAdd) builds upon an invalid tree!" }
                logger.warn { "third previous block hash or index thereof (${blockToAdd.secondPreviousKeystone}) does not match $hash" }
                return false
            }
        }

        return true
    }
}
