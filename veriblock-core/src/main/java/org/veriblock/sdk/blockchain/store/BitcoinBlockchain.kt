// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.sdk.blockchain.store

import org.veriblock.core.bitcoinj.BitcoinUtilities
import org.veriblock.core.crypto.Sha256Hash
import org.veriblock.core.params.BitcoinNetworkParameters
import org.veriblock.core.utilities.Preconditions
import org.veriblock.core.utilities.createLogger
import org.veriblock.sdk.auditor.Change
import org.veriblock.sdk.auditor.Operation
import org.veriblock.sdk.blockchain.changes.AddBitcoinBlockChange
import org.veriblock.sdk.blockchain.changes.SetBitcoinHeadChange
import org.veriblock.sdk.blockchain.store.BlockStore
import org.veriblock.sdk.blockchain.store.StoredBitcoinBlock
import org.veriblock.sdk.models.BitcoinBlock
import org.veriblock.sdk.models.BlockStoreException
import org.veriblock.sdk.models.Constants
import org.veriblock.sdk.models.VerificationException
import org.veriblock.sdk.services.ValidationService
import java.math.BigInteger
import java.sql.SQLException
import java.util.ArrayList
import java.util.Comparator
import java.util.HashMap
import java.util.OptionalInt
import java.util.OptionalLong
import java.util.function.Consumer

private const val MINIMUM_TIMESTAMP_BLOCK_COUNT = 11

private val logger = createLogger {}

