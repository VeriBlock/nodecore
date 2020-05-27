// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.miners.pop.service.mockmining

import org.veriblock.sdk.blockchain.store.BlockStore
import org.veriblock.sdk.blockchain.store.StoredBitcoinBlock
import org.veriblock.sdk.models.BlockStoreException
import org.veriblock.core.crypto.Sha256Hash
import java.sql.SQLException
import java.util.HashMap

// A stopgap store wrapper that implements get(height) on top of BlockStore interface
class IndexedBitcoinBlockStore(
    private val store: BlockStore<StoredBitcoinBlock, Sha256Hash>
) : BlockStore<StoredBitcoinBlock, Sha256Hash> {
    private val blockByHeightIndex: MutableMap<Int, StoredBitcoinBlock> = HashMap()

    init {
        //FIXME: read and index the store
    }

    override fun shutdown() {
        store.shutdown()
    }

    @Throws(SQLException::class)
    override fun clear() {
        store.clear()
        blockByHeightIndex.clear()
    }

    @Throws(BlockStoreException::class, SQLException::class)
    override fun getChainHead(): StoredBitcoinBlock? {
        return store.getChainHead()
    }

    @Throws(BlockStoreException::class, SQLException::class)
    override fun setChainHead(chainHead: StoredBitcoinBlock): StoredBitcoinBlock? {
        return store.setChainHead(chainHead)
    }

    @Throws(BlockStoreException::class, SQLException::class)
    override fun put(block: StoredBitcoinBlock) {
        store.put(block)
        blockByHeightIndex[block.height] = block
    }

    @Throws(BlockStoreException::class, SQLException::class)
    override operator fun get(hash: Sha256Hash): StoredBitcoinBlock? {
        return store.get(hash)
    }

    @Throws(BlockStoreException::class, SQLException::class)
    operator fun get(height: Int): StoredBitcoinBlock? {
        return blockByHeightIndex[height]
    }

    @Throws(BlockStoreException::class, SQLException::class)
    override fun erase(hash: Sha256Hash): StoredBitcoinBlock? {
        val erased = store.erase(hash)
        if (erased != null) {
            blockByHeightIndex.remove(erased.height)
        }
        return erased
    }

    @Throws(BlockStoreException::class, SQLException::class)
    override fun replace(hash: Sha256Hash, block: StoredBitcoinBlock): StoredBitcoinBlock? {
        return store.replace(hash, block)
    }

    @Throws(BlockStoreException::class, SQLException::class)
    override operator fun get(hash: Sha256Hash, count: Int): List<StoredBitcoinBlock> {
        return store.get(hash, count)
    }

    @Throws(BlockStoreException::class, SQLException::class)
    override fun getFromChain(hash: Sha256Hash, blocksAgo: Int): StoredBitcoinBlock? {
        return store.getFromChain(hash, blocksAgo)
    }

    @Throws(BlockStoreException::class, SQLException::class)
    override fun scanBestChain(hash: Sha256Hash): StoredBitcoinBlock? {
        return store.scanBestChain(hash)
    }

}
