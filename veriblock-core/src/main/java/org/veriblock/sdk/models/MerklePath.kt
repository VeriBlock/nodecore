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
import org.veriblock.core.utilities.Utility
import org.veriblock.sdk.models.Constants.MAX_LAYER_COUNT_MERKLE
import org.veriblock.sdk.util.writeSingleByteLengthValue
import org.veriblock.sdk.util.writeSingleIntLengthValue
import org.veriblock.sdk.util.writeVariableLengthValue
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.nio.ByteBuffer

class MerklePath {
    private val compactFormat: String
    val layers: List<Sha256Hash>
    val subject: Sha256Hash
    val index: Int

    /**
     * The Merkle root produced by following the layers up to the top of the tree.
     */
    val merkleRoot: Sha256Hash

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
        merkleRoot = calculateMerkleRoot()
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
        merkleRoot = calculateMerkleRoot()
        this.compactFormat = compactFormat
    }

    private fun calculateMerkleRoot(): Sha256Hash {
        var layerIndex = index
        var cursor = subject
        for (layer in layers) {
            // Climb one layer up the tree by concatenating the current state with the next layer in the right order
            val first = if (layerIndex % 2 == 0) cursor.bytes else layer.bytes
            val second = if (layerIndex % 2 == 0) layer.bytes else cursor.bytes
            cursor = Sha256Hash.twiceOf(first, second)

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
            toCompactString() == (other as MerklePath).toCompactString()
    }

    override fun hashCode(): Int {
        return compactFormat.hashCode()
    }

    companion object {
        @JvmStatic
        fun parseRaw(buffer: ByteBuffer, subject: Sha256Hash): MerklePath {
            val index = SerializerUtility.readSingleBEValue(buffer, 4)
            val numLayers = SerializerUtility.readSingleBEValue(buffer, 4).toInt()
            SerializerUtility.checkRange(numLayers, 0, MAX_LAYER_COUNT_MERKLE,
                "Merkle path has $numLayers layers. Should contain at most $MAX_LAYER_COUNT_MERKLE")
            val sizeOfSizeBottomData = SerializerUtility.readSingleBEValue(buffer, 4)
            require(sizeOfSizeBottomData == 4L) {
                "Bad sizeOfSizeBottomData ($sizeOfSizeBottomData). Should be 4."
            }
            val sizeBottomData = buffer.int
            require(sizeBottomData == Sha256Hash.BITCOIN_LENGTH) {
                "Bad sizeBottomData ($sizeBottomData). Should be ${Sha256Hash.BITCOIN_LENGTH}."
            }
            val layers = List(numLayers) {
                val layer = SerializerUtility.readSingleByteLenValue(buffer, Sha256Hash.BITCOIN_LENGTH, Sha256Hash.BITCOIN_LENGTH)
                Sha256Hash.wrap(layer)
            }
            return MerklePath(index.toInt(), subject, layers)
        }

        @JvmStatic
        fun parse(buffer: ByteBuffer, subject: Sha256Hash): MerklePath {
            val bytes = SerializerUtility.readVariableLengthValueFromStream(buffer, 0, Constants.MAX_MERKLE_BYTES)
            return parseRaw(ByteBuffer.wrap(bytes), subject)
        }
    }

    fun serializeRaw(): ByteArray {
        ByteArrayOutputStream().use { stream ->
            serializeRaw(stream)
            return stream.toByteArray()
        }
    }

    fun serializeRaw(stream: OutputStream) {
        stream.writeSingleIntLengthValue(index)
        stream.writeSingleIntLengthValue(layers.size)
        val sizeBottomData = Utility.toByteArray(subject.length)
        // Write size of the int describing the size of the bottom layer of data
        stream.writeSingleIntLengthValue(sizeBottomData.size)
        stream.write(sizeBottomData)
        for (hash in layers) {
            val layer = hash.bytes
            stream.writeSingleByteLengthValue(layer)
        }
    }

    fun serialize(): ByteArray {
        ByteArrayOutputStream().use { stream ->
            serialize(stream)
            return stream.toByteArray()
        }
    }

    fun serialize(stream: OutputStream) {
        stream.writeVariableLengthValue(serializeRaw())
    }
}
