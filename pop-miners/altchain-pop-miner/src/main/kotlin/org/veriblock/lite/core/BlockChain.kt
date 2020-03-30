// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.lite.core

import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel.Factory.CONFLATED
import org.veriblock.core.utilities.createLogger
import org.veriblock.lite.params.NetworkParameters
import org.veriblock.lite.store.StoredVeriBlockBlock
import org.veriblock.lite.store.VeriBlockBlockStore
import org.veriblock.lite.util.Threading
import org.veriblock.sdk.models.BlockStoreException
import org.veriblock.sdk.models.VBlakeHash
import org.veriblock.sdk.models.VeriBlockBlock
import org.veriblock.sdk.models.VerificationException
import org.veriblock.sdk.services.ValidationService
import org.veriblock.sdk.util.BitcoinUtils
import java.math.BigInteger

private const val MINIMUM_TIMESTAMP_BLOCK_COUNT = 20
private const val DIFFICULTY_ADJUST_BLOCK_COUNT = VeriBlockDifficultyCalculator.RETARGET_PERIOD

private val logger = createLogger {}

class BlockChain(
    private val params: NetworkParameters,
    private val veriBlockStore: VeriBlockBlockStore
) {
    val newBestBlockEvent = AsyncEvent<FullBlock>(Threading.LISTENER_THREAD)
    val newBestBlockChannel = BroadcastChannel<FullBlock>(CONFLATED)

    val blockChainReorganizedEvent = AsyncEvent<BlockChainReorganizedEventData>(Threading.LISTENER_THREAD)

    fun getChainHead(): VeriBlockBlock? = veriBlockStore.getChainHead()?.block

    fun get(hash: VBlakeHash): VeriBlockBlock? = veriBlockStore.get(hash)?.block

    fun add(block: VeriBlockBlock) {
        // Lightweight verification of the header
        ValidationService.verify(block)

        // Further verification requiring context
        var cumulativeWork = BigInteger.ZERO
        if (getChainHead() != null) {
            val previous = checkConnectivity(block)
            if (!verifyBlock(block, previous)) {
                return
            }
            cumulativeWork += previous.work
        }

        val storedBlock = StoredVeriBlockBlock(
            block,
            cumulativeWork + BitcoinUtils.decodeCompactBits(block.difficulty.toLong())
        )

        veriBlockStore.put(storedBlock)

        // For the time being, add is only called when building the best chain, so the added block is always the new head
        veriBlockStore.setChainHead(storedBlock)
    }

    fun handleNewBestChain(oldBlocks: List<VeriBlockBlock>, newBlocks: List<FullBlock>) {
        val reorganizing = oldBlocks.isNotEmpty()

        logger.debug { "Handling new best chain (${if (reorganizing) "reorg" else "no reorg"})..." }
        if (newBlocks.isNotEmpty()) {
            logger.debug { "There are ${newBlocks.size} blocks to process" }
            for (block in newBlocks) {
                try {
                    add(block)
                } catch (e: BlockStoreException) {
                    logger.error("VeriBlockBlock store exception", e)
                    break
                }

                if (!reorganizing) {
                    logger.debug { "Notifying event listeners about VBK block ${block.hash}" }
                    informListenersNewBestBlock(block)
                }
            }

            if (reorganizing) {
                blockChainReorganizedEvent.trigger(BlockChainReorganizedEventData(oldBlocks, newBlocks))
            }
        }
    }

    private fun informListenersNewBestBlock(block: FullBlock) {
        newBestBlockEvent.trigger(block)
        newBestBlockChannel.offer(block)
    }

    private fun informListenersBlockChainReorganized(oldBlocks: List<VeriBlockBlock>, newBlocks: List<FullBlock>) {
        blockChainReorganizedEvent.trigger(BlockChainReorganizedEventData(oldBlocks, newBlocks))
    }

    private fun verifyBlock(block: VeriBlockBlock, previous: StoredVeriBlockBlock): Boolean {
        if (!checkDuplicate(block)) {
            return false
        }

        val context = veriBlockStore.get(block.previousBlock, DIFFICULTY_ADJUST_BLOCK_COUNT)

        checkTimestamp(block, context)
        checkDifficulty(block, previous, context)

        return true
    }

    private fun checkDuplicate(block: VeriBlockBlock): Boolean {
        // Duplicate?
        val duplicate = veriBlockStore.get(block.hash)
        if (duplicate != null) {
            logger.info { "Block '${block.hash}' has already been added" }
            return false
        }

        return true
    }

    private fun checkConnectivity(block: VeriBlockBlock): StoredVeriBlockBlock {
        // Connects to a known "seen" block (except for origin block)
        val previous = veriBlockStore.get(block.previousBlock)
            ?: throw VerificationException("Block does not fit")

        val keystone = veriBlockStore.get(block.previousKeystone)
        if (keystone == null) {
            // Do I have any blocks at that keystone height?
            var keystoneBlocksAgo = block.height % 20
            when (keystoneBlocksAgo) {
                0 -> keystoneBlocksAgo = 20
                1 -> keystoneBlocksAgo = 21
            }
            val context = veriBlockStore.get(block.previousBlock, keystoneBlocksAgo)
            if (context.size == keystoneBlocksAgo) {
                throw VerificationException("Block's previous keystone is not found")
            }
            // If the context chain can't reach to this height, we just don't have enough blocks yet
        }

        val secondKeystone = veriBlockStore.get(block.secondPreviousKeystone)
        if (secondKeystone == null) {
            // Do I have any blocks at that keystone height?
            var keystoneBlocksAgo = block.height % 20
            when (keystoneBlocksAgo) {
                0 -> keystoneBlocksAgo = 40
                1 -> keystoneBlocksAgo = 41
                else -> keystoneBlocksAgo += 20
            }
            val context = veriBlockStore.get(block.previousBlock, keystoneBlocksAgo)
            if (context.size == keystoneBlocksAgo) {
                throw VerificationException("Block's second previous keystone is not found")
            }
        }

        return previous
    }

    private fun checkTimestamp(block: VeriBlockBlock, context: List<StoredVeriBlockBlock>) {
        if (context.size < MINIMUM_TIMESTAMP_BLOCK_COUNT) {
            logger.debug("Not enough VBK context blocks to check timestamp")
            return
        }

        val median = context.asSequence()
            .sortedByDescending { it.height }
            .take(MINIMUM_TIMESTAMP_BLOCK_COUNT)
            .map { it.block.timestamp }
            .sorted()
            .drop(MINIMUM_TIMESTAMP_BLOCK_COUNT / 2 - 1)
            .firstOrNull()

        if (median == null || block.timestamp <= median) {
            throw VerificationException("Block is too far in the past")
        }
    }

    private fun checkDifficulty(block: VeriBlockBlock, previous: StoredVeriBlockBlock, context: List<StoredVeriBlockBlock>) {
        if (context.size < DIFFICULTY_ADJUST_BLOCK_COUNT) {
            logger.debug("Not enough VBK context blocks to check difficulty")
            return
        }

        val contextBlocks = context.map { it.block }
        val calculated = VeriBlockDifficultyCalculator.calculate(params, previous.block, contextBlocks)

        if (block.difficulty != BitcoinUtils.encodeCompactBits(calculated).toInt()) {
            throw VerificationException("Block does not conform to expected difficulty")
        }
    }

    fun reset() {
        veriBlockStore.reset()
    }
}
