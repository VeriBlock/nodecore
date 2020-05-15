// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.miners.pop.service.mockmining

import org.veriblock.core.crypto.Sha256Hash
import org.veriblock.sdk.models.VeriBlockMerklePath
import org.veriblock.sdk.models.VeriBlockPopTransaction
import org.veriblock.sdk.models.VeriBlockTransaction
import org.veriblock.sdk.services.SerializeDeserializeService
import java.util.ArrayList

class VeriBlockBlockData {
    abstract inner class Subtree<T> : ArrayList<T>() {
        abstract fun getSubject(index: Int): Sha256Hash
        val merkleRoot: Sha256Hash
            get() = calculateSubtreeHash(0, 0)

        // calculate the number of bits it takes to store size()
        private val maxDepth: Int
            private get() = (Math.log(size.toDouble()) / Math.log(2.0) + 1).toInt()

        // at each depth, there are 2**depth subtrees
        // leaves are at the depth equal to getMaxDepth()
        private fun calculateSubtreeHash(index: Int, depth: Int): Sha256Hash {
            return if (depth >= maxDepth) {
                if (index < size) getSubject(index) else Sha256Hash.of(ByteArray(0))
            } else Sha256Hash.of(
                calculateSubtreeHash(index * 2, depth + 1).bytes,
                calculateSubtreeHash(index * 2 + 1, depth + 1).bytes
            )
        }

        fun getMerkleLayers(index: Int): List<Sha256Hash> {
            if (index >= size) {
                throw IndexOutOfBoundsException("index must be less than size()")
            }
            val maxDepth = maxDepth
            var layerIndex = index

            // 2 layers will be added by get*MerklePath()
            val layers: MutableList<Sha256Hash> = ArrayList(
                maxDepth + 2
            )
            for (depth in maxDepth downTo 1) {
                // invert the last bit of layerIndex to reach the opposite subtree
                val layer = calculateSubtreeHash(layerIndex xor 1, depth)
                layers.add(layer)
                layerIndex /= 2
            }
            return layers
        }
    }

    inner class RegularSubtree : Subtree<VeriBlockTransaction?>() {
        override fun getSubject(index: Int): Sha256Hash {
            return SerializeDeserializeService.getId(get(index))
        }
    }

    inner class PoPSubtree : Subtree<VeriBlockPopTransaction?>() {
        override fun getSubject(index: Int): Sha256Hash {
            return SerializeDeserializeService.getId(get(index))
        }
    }

    val regularTransactions = RegularSubtree()
    val popTransactions = PoPSubtree()
    var blockContentMetapackage = ByteArray(0)
    val merkleRoot: Sha256Hash
        get() = Sha256Hash.of(
            Sha256Hash.of(blockContentMetapackage).bytes,
            Sha256Hash.of(
                regularTransactions.merkleRoot.bytes,
                popTransactions.merkleRoot.bytes
            ).bytes
        )

    fun getRegularMerklePath(index: Int): VeriBlockMerklePath {
        val subject = regularTransactions.getSubject(index)
        val layers: MutableList<Sha256Hash> = regularTransactions.getMerkleLayers(index).toMutableList()

        // the other transaction subtree
        layers.add(popTransactions.merkleRoot)
        layers.add(Sha256Hash.of(blockContentMetapackage))
        return VeriBlockMerklePath(0, index, subject, layers)
    }

    fun getPoPMerklePath(index: Int): VeriBlockMerklePath {
        val subject = popTransactions.getSubject(index)
        val layers: MutableList<Sha256Hash> = popTransactions.getMerkleLayers(index).toMutableList()

        // the other transaction subtree
        layers.add(regularTransactions.merkleRoot)
        layers.add(Sha256Hash.of(blockContentMetapackage))
        return VeriBlockMerklePath(1, index, subject, layers)
    }
}
