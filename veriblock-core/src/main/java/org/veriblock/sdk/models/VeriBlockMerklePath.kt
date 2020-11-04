// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.sdk.models

import org.veriblock.core.crypto.MerkleRoot
import org.veriblock.core.crypto.Sha256Hash
import org.veriblock.core.crypto.merkleRootHashOf
import org.veriblock.core.crypto.asBtcHash
import org.veriblock.core.crypto.asMerkleRoot

class VeriBlockMerklePath {
    val treeIndex: Int
    private val compactFormat: String
    val layers: List<Sha256Hash>
    val subject: MerkleRoot
    val index: Int

    /**
     * The Merkle root produced by following the layers up to the top of the tree.
     */
    var merkleRoot: MerkleRoot

    constructor(
        treeIndex: Int,
        index: Int,
        subject: MerkleRoot,
        layers: MutableList<Sha256Hash>
    ) {
        this.treeIndex = treeIndex
        this.index = index
        this.subject = subject
        this.layers = layers
        val layerStrings = layers.map { it.toString() }
        compactFormat = String.format("%d:%d:%s:%s", treeIndex, index, subject.toString(), layerStrings.joinToString(":"))
        merkleRoot = calculateVeriMerkleRoot()
    }

    constructor(compactFormat: String) {
        val parts = compactFormat.split(":".toRegex()).toTypedArray()
        require(parts.size > 3 && parts[0].toInt() >= 0 && parts[1].toInt() >= 0) {
            "Invalid merkle path compact format: $compactFormat"
        }
        treeIndex = parts[0].toInt()
        index = parts[1].toInt()
        subject = parts[2].asMerkleRoot()
        layers = (3 until parts.size).map {
            parts[it].asBtcHash()
        }
        this.compactFormat = compactFormat
        merkleRoot = calculateVeriMerkleRoot()
    }

    private fun calculateVeriMerkleRoot(): MerkleRoot {
        var cursor = subject
        var layerIndex = index
        for (i in layers.indices) {
            // Because a layer has processed but the index (i) hasn't progressed, these values are offset by 1
            if (i == layers.size - 1) {
                // The last layer is the BlockContentMetapackage hash and will always be the "left" side,
                // so set the layerIndex to 1
                layerIndex = 1
            } else if (i == layers.size - 2) {
                // The second to last layer is the joining with the opposite transaction type group (normal vs pop),
                // so use the tree index specified in the compact format
                layerIndex = treeIndex
            }

            // Climb one layer up the tree by concatenating the current state with the next layer in the right order
            val first = if (layerIndex % 2 == 0) cursor.bytes else layers[i].bytes
            val second = if (layerIndex % 2 == 0) layers[i].bytes else cursor.bytes
            cursor = merkleRootHashOf(first, second)

            // The position above on the tree will be floor(currentIndex / 2)
            layerIndex /= 2
        }
        return cursor
    }

    fun toCompactString(): String {
        return compactFormat
    }

    override fun equals(other: Any?): Boolean {
        return this === other || other != null && javaClass == other.javaClass &&
            toCompactString() == (other as VeriBlockMerklePath).toCompactString()
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + treeIndex
        return result
    }
}
