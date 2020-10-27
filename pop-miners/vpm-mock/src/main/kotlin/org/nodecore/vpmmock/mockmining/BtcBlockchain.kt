// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.nodecore.vpmmock.mockmining

import org.veriblock.core.bitcoinj.BitcoinUtilities
import org.veriblock.core.crypto.Sha256Hash
import org.veriblock.core.utilities.Utility
import org.veriblock.core.utilities.createLogger
import org.veriblock.sdk.models.BitcoinBlock
import org.veriblock.sdk.services.ValidationService
import java.util.concurrent.ConcurrentHashMap

private val logger = createLogger {}

class BtcBlockchain(
    val genesisBlock: BitcoinBlock,
    val genesisBlockHeight: Int
) {
    // in-memory block index
    val blockIndex = ConcurrentHashMap<Sha256Hash, BtcBlockIndex>()
    lateinit var activeChain: BtcChain
    val size get() = blockIndex.size

    init {
        writeGenesisBlock()
    }

    /**
     * Getter for block index.
     */
    fun getBlockIndex(hash: Sha256Hash): BtcBlockIndex? = blockIndex[hash]
    fun getBlockIndex(height: Int): BtcBlockIndex? = activeChain[height]

    /**
     * Getter for tip.
     */
    fun getChainHeadIndex(): BtcBlockIndex = activeChain.tip

    /**
     * Reads a chain of blocks from active chain of given `size` ending with `hash`
     */
    fun getChainWithTip(hash: Sha256Hash, size: Int): List<BitcoinBlock> {
        val ret = ArrayList<BtcBlockIndex>()
        var i = 0
        var cursor = getBlockIndex(hash)
        while (cursor != null && i++ < size) {
            ret.add(cursor)
            cursor = getBlockIndex(cursor.header.previousBlock)
        }
        ret.reverse()
        return ret.map { it.header }
    }

    fun getBody(hash: Sha256Hash): BitcoinBlockData? {
        return blockIndex[hash]?.body
    }

    fun getContext(from: Sha256Hash, to: Sha256Hash): List<BitcoinBlock> {
        val list = ArrayList<BitcoinBlock>()
        val _from = getBlockIndex(from)
        val _to = getBlockIndex(to)?.prev

        var cursor = _to
        while (cursor != null && cursor != _from) {
            list.add(cursor.header)
            cursor = cursor.prev
        }

        return list.reversed()
    }

    fun mine(data: BitcoinBlockData, prev: BtcBlockIndex = activeChain.tip): BitcoinBlock {
        val timestamp = prev.header.timestamp.coerceAtLeast(Utility.getCurrentTimestamp())
        // TODO(warchant): eventually add diff calculator
        val difficulty = prev.header.difficulty
        for (nonce in 0 until Int.MAX_VALUE) {
            val newBlock = BitcoinBlock(
                prev.header.version,
                prev.header.hash,
                data.merkleRoot.reversed(),
                timestamp,
                difficulty,
                nonce
            )
            if (ValidationService.isProofOfWorkValid(newBlock)) {
                val ret = acceptBlock(newBlock, data)
                check(ret)
                return newBlock
            }
        }
        throw RuntimeException("Failed to mine the block due to too high difficulty")
    }

    /**
     * Adds new block to Blockchain.
     * @return true if block is valid, false otherwise
     */
    fun acceptBlock(
        block: BitcoinBlock,
        body: BitcoinBlockData
    ): Boolean {
        if (getBlockIndex(block.hash) != null) {
            // block is valid, we already have it
            return true
        }

        // does block connect to blockchain?
        val prev = getBlockIndex(block.previousBlock)
        if (prev == null) {
            logger.warn { "Can not connect block=${block.hash}, its prev=${block.previousBlock}" }
            return false
        }

        // is block statelessly valid?
        if (!ValidationService.checkBlock(block)) {
            return false
        }

        // TODO: contextually check block

        // all ok, we can add block to blockchain
        val index = BtcBlockIndex(
            header = block,
            work = prev.work + BitcoinUtilities.decodeCompactBits(block.difficulty.toLong()),
            body = body,
            prev = prev,
            height = prev.height + 1
        )

        blockIndex[block.hash] = index

        // do fork resolution
        if (index.work > activeChain.tip.work) {
            // new block wins
            activeChain.setTip(index)
        }

        return true
    }

    private fun writeGenesisBlock() {
        val hash = genesisBlock.hash
        val work = BitcoinUtilities.decodeCompactBits(
            genesisBlock.difficulty.toLong()
        )

        // block index always contains genesis block on start
        val index = BtcBlockIndex(
            height = genesisBlockHeight,
            prev = null,
            work = work,
            body = BitcoinBlockData(),
            header = genesisBlock
        )

        blockIndex[hash] = index
        activeChain = BtcChain(genesisBlockHeight, index)
    }
}
