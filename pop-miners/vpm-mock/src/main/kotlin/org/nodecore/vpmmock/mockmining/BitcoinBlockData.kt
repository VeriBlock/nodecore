// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.nodecore.vpmmock.mockmining

import org.veriblock.core.crypto.MerkleRoot
import org.veriblock.sdk.models.MerklePath
import org.veriblock.core.crypto.Sha256Hash
import org.veriblock.core.crypto.asMerkleRoot
import org.veriblock.core.crypto.asSha256Hash
import org.veriblock.core.crypto.doubleSha256HashOf
import org.veriblock.core.utilities.extensions.flip
import java.util.ArrayList
import kotlin.math.ln

class BitcoinBlockData : ArrayList<ByteArray>() {
    val merkleRoot: MerkleRoot
        get() = calculateSubtreeHash(0, 0).bytes.flip().asMerkleRoot()

    // calculate the number of bits it takes to store size()
    private val maxDepth: Int
        get() = (ln(size.toDouble()) / ln(2.0) + 1).toInt()

    // at each depth, there are 2**depth subtrees
    // leaves are at the depth equal to getMaxDepth()
    private fun calculateSubtreeHash(index: Int, depth: Int): MerkleRoot {
        return if (depth >= maxDepth) {
            doubleSha256HashOf(if (index < size) get(index) else ByteArray(0))
        } else {
            doubleSha256HashOf(
                calculateSubtreeHash(index * 2, depth + 1).bytes,
                calculateSubtreeHash(index * 2 + 1, depth + 1).bytes
            )
        }.asMerkleRoot()
    }

    fun getMerklePath(index: Int): MerklePath {
        if (index >= size) throw IndexOutOfBoundsException("index must be less than size()")
        val maxDepth = maxDepth
        var layerIndex = index
        val subject = calculateSubtreeHash(index, maxDepth)
        val layers: MutableList<Sha256Hash> = ArrayList(
            maxDepth
        )
        for (depth in maxDepth downTo 1) {
            // invert the last bit of layerIndex to reach the opposite subtree
            val layer = calculateSubtreeHash(layerIndex xor 1, depth)
            layers.add(layer)
            layerIndex /= 2
        }
        return MerklePath(index, subject, layers)
    }
}
