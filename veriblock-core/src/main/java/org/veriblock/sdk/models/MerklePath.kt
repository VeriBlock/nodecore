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

class MerklePath {
    private val compactFormat: String
    val layers: List<Sha256Hash>
    val subject: Sha256Hash
    val index: Int

    /**
     * The Merkle root produced by following the layers up to the top of the tree.
     */
    var merkleRoot: Sha256Hash

    constructor(
        index: Int,
        subject: Sha256Hash,
        layers: List<Sha256Hash>
    ) {
        this.index = index
        this.subject = subject
        this.layers = layers
        val layerStrings = layers.map { it.toString() }
        compactFormat = String.format("%d:%s:%s", index, subject.toString(), layerStrings.joinToString(":"))
        merkleRoot = MerklePathUtil.calculateMerkleRoot(index, subject, layers)
    }

    constructor(compactFormat: String) {
        val parts = compactFormat.split(":".toRegex()).toTypedArray()
        require(parts.size > 2 && parts[0].toInt() >= 0) {
            "Invalid merkle path: $compactFormat"
        }
        index = parts[0].toInt()
        subject = Sha256Hash.wrap(parts[1])
        layers = (2 until parts.size).map {
            Sha256Hash.wrap(parts[it])
        }
        merkleRoot = MerklePathUtil.calculateMerkleRoot(index, subject, layers)
        this.compactFormat = compactFormat
    }

    fun toCompactString(): String {
        return compactFormat
    }

    override fun equals(other: Any?): Boolean {
        return this === other || other != null && javaClass == other.javaClass &&
            toCompactString() == (other as MerklePath).toCompactString()
    }

    override fun hashCode(): Int {
        return compactFormat.hashCode()
    }
}
