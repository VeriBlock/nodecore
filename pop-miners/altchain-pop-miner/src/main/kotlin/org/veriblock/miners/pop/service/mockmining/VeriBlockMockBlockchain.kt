// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.miners.pop.service.mockmining

import org.veriblock.sdk.blockchain.VeriBlockBlockchain
import org.veriblock.sdk.blockchain.store.BlockStore
import org.veriblock.sdk.blockchain.store.StoredBitcoinBlock
import org.veriblock.sdk.blockchain.store.StoredVeriBlockBlock
import org.veriblock.sdk.conf.VeriBlockNetworkParameters
import org.veriblock.sdk.models.Sha256Hash
import org.veriblock.sdk.models.VBlakeHash
import org.veriblock.sdk.models.VeriBlockBlock
import org.veriblock.sdk.services.ValidationService
import org.veriblock.sdk.util.Utils
import java.sql.SQLException
import java.util.ArrayList
import java.util.Collections
import java.util.HashMap

class VeriBlockMockBlockchain(
    networkParameters: VeriBlockNetworkParameters?,
    private val store: BlockStore<StoredVeriBlockBlock, VBlakeHash>,
    bitcoinStore: BlockStore<StoredBitcoinBlock?, Sha256Hash?>?
) : VeriBlockBlockchain(networkParameters, store, bitcoinStore) {
    private val blockDataStore: MutableMap<VBlakeHash, VeriBlockBlockData> = HashMap()

    @get:Throws(SQLException::class)
    private val previousKeystoneForNewBlock: VBlakeHash
        private get() {
            val chainHead = chainHead
            val blockHeight = chainHead.height + 1
            var keystoneBlocksAgo = blockHeight % 20
            when (keystoneBlocksAgo) {
                0 -> keystoneBlocksAgo = 20
                1 -> keystoneBlocksAgo = 21
            }
            val context = store[chainHead.hash, keystoneBlocksAgo]
            return if (context.size == keystoneBlocksAgo) context[keystoneBlocksAgo - 1].block.hash.trimToPreviousKeystoneSize() else VBlakeHash.EMPTY_HASH.trimToPreviousKeystoneSize()
        }

    @get:Throws(SQLException::class)
    private val secondPreviousKeystoneForNewBlock: VBlakeHash
        private get() {
            val chainHead = chainHead
            val blockHeight = chainHead.height + 1
            var keystoneBlocksAgo = blockHeight % 20
            when (keystoneBlocksAgo) {
                0 -> keystoneBlocksAgo = 20
                1 -> keystoneBlocksAgo = 21
            }
            keystoneBlocksAgo += 20
            val context = store[chainHead.hash, keystoneBlocksAgo]
            return if (context.size == keystoneBlocksAgo) context[keystoneBlocksAgo - 1].block.hash.trimToPreviousKeystoneSize() else VBlakeHash.EMPTY_HASH.trimToPreviousKeystoneSize()
        }

    // retrieve the blocks between lastKnownBlock and getChainHead()
    @Throws(SQLException::class)
    fun getContext(lastKnownBlock: VeriBlockBlock): List<VeriBlockBlock?> {
        val context: MutableList<VeriBlockBlock?> = ArrayList()

        // FIXME: using scanBestChain as a workaround as it should be faster in cached stores than a plain get
        var prevBlock = store.scanBestChain(chainHead.previousBlock)
        while (prevBlock != null && prevBlock.block != lastKnownBlock) {
            context.add(prevBlock.block)
            prevBlock = store.scanBestChain(prevBlock.block.previousBlock)
        }
        Collections.reverse(context)
        return context
    }

    @Throws(SQLException::class)
    fun mine(blockData: VeriBlockBlockData): VeriBlockBlock {
        val chainHead = chainHead
        val blockHeight = chainHead.height + 1
        val previousKeystone = previousKeystoneForNewBlock
        val secondPreviousKeystone = secondPreviousKeystoneForNewBlock
        val timestamp = Math.max(
            getNextEarliestTimestamp(chainHead.hash).orElse(0), Utils.getCurrentTimestamp()
        )
        val difficulty = getNextDifficulty(chainHead).asInt
        for (nonce in 0 until Int.MAX_VALUE) {
            val newBlock = VeriBlockBlock(
                blockHeight,
                chainHead.version,
                chainHead.hash.trimToPreviousBlockSize(),
                previousKeystone,
                secondPreviousKeystone,
                blockData.merkleRoot,
                timestamp,
                difficulty,
                nonce
            )
            if (ValidationService.isProofOfWorkValid(newBlock)) {
                add(newBlock)
                blockDataStore[newBlock.hash] = blockData
                return newBlock
            }
        }
        throw RuntimeException("Failed to mine the block due to too high difficulty")
    }

}
