package org.nodecore.vpmmock.mockmining

import org.veriblock.core.crypto.Sha256Hash
import org.veriblock.sdk.models.BitcoinBlock
import java.math.BigInteger


data class BtcBlockIndex(
    val header: BitcoinBlock,
    val body: BitcoinBlockData,
    val work: BigInteger,
    val height: Int,
    // for fast access of previous block
    val prev: BtcBlockIndex? = null
) {
    val hash by lazy { header.hash }

    fun getAncestorAtHeight(height: Int): BtcBlockIndex? {
        if (height < 0 || height > this.height) {
            return null
        }

        // in O(n) seek backwards until we hit valid height
        var cursor: BtcBlockIndex? = this
        while (cursor != null && cursor.height > height) {
            cursor = cursor.prev
        }

        return cursor
    }

    override fun toString(): String {
        return "BtcBlockIndex(hash=${hash}, prev=${prev?.hash}, height=${height})"
    }
}
