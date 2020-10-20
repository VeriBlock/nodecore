package org.veriblock.spv.service

import org.veriblock.core.crypto.PreviousBlockVbkHash
import org.veriblock.sdk.blockchain.store.StoredVeriBlockBlock

const val KEYSTONE_INTERVAL = 20

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

    val isKeystone: Boolean
        get() {
            return height % KEYSTONE_INTERVAL == 0
        }

    val previousKeystone: BlockIndex?
        get() {
            // start with previous block
            var cursor = this.prev
            while (cursor != null && !cursor.isKeystone) {
                cursor = cursor.prev
            }
            return cursor
        }

    val secondPreviousKeystone: BlockIndex?
        get() {
            // start with previous block of previous keystone
            var cursor = this.previousKeystone?.prev
            while (cursor != null && !cursor.isKeystone) {
                cursor = cursor.prev
            }
            return cursor
        }
}
