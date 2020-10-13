// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.sdk.blockchain

import org.slf4j.LoggerFactory
import org.veriblock.core.bitcoinj.BitcoinUtilities
import org.veriblock.core.crypto.Sha256Hash
import org.veriblock.core.crypto.VBlakeHash
import org.veriblock.core.params.NetworkParameters
import org.veriblock.core.utilities.Preconditions
import org.veriblock.sdk.blockchain.store.BlockStore
import org.veriblock.sdk.blockchain.store.StoredBitcoinBlock
import org.veriblock.sdk.blockchain.store.StoredVeriBlockBlock
import org.veriblock.sdk.models.BlockStoreException
import org.veriblock.sdk.models.VeriBlockBlock
import org.veriblock.sdk.models.VerificationException
import org.veriblock.sdk.services.ValidationService
import java.math.BigInteger
import java.sql.SQLException
import java.util.ArrayList
import java.util.Comparator
import java.util.HashMap
import java.util.OptionalInt

open class VeriBlockBlockchain(
    networkParameters: NetworkParameters,
    store: BlockStore<StoredVeriBlockBlock, VBlakeHash>,
    bitcoinStore: BlockStore<StoredBitcoinBlock, Sha256Hash>
) {
    private val store: BlockStore<StoredVeriBlockBlock, VBlakeHash>
    private val bitcoinStore: BlockStore<StoredBitcoinBlock, Sha256Hash>
    private val temporalStore: MutableMap<VBlakeHash, StoredVeriBlockBlock>
    val networkParameters: NetworkParameters
    private var temporaryChainHead: StoredVeriBlockBlock? = null
    private var skipValidateBlocksDifficulty = false
    private fun hasTemporaryModifications(): Boolean {
        return temporaryChainHead != null || temporalStore.size > 0
    }

    val isValidateBlocksDifficulty: Boolean
        get() = !skipValidateBlocksDifficulty

    fun setSkipValidateBlocksDifficulty(skip: Boolean) {
        skipValidateBlocksDifficulty = skip
    }

    @Throws(BlockStoreException::class, SQLException::class)
    fun get(hash: VBlakeHash): VeriBlockBlock? {
        val storedBlock = getInternal(hash)
        return storedBlock?.block
    }

    @Throws(BlockStoreException::class, SQLException::class)
    fun searchBestChain(hash: VBlakeHash): VeriBlockBlock? {
        // Look at the temporal store first
        val storedBlock: StoredVeriBlockBlock?
        storedBlock = if (temporaryChainHead != null) {
            getInternal(hash)
        } else {
            store.scanBestChain(hash)
        }
        return storedBlock?.block
    }

    @Throws(VerificationException::class, BlockStoreException::class, SQLException::class)
    fun add(block: VeriBlockBlock) {
        Preconditions.state(!hasTemporaryModifications(), "Cannot add a block while having temporary modifications")
        check(!hasTemporaryModifications()) {
            "Cannot add a block while having temporary modifications"
        }

        // Lightweight verification of the header
        ValidationService.verify(block)
        var work = BigInteger.ZERO
        if (getChainHeadInternal() != null) {
            // Further verification requiring context
            val previous = checkConnectivity(block)!!
            if (!verifyBlock(block, previous)) {
                return
            }
            work = work.add(previous.work)
        }
        val storedBlock = StoredVeriBlockBlock(
            block,
            work.add(BitcoinUtilities.decodeCompactBits(block.difficulty.toLong())),
            block.hash
        )
        store.put(storedBlock)
    }

    @Throws(VerificationException::class, BlockStoreException::class, SQLException::class)
    fun addAll(blocks: List<VeriBlockBlock>) {
        check(!hasTemporaryModifications()) {
            "Cannot add blocks while having temporary modifications"
        }
        val sortedBlocks: List<VeriBlockBlock> = blocks.sortedBy {
            it.height
        }
        sortedBlocks.forEach {
            add(it)
        }
    }

    @Throws(SQLException::class)
    fun getChainHead(): VeriBlockBlock? {
        val chainHead = store.getChainHead()
        return chainHead?.block
    }

    @Throws(BlockStoreException::class, SQLException::class)
    private fun getInternal(hash: VBlakeHash): StoredVeriBlockBlock? {
        val trimmed = hash.trimToPreviousKeystoneSize()
        return if (temporalStore.containsKey(trimmed)) {
            temporalStore[trimmed]
        } else {
            store.get(hash)
        }
    }

    @Throws(BlockStoreException::class, SQLException::class)
    private fun getChainHeadInternal(): StoredVeriBlockBlock? =
        temporaryChainHead ?: store.getChainHead()

    @Throws(BlockStoreException::class, SQLException::class)
    private fun getChainInternal(head: VBlakeHash, count: Int): List<StoredVeriBlockBlock> {
        val blocks: MutableList<StoredVeriBlockBlock> = ArrayList()
        var cursor = head.trimToPreviousKeystoneSize()
        while (temporalStore.containsKey(cursor)) {
            val tempBlock = temporalStore[cursor]!!
            blocks.add(tempBlock)
            if (blocks.size == count) {
                break
            }
            cursor = tempBlock.block.previousBlock.trimToPreviousKeystoneSize()
        }
        if (blocks.size > 0) {
            val last = blocks[blocks.size - 1]
            blocks.addAll(store.get(last.block.previousBlock, count - blocks.size))
        } else {
            blocks.addAll(store.get(head, count))
        }
        return blocks
    }

    @Throws(VerificationException::class, BlockStoreException::class, SQLException::class)
    private fun verifyBlock(block: VeriBlockBlock, previous: StoredVeriBlockBlock): Boolean {
        if (!checkDuplicate(block)) {
            return false
        }
        val context = getChainInternal(block.previousBlock, DIFFICULTY_ADJUST_BLOCK_COUNT)
        checkTimestamp(block, context)
        checkDifficulty(block, previous, context)
        return true
    }

    @Throws(BlockStoreException::class, SQLException::class)
    private fun checkDuplicate(block: VeriBlockBlock): Boolean {
        // Duplicate?
        val duplicate = getInternal(block.hash)
        if (duplicate != null) {
            log.trace("Block '{}' has already been added", block.hash.toString())
            return false
        }
        return true
    }

    @Throws(BlockStoreException::class, SQLException::class)
    private fun checkConnectivity(block: VeriBlockBlock): StoredVeriBlockBlock? {
        // Connects to a known "seen" block (except for origin block)
        val previous = getInternal(block.previousBlock)
        if (previous == null) {
            // corner case: the first bootstrap block connects to the blockchain
            // by definition despite not having the previous block in the store
            if (getInternal(block.hash) == null) {
                throw VerificationException("Block does not fit")
            }
        }
        val keystone = store.get(block.previousKeystone)
        if (keystone == null) {
            // Do I have any blocks at that keystone height?
            var keystoneBlocksAgo = block.height % 20
            when (keystoneBlocksAgo) {
                0 -> keystoneBlocksAgo = 20
                1 -> keystoneBlocksAgo = 21
            }
            val context = store.get(
                block.previousBlock, keystoneBlocksAgo
            )
            if (context.size == keystoneBlocksAgo) {
                throw VerificationException("Block's previous keystone is not found")
            }
            // If the context chain can't reach to this height, we just don't have enough blocks yet
        }
        val secondKeystone = store.get(block.secondPreviousKeystone)
        if (secondKeystone == null) {
            // Do I have any blocks at that keystone height?
            var keystoneBlocksAgo = block.height % 20
            when (keystoneBlocksAgo) {
                0 -> keystoneBlocksAgo = 40
                1 -> keystoneBlocksAgo = 41
                else -> keystoneBlocksAgo += 20
            }
            val context = store.get(
                block.previousBlock, keystoneBlocksAgo
            )
            if (context.size == keystoneBlocksAgo) {
                throw VerificationException("Block's second previous keystone is not found")
            }
        }
        return previous
    }

    // return the earliest valid timestamp for a block that follows the blockHash block
    @Throws(SQLException::class)
    fun getNextEarliestTimestamp(blockHash: VBlakeHash): Int? {
        val context = getChainInternal(
            blockHash, DIFFICULTY_ADJUST_BLOCK_COUNT
        )
        return getNextEarliestTimestamp(context)
    }

    fun getNextEarliestTimestamp(context: List<StoredVeriBlockBlock>): Int? {
        if (context.size < MINIMUM_TIMESTAMP_BLOCK_COUNT) {
            return null
        }
        val median = context.asSequence()
            .sortedByDescending { it.height }
            .take(MINIMUM_TIMESTAMP_BLOCK_COUNT)
            .map { it.block.timestamp }
            .sorted()
            .drop(MINIMUM_TIMESTAMP_BLOCK_COUNT / 2 - 1)
            .firstOrNull()
        return if (median != null) median + 1 else null
    }

    @Throws(VerificationException::class)
    private fun checkTimestamp(
        block: VeriBlockBlock,
        context: List<StoredVeriBlockBlock>
    ) {
        val timestamp = getNextEarliestTimestamp(context)
        if (timestamp != null) {
            if (block.timestamp < timestamp) {
                throw VerificationException("Block is too far in the past")
            }
        } else {
            log.debug("Not enough context blocks to check the timestamp of block '{}'", block.hash.toString())
        }
    }

    fun getNextDifficulty(previous: VeriBlockBlock, context: List<VeriBlockBlock>): OptionalInt {
        if (previous.height >= VeriBlockDifficultyCalculator.RETARGET_PERIOD &&
            context.size < VeriBlockDifficultyCalculator.RETARGET_PERIOD
        ) {
            return OptionalInt.empty()
        }
        val difficulty = VeriBlockDifficultyCalculator.calculate(networkParameters, previous, context)
        return OptionalInt.of(BitcoinUtilities.encodeCompactBits(difficulty).toInt())
    }

    @Throws(SQLException::class)
    fun getNextDifficulty(previous: VeriBlockBlock): OptionalInt {
        val storedContext = getChainInternal(
            previous.hash, VeriBlockDifficultyCalculator.RETARGET_PERIOD
        )
        val context: List<VeriBlockBlock> = storedContext.map { it.block }
        return getNextDifficulty(previous, context)
    }

    @Throws(VerificationException::class)
    private fun checkDifficulty(
        block: VeriBlockBlock,
        previous: StoredVeriBlockBlock,
        context: List<StoredVeriBlockBlock>
    ) {
        if (!isValidateBlocksDifficulty) {
            return
        }
        val contextBlocks: List<VeriBlockBlock> = context.map { it.block }
        val difficulty = getNextDifficulty(previous.block, contextBlocks)
        if (difficulty.isPresent) {
            if (block.difficulty != difficulty.asInt) {
                throw VerificationException("Block does not conform to expected difficulty")
            }
        } else {
            log.debug("Not enough context blocks to check the difficulty of block '{}'", block.hash.toString())
        }
    }

    // Returns true if the store was empty and the bootstrap
    // blocks were added to it successfully.
    // Otherwise, returns false.
    @Throws(SQLException::class, VerificationException::class)
    fun bootstrap(blocks: List<VeriBlockBlock>): Boolean {
        assert(!blocks.isEmpty())
        val bootstrapped = store.getChainHead() != null
        if (!bootstrapped) {
            log.info(
                "Bootstrapping starting at height {} with {} blocks: {} to {}", blocks[0].height.toString(), blocks.size.toString(),
                blocks[0].hash.toString(),
                blocks[blocks.size - 1].hash.toString()
            )
            var prevHash: VBlakeHash? = null
            for (block in blocks) {
                if (prevHash != null && block.previousBlock != prevHash) throw VerificationException(
                    "VeriBlock bootstrap blocks must be contiguous"
                )
                prevHash = block.hash.trimToPreviousBlockSize()
            }
            for (block in blocks) {
                val work = BitcoinUtilities.decodeCompactBits(block.difficulty.toLong())
                val storedBlock = StoredVeriBlockBlock(
                    block, work, block.hash
                )
                store.put(storedBlock)
                store.setChainHead(storedBlock)
            }
        }
        return !bootstrapped
    }

    companion object {
        private val log = LoggerFactory.getLogger(VeriBlockBlockchain::class.java)
        private const val MINIMUM_TIMESTAMP_BLOCK_COUNT = 20
        private const val DIFFICULTY_ADJUST_BLOCK_COUNT = VeriBlockDifficultyCalculator.RETARGET_PERIOD
        private const val BITCOIN_FINALITY = 11
        private val POP_CONSENSUS_WEIGHTS_BY_RELATIVE_BITCOIN_INDEX = intArrayOf(100, 100, 95, 89, 80, 69, 56, 40, 21)
    }

    init {
        Preconditions.notNull(
            store, "Store cannot be null"
        )
        Preconditions.notNull(
            bitcoinStore, "Bitcoin store cannot be null"
        )
        Preconditions.notNull(
            networkParameters, "Network parameters cannot be null"
        )
        this.store = store
        this.bitcoinStore = bitcoinStore
        this.networkParameters = networkParameters
        temporalStore = HashMap()
    }
}