open class BitcoinBlockchain(
    private val networkParameters: BitcoinNetworkParameters,
    private val store: BlockStore<StoredBitcoinBlock, Sha256Hash>
) {
    private val temporalStore: MutableMap<Sha256Hash, StoredBitcoinBlock> = HashMap()
    private var temporaryChainHead: StoredBitcoinBlock? = null
    private var skipValidateBlocksDifficulty = false
    private fun hasTemporaryModifications(): Boolean {
        return temporaryChainHead != null || temporalStore.isNotEmpty()
    }

    val isValidateBlocksDifficulty: Boolean
        get() = !skipValidateBlocksDifficulty

    fun setSkipValidateBlocksDifficulty(skip: Boolean) {
        skipValidateBlocksDifficulty = skip
    }

    @Throws(BlockStoreException::class, SQLException::class)
    operator fun get(hash: Sha256Hash): BitcoinBlock? {
        val storedBlock = getInternal(hash)
        return storedBlock?.block
    }

    @Throws(BlockStoreException::class, SQLException::class)
    fun searchBestChain(hash: Sha256Hash): BitcoinBlock? {
        // Look at the temporal store first
        val storedBlock: StoredBitcoinBlock?
        storedBlock = if (temporaryChainHead != null) {
            temporalStore[hash]
        } else {
            store.scanBestChain(hash)
        }
        return storedBlock?.block
    }

    @Throws(VerificationException::class, BlockStoreException::class, SQLException::class)
    fun add(block: BitcoinBlock): List<Change> {
        Preconditions.state(!hasTemporaryModifications(), "Cannot add a block while having temporary modifications")

        // Lightweight verification of the header
        ValidationService.verify(block)
        var work = BigInteger.ZERO
        // TODO: Need to be able to set this accurately on the first block
        var currentHeight = 0
        if (getChainHeadInternal() != null) {
            // Further verification requiring context
            val previous = checkConnectivity(block)
            if (!verifyBlock(block, previous)) {
                return emptyList()
            }
            work = work.add(previous!!.work)
            currentHeight = previous.height + 1
        }
        val storedBlock = StoredBitcoinBlock(
            block,
            work.add(BitcoinUtilities.decodeCompactBits(block.difficulty.toLong())),
            currentHeight
        )
        val changes: MutableList<Change> = ArrayList()
        store.put(storedBlock)
        changes.add(AddBitcoinBlockChange(null, storedBlock))
        val chainHead = store.getChainHead()
        if (chainHead == null || storedBlock.work.compareTo(chainHead.work) > 0) {
            var priorHead = store.setChainHead(storedBlock)
            ///HACK: this is a dummy block that represents a change from null to genesis block
            if (priorHead == null) {
                val emptyBlock = BitcoinBlock(
                    0, Sha256Hash.ZERO_HASH, Sha256Hash.ZERO_HASH, 0, 1, 0
                )
                priorHead = StoredBitcoinBlock(emptyBlock, BigInteger.ONE, 0)
            }
            changes.add(SetBitcoinHeadChange(priorHead, storedBlock))
        }
        return changes
    }

    @Throws(
        VerificationException::class, BlockStoreException::class, SQLException::class
    )
    fun addAll(blocks: List<BitcoinBlock>): List<Change> {
        Preconditions.state(!hasTemporaryModifications(), "Cannot add blocks whle having temporary modifications")
        val changes: MutableList<Change> = ArrayList()
        for (block in blocks) {
            changes.addAll(add(block))
        }
        return changes
    }

    @Throws(VerificationException::class, BlockStoreException::class, SQLException::class)
    fun addTemporarily(block: BitcoinBlock) {
        // Lightweight verification of the header
        ValidationService.verify(block)

        // Further verification requiring context
        val previous = checkConnectivity(block)
        if (!verifyBlock(block, previous)) {
            return
        }
        val storedBlock = StoredBitcoinBlock(
            block,
            previous!!.work.add(BitcoinUtilities.decodeCompactBits(block.difficulty.toLong())),
            previous.height + 1
        )
        temporalStore[block.hash] = storedBlock
        val chainHead = getChainHeadInternal()
        if (storedBlock.work.compareTo(chainHead!!.work) > 0) {
            temporaryChainHead = storedBlock
        }
    }

    fun addAllTemporarily(blocks: List<BitcoinBlock>) {
        blocks.forEach(Consumer { t: BitcoinBlock ->
            try {
                addTemporarily(t)
            } catch (e: VerificationException) {
                throw BlockStoreException(e)
            } catch (e: BlockStoreException) {
                throw BlockStoreException(e)
            } catch (e: SQLException) {
                throw BlockStoreException(e)
            }
        })
    }

    fun clearTemporaryModifications() {
        temporaryChainHead = null
        temporalStore.clear()
    }

    @Throws(BlockStoreException::class, SQLException::class)
    fun rewind(changes: List<Change>) {
        for (change in changes) {
            if (change.chainIdentifier == Constants.BITCOIN_HEADER_MAGIC) {
                when (change.operation) {
                    Operation.ADD_BLOCK -> {
                        val newValue = StoredBitcoinBlock.deserialize(change.newValue)
                        if (change.oldValue != null && change.oldValue.size > 0) {
                            val oldValue = StoredBitcoinBlock.deserialize(change.oldValue)
                            store.replace(newValue.hash, oldValue)
                        } else {
                            store.erase(newValue.hash)
                        }
                    }
                    Operation.SET_HEAD -> {
                        val priorHead = StoredBitcoinBlock.deserialize(change.oldValue)
                        store.setChainHead(priorHead)
                    }
                    else -> {
                    }
                }
            }
        }
    }

    @Throws(SQLException::class)
    fun getChainHead(): BitcoinBlock? {
        val chainHead = store.getChainHead()
        return chainHead?.block
    }

    // in case there's a need to know the chain head block height
    @Throws(SQLException::class)
    fun getStoredChainHead(): StoredBitcoinBlock? =
        store.getChainHead()

    @Throws(BlockStoreException::class, SQLException::class)
    private fun getInternal(hash: Sha256Hash): StoredBitcoinBlock? {
        return if (temporalStore.containsKey(hash)) {
            temporalStore[hash]
        } else {
            store.get(hash)
        }
    }

    @Throws(BlockStoreException::class, SQLException::class)
    private fun getChainHeadInternal(): StoredBitcoinBlock? =
        if (temporaryChainHead != null) temporaryChainHead else store.getChainHead()

    private fun getTemporaryBlocks(hash: Sha256Hash, count: Int): MutableList<StoredBitcoinBlock?> {
        val blocks: MutableList<StoredBitcoinBlock?> = ArrayList()
        var cursor = Sha256Hash.wrap(hash.bytes)
        while (temporalStore.containsKey(cursor)) {
            val tempBlock = temporalStore[cursor]
            blocks.add(tempBlock)
            if (blocks.size >= count) break
            cursor = tempBlock!!.block.previousBlock
        }
        return blocks
    }

    @Throws(VerificationException::class, BlockStoreException::class, SQLException::class)
    private fun verifyBlock(block: BitcoinBlock, previous: StoredBitcoinBlock?): Boolean {
        if (!checkDuplicate(block)) return false
        checkTimestamp(block)
        checkDifficulty(block, previous)
        return true
    }

    @Throws(BlockStoreException::class, SQLException::class)
    private fun checkDuplicate(block: BitcoinBlock): Boolean {
        // Duplicate?
        val duplicate = getInternal(block.hash)
        if (duplicate != null) {
            logger.trace("Block '{}' has already been added", block.hash.toString())
            return false
        }
        return true
    }

    @Throws(BlockStoreException::class, SQLException::class)
    private fun checkConnectivity(block: BitcoinBlock): StoredBitcoinBlock? {
        // Connects to a known "seen" block (except for origin block)
        val previous = getInternal(block.previousBlock)
        if (previous == null) {
            // corner case: the first bootstrap block connects to the blockchain
            // by definition despite not having the previous block in the store
            if (getInternal(block.hash) == null) {
                throw VerificationException("Block does not fit")
            }
        }
        return previous
    }

    // return the earliest valid timestamp for a block that follows the blockHash block
    @Throws(BlockStoreException::class, SQLException::class)
    fun getNextEarliestTimestamp(blockHash: Sha256Hash): OptionalInt {
        // Checks the temporary blocks first
        val context = getTemporaryBlocks(
            blockHash, MINIMUM_TIMESTAMP_BLOCK_COUNT
        )
        if (context.size > 0) {
            val last = context[context.size - 1]
            context.addAll(store.get(last!!.block.previousBlock, MINIMUM_TIMESTAMP_BLOCK_COUNT - context.size))
        } else {
            context.addAll(store.get(blockHash, MINIMUM_TIMESTAMP_BLOCK_COUNT))
        }
        if (context.size < MINIMUM_TIMESTAMP_BLOCK_COUNT) {
            return OptionalInt.empty()
        }
        val median = context.stream().sorted(
                Comparator.comparingInt { obj: StoredBitcoinBlock -> obj.height }.reversed()
            )
            .limit(MINIMUM_TIMESTAMP_BLOCK_COUNT.toLong())
            .map { b: StoredBitcoinBlock? -> b!!.block.timestamp }
            .sorted()
            .skip(MINIMUM_TIMESTAMP_BLOCK_COUNT / 2.toLong())
            .findFirst()
        return if (median.isPresent) OptionalInt.of(median.get() + 1) else OptionalInt.empty()
    }

    @Throws(
        VerificationException::class, BlockStoreException::class, SQLException::class
    )
    private fun checkTimestamp(block: BitcoinBlock) {
        val timestamp = getNextEarliestTimestamp(block.previousBlock)
        if (timestamp.isPresent) {
            if (block.timestamp < timestamp.asInt) {
                throw VerificationException("Block is too far in the past")
            }
        } else {
            logger.debug("Not enough context blocks to check the timestamp of block '{}'", block.hash.toString())
        }
    }

    @Throws(BlockStoreException::class, SQLException::class)
    fun getNextDifficulty(blockTimestamp: Int, previous: StoredBitcoinBlock?): OptionalLong {
        var previous = previous
        val difficultyAdjustmentInterval = (networkParameters.powTargetTimespan
            / networkParameters.powTargetSpacing)

        // Special rule for the regtest: all blocks are minimum difficulty
        if (networkParameters.powNoRetargeting) return OptionalLong.of(previous!!.block.difficulty.toLong())

        // Previous + 1 = height of block
        return if ((previous!!.height + 1) % difficultyAdjustmentInterval > 0) {

            // Unless minimum difficulty blocks are allowed(special difficulty rule for the testnet),
            // the difficulty should be same as previous
            if (!networkParameters.allowMinDifficultyBlocks) {
                OptionalLong.of(previous.block.difficulty.toLong())
            } else {
                val proofOfWorkLimit = BitcoinUtilities.encodeCompactBits(networkParameters.powLimit)

                // If the block's timestamp is more than 2*PowTargetSpacing minutes
                // then allow mining of a minimum difficulty block
                if (blockTimestamp > previous.block.timestamp + networkParameters.powTargetSpacing * 2) {
                    OptionalLong.of(proofOfWorkLimit)
                } else {

                    // Find the last non-minimum difficulty block
                    while (previous != null && previous.block.previousBlock != null && previous.height % difficultyAdjustmentInterval != 0 && previous.block.difficulty.toLong() == proofOfWorkLimit
                    ) {
                        previous = getInternal(previous.block.previousBlock)
                    }

                    // Corner case: we're less than difficultyAdjustmentInterval
                    // from the bootstrap and all blocks are minimum difficulty
                    if (previous == null) OptionalLong.empty() else OptionalLong.of(previous.block.difficulty.toLong())

                    // Difficulty matches the closest non-minimum difficulty block
                }
            }
        } else {

            // Difficulty needs to adjust
            val tempBlocks: List<StoredBitcoinBlock?> = getTemporaryBlocks(previous.hash, difficultyAdjustmentInterval)
            val cycleStart: StoredBitcoinBlock?
            cycleStart = if (tempBlocks.size == difficultyAdjustmentInterval) {
                tempBlocks[tempBlocks.size - 1]
            } else if (tempBlocks.size > 0) {
                val last = tempBlocks[tempBlocks.size - 1]
                store.getFromChain(last!!.block.previousBlock, difficultyAdjustmentInterval - tempBlocks.size)
            } else {
                store.getFromChain(previous.hash, difficultyAdjustmentInterval - 1)
            }
            if (cycleStart == null) {
                // Because there will just be some Bitcoin block from whence accounting begins, it's likely
                // that the first adjustment period will not have sufficient blocks to compute correctly
                return OptionalLong.empty()
            }
            val newTarget = calculateNewTarget(
                previous.block.difficulty.toLong(),
                cycleStart.block.timestamp,
                previous.block.timestamp
            )
            OptionalLong.of(newTarget)
        }
    }

    @Throws(VerificationException::class, BlockStoreException::class, SQLException::class)
    private fun checkDifficulty(block: BitcoinBlock, previous: StoredBitcoinBlock?) {
        if (!isValidateBlocksDifficulty) {
            return
        }
        val difficulty = getNextDifficulty(block.timestamp, previous)
        if (difficulty.isPresent) {
            if (block.difficulty.toLong() != difficulty.asLong) {
                throw VerificationException("Block does not match computed difficulty adjustment")
            }
        } else {
            logger.debug("Not enough context blocks to check the difficulty of block '{}'", block.hash.toString())
        }
    }

    private fun calculateNewTarget(current: Long, startTimestamp: Int, endTimestamp: Int): Long {
        var elapsed = endTimestamp - startTimestamp
        elapsed = Math.max(elapsed, networkParameters.powTargetTimespan / 4)
        elapsed = Math.min(elapsed, networkParameters.powTargetTimespan * 4)
        var newTarget = BitcoinUtilities.decodeCompactBits(current)
            .multiply(BigInteger.valueOf(elapsed.toLong()))
            .divide(BigInteger.valueOf(networkParameters.powTargetTimespan.toLong()))

        // Should never occur; hitting the max target would mean Bitcoin has the hashrate of a few CPUs
        newTarget = newTarget.min(networkParameters.powLimit)

        // Reduce the precision of the calculated difficulty to match that of the compact bits representation
        val byteLength = (newTarget.bitLength() + 8 - 1) / 8
        val mask = BigInteger.valueOf(0xFFFFFFL).shiftLeft((byteLength - 3) * 8)
        newTarget = newTarget.and(mask)
        return BitcoinUtilities.encodeCompactBits(newTarget)
    }

    // Returns true if the store was empty and the bootstrap
    // blocks were added to it successfully.
    // Otherwise, returns false.
    @Throws(SQLException::class, VerificationException::class)
    fun bootstrap(blocks: List<BitcoinBlock>, firstBlockHeight: Int): Boolean {
        assert(!blocks.isEmpty())
        assert(firstBlockHeight >= 0)
        val bootstrapped = store.getChainHead() != null
        if (!bootstrapped) {
            logger.info(
                "Bootstrapping starting at height {} with {} blocks: {} to {}", firstBlockHeight.toString(), blocks.size.toString(),
                blocks[0].hash.toString(),
                blocks[blocks.size - 1].hash.toString()
            )
            var prevHash: Sha256Hash? = null
            for (block in blocks) {
                if (prevHash != null && block.previousBlock != prevHash) throw VerificationException(
                    "Bitcoin bootstrap blocks must be contiguous"
                )
                prevHash = block.hash
            }
            var blockHeight = firstBlockHeight
            for (block in blocks) {
                val work = BitcoinUtilities.decodeCompactBits(block.difficulty.toLong())
                val storedBlock = StoredBitcoinBlock(block, work, blockHeight)
                blockHeight++
                store.put(storedBlock)
                store.setChainHead(storedBlock)
            }
        }
        return !bootstrapped
    }
}
