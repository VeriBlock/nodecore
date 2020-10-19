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
import org.veriblock.core.utilities.createLogger
import org.veriblock.sdk.blockchain.store.StoredVeriBlockBlock
import org.veriblock.sdk.models.VeriBlockBlock
import org.veriblock.sdk.services.ValidationService
import org.veriblock.spv.util.SpvEventBus
import java.lang.IllegalStateException

private val logger = createLogger {}

class Blockchain(
    private val blockStore: BlockStore
) {

    val activeChain get() = blockStore.activeChain
    val blockIndex get() = blockStore.blockIndex
    val size get() = blockIndex.size

    fun getChainHeadBlock(): StoredVeriBlockBlock {
        return blockStore.getChainHeadBlock()
    }

    fun getBlock(hash: AnyVbkHash): StoredVeriBlockBlock? {
        return blockStore.readBlock(hash)
    }

    fun getBlockIndex(hash: AnyVbkHash): BlockIndex? {
        return blockStore.getBlockIndex(hash)
    }

    fun acceptBlock(block: VeriBlockBlock): Boolean {
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

        // all ok, we can add block to blockchain
        val stored = StoredVeriBlockBlock(
            header = block,
            work = prev.work + BitcoinUtilities.decodeCompactBits(block.difficulty.toLong()),
            hash = block.hash
        )

//        val ctx = getMiningContext(prev.header, 100) {
//            blockStore.readBlock(it.previousBlock)?.header
//        }
        // TODO: test this algo against real data
//        if(getNextWorkRequired(prev.header, networkParameters, ctx) != block.difficulty) {
//            // bad difficulty
//            return false
//        }

        // TODO: contextually check block: validate median time past, validate keystones

        // write block on disk
        val index = blockStore.appendBlock(stored)

        // do fork resolution
        if (stored.work > blockStore.activeChain.tipWork) {
            // new block wins
            blockStore.activeChain.setTip(index, stored.work)
            SpvEventBus.newBestBlockEvent.trigger(block)
        }

        // add new block to a queue
        SpvEventBus.newBlockChannel.offer(block)

        return true
    }

    fun getPeerQuery(): List<VeriBlockBlock> {
        return blockStore
            .readChainWithTip(blockStore.activeChain.tip.smallHash, 100)
            .map { it.header }
            .filter { it.isKeystone() }
    }
}
