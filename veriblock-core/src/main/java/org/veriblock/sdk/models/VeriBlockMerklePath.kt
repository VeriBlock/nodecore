// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.sdk.models

import org.veriblock.core.crypto.Sha256Hash
import org.veriblock.sdk.util.MerklePathUtil

class VeriBlockMerklePath {
    val treeIndex: Int
    private val compactFormat: String
    val layers: List<Sha256Hash>
    val subject: Sha256Hash
    val index: Int

    /**
     * The Merkle root produced by following the layers up to the top of the tree.
     */
    var merkleRoot: Sha256Hash

    constructor(
        treeIndex: Int,
        index: Int,
        subject: Sha256Hash,
        layers: MutableList<Sha256Hash>
    ) {
        this.treeIndex = treeIndex
        this.index = index
        this.subject = subject
        this.layers = layers
        val layerStrings = layers.map { it.toString() }
        compactFormat = String.format("%d:%d:%s:%s", treeIndex, index, subject.toString(), layerStrings.joinToString(":"))
        merkleRoot = MerklePathUtil.calculateVeriMerkleRoot(this)
    }

    constructor(compactFormat: String) {
        val parts = compactFormat.split(":".toRegex()).toTypedArray()
        require(parts.size > 3 && parts[0].toInt() >= 0 && parts[1].toInt() >= 0) {
            "Invalid merkle path compact format: $compactFormat"
        }
        treeIndex = parts[0].toInt()
        index = parts[1].toInt()
        subject = Sha256Hash.wrap(parts[2])
        layers = (3 until parts.size).map {
            Sha256Hash.wrap(parts[it])
        }
        this.compactFormat = compactFormat
        merkleRoot = MerklePathUtil.calculateVeriMerkleRoot(this)
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
