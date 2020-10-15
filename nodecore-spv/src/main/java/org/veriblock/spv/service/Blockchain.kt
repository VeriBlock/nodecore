// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.spv.service

import org.veriblock.core.bitcoinj.BitcoinUtilities
import org.veriblock.core.crypto.VBlakeHash
import org.veriblock.core.utilities.createLogger
import org.veriblock.sdk.blockchain.store.StoredVeriBlockBlock
import org.veriblock.sdk.models.VeriBlockBlock
import org.veriblock.sdk.services.ValidationService
import org.veriblock.spv.util.SpvEventBus

private val logger = createLogger {}

class Blockchain(
    private val blockStore: BlockStore
) {
    val activeChain = TruncatedChain(blockStore)

    val size: Int
        get() = this.blockStore.index.fileIndex.size

    fun getChainHead(): StoredVeriBlockBlock {
        return activeChain.tip()
    }

    fun get(hash: VBlakeHash): StoredVeriBlockBlock? {
        return blockStore.readBlock(hash)
    }

    fun acceptBlock(block: VeriBlockBlock): Boolean {
        if (blockStore.exists(block.hash)) {
            // block is valid, we already have it
            return true
        }

        // does block connect to blockchain?
        val prev = blockStore.readBlock(block.previousBlock)
            ?: return false

        // is block statelessly valid?
        if (!ValidationService.checkBlock(block)) {
            return false
        }

        // all ok, we can add block to blockchain
        val stored = StoredVeriBlockBlock(
            header = block,
            work = prev.work + BitcoinUtilities.decodeCompactBits(block.difficulty.toLong()),
            hash = block.hash
        )

        // TODO: contextually check block: get difficulty, get median time past, get keystones

        // write block on disk
        blockStore.writeBlock(stored)

        activeChain.findFork(stored)
            ?:
            // this block does not connect to active chain
            // do not consider it for fork resolution
            return true

        // do fork resolution
        if (stored.work > activeChain.tip().work) {
            // new block wins
            activeChain.setTip(stored)
            SpvEventBus.newBestBlockEvent.trigger(block)
        }

        // add new block to a queue
        SpvEventBus.newBlockChannel.offer(block)

        return true
    }

    fun getBlockByHeight(height: Int): StoredVeriBlockBlock? {
        return activeChain.get(height)
    }

    fun getPeerQuery(): List<VeriBlockBlock> {
        return activeChain.getLast(16).map { it.header }
    }
}
