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
import org.veriblock.core.miner.getMiningContext
import org.veriblock.core.miner.getNextWorkRequired
import org.veriblock.core.params.NetworkParameters
import org.veriblock.core.utilities.createLogger
import org.veriblock.sdk.blockchain.store.StoredVeriBlockBlock
import org.veriblock.sdk.models.VeriBlockBlock
import org.veriblock.sdk.services.ValidationService
import org.veriblock.spv.util.SpvEventBus
import java.io.IOException
import java.lang.IllegalStateException

private val logger = createLogger {}

class Blockchain(
    private val networkParameters: NetworkParameters,
    private val blockStore: BlockStore
) {
    val size: Int
        get() = this.blockStore.index.fileIndex.size

    fun getChainHead(): StoredVeriBlockBlock {
        try {
            return blockStore.getTip()
        } catch (e: Exception) {
            throw IllegalStateException("Can not get tip: $e")
        }
    }

    fun get(hash: AnyVbkHash): StoredVeriBlockBlock? {
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
        blockStore.writeBlock(stored)

        // do fork resolution
        if (stored.work > getChainHead().work) {
            // new block wins
            blockStore.setTip(stored)
            SpvEventBus.newBestBlockEvent.trigger(block)
        }

        // add new block to a queue
        SpvEventBus.newBlockChannel.offer(block)

        return true
    }

    fun getPeerQuery(): List<VeriBlockBlock> {
        return blockStore.readChainWithTip(getChainHead().hash, 100).map { it.header }.filter { it.isKeystone() }
    }
}
