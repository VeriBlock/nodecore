package org.veriblock.spv.service

import org.veriblock.core.crypto.PreviousBlockVbkHash
import org.veriblock.sdk.blockchain.store.StoredVeriBlockBlock

data class BlockIndex(
    val smallHash: PreviousBlockVbkHash,
    val position: Long,
    val height: Int,
    // for fast access of previous block
    val prev: BlockIndex? = null
) {
    fun getAncestorAtHeight(height: Int): BlockIndex? {
        if (height < 0 || height > this.height) {
            return null
        }

        // in O(n) seek backwards until we hit valid height
        var cursor: BlockIndex? = this
        while (cursor != null && cursor.height > height) {
            cursor = cursor.prev
        }

        return cursor
    }

    fun readBlock(store: BlockStore): StoredVeriBlockBlock? {
        return store.readBlock(position)
    }
}
