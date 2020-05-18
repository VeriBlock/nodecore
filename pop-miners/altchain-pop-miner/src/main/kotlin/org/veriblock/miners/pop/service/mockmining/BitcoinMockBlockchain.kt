// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.miners.pop.service.mockmining

import org.veriblock.sdk.blockchain.BitcoinBlockchain
import org.veriblock.sdk.blockchain.store.BlockStore
import org.veriblock.sdk.blockchain.store.StoredBitcoinBlock
import org.veriblock.sdk.models.BitcoinBlock
import org.veriblock.core.crypto.Sha256Hash
import org.veriblock.core.params.BitcoinNetworkParameters
import org.veriblock.sdk.services.ValidationService
import org.veriblock.core.utilities.Utility
import java.sql.SQLException
import java.util.ArrayList
import java.util.Collections
import java.util.HashMap

class BitcoinMockBlockchain(
    networkParameters: BitcoinNetworkParameters,
    private val store: IndexedBitcoinBlockStore
) : BitcoinBlockchain(networkParameters, store) {
    private val blockDataStore: MutableMap<Sha256Hash, BitcoinBlockData> = HashMap()

    @Throws(SQLException::class)
    operator fun get(height: Int): BitcoinBlock? {
        val block = store[height]
        return block?.block
    }

    constructor(
        networkParameters: BitcoinNetworkParameters,
        store: BlockStore<StoredBitcoinBlock, Sha256Hash>
    ) : this(networkParameters, IndexedBitcoinBlockStore(store))

    // retrieve the blocks between lastKnownBlock and getChainHead()
    @Throws(SQLException::class)
    fun getContext(lastKnownBlock: BitcoinBlock): List<BitcoinBlock?> {
        val context: MutableList<BitcoinBlock?> = ArrayList()
        var prevBlock = get(chainHead.previousBlock)
        while (prevBlock != null && prevBlock != lastKnownBlock) {
            context.add(prevBlock)
            prevBlock = get(prevBlock.previousBlock)
        }
        Collections.reverse(context)
        return context
    }

    @Throws(SQLException::class)
    fun mine(blockData: BitcoinBlockData): BitcoinBlock {
        val chainHead = storedChainHead
        val timestamp = Math.max(
            getNextEarliestTimestamp(chainHead.hash).orElse(0), Utility.getCurrentTimestamp()
        )
        val difficulty = getNextDifficulty(timestamp, chainHead).asLong.toInt()
        for (nonce in 0 until Int.MAX_VALUE) {
            val newBlock = BitcoinBlock(
                chainHead.block.version,
                chainHead.hash,
                blockData.merkleRoot.reversed,
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
