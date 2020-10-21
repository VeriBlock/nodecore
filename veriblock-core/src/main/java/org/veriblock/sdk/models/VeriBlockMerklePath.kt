// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.sdk.models

import org.veriblock.core.crypto.Sha256Hash
import org.veriblock.core.utilities.SerializerUtility
import org.veriblock.sdk.util.writeSingleByteLengthValue
import org.veriblock.sdk.util.writeSingleIntLengthValue
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.nio.ByteBuffer

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
        layers: List<Sha256Hash>
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
        subject = Sha256Hash.wrap(parts[2])
        layers = (3 until parts.size).map {
            Sha256Hash.wrap(parts[it])
        }
        this.compactFormat = compactFormat
        merkleRoot = calculateVeriMerkleRoot()
    }

    private fun calculateVeriMerkleRoot(): Sha256Hash {
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
            cursor = Sha256Hash.of(first, second)

            // The position above on the tree will be floor(currentIndex / 2)
            layerIndex /= 2
        }
        return cursor
    }

    fun toCompactString(): String {
        return compactFormat
    }

    companion object {
        @JvmStatic
        fun parse(buffer: ByteBuffer): VeriBlockMerklePath {
            val treeIndex = SerializerUtility.readSingleBEValue(buffer, 4)
            val index = SerializerUtility.readSingleBEValue(buffer, 4)
            val subject = SerializerUtility.readSingleByteLenValue(buffer, Sha256Hash.BITCOIN_LENGTH, Sha256Hash.BITCOIN_LENGTH)
            val numLayers = SerializerUtility.readSingleBEValue(buffer, 4).toInt()
            SerializerUtility.checkRange(numLayers, 0, Constants.MAX_LAYER_COUNT_MERKLE,
                "Merkle path has $numLayers layers. Should contain at most ${Constants.MAX_LAYER_COUNT_MERKLE}")
            val layers = List(numLayers) {
                val layer = SerializerUtility.readSingleByteLenValue(buffer, Sha256Hash.BITCOIN_LENGTH, Sha256Hash.BITCOIN_LENGTH)
                Sha256Hash.wrap(layer)
            }
            return VeriBlockMerklePath(treeIndex.toInt(), index.toInt(), Sha256Hash.wrap(subject), layers)
        }
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

    fun serialize(): ByteArray {
        ByteArrayOutputStream().use { stream ->
            serialize(stream)
            return stream.toByteArray()
        }
    }

    fun serialize(stream: OutputStream) {
        stream.writeSingleIntLengthValue(treeIndex)
        stream.writeSingleIntLengthValue(index)
        stream.writeSingleByteLengthValue(subject.bytes)
        stream.writeSingleIntLengthValue(layers.size)
        for (hash in layers) {
            stream.writeSingleByteLengthValue(hash.bytes)
        }
    }
}
