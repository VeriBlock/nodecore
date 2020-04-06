// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.miners.pop.model.merkle

import org.veriblock.core.utilities.extensions.asHexBytes
import org.veriblock.core.utilities.extensions.flip
import org.veriblock.core.utilities.extensions.isHex
import org.veriblock.core.utilities.extensions.isPositiveInteger
import org.veriblock.core.utilities.extensions.toHex

/**
 * A BitcoinMerklePath object represents the path, from a TxID to a Bitcoin merkle root.
 */
class BitcoinMerklePath {
    val layers: Array<ByteArray>
    val bottomData: ByteArray
    val bottomDataIndex: Int

    /**
     * Constructs a BitcoinMerklePath object with the provided layers in internal-endian.
     *
     * @param layers          The layers of the merkle path
     * @param bottomData      The TxID that this merkle path authenticates
     * @param bottomDataIndex The index of the bottomData TxID in the block it came from
     */
    constructor(layers: Array<ByteArray>, bottomData: ByteArray, bottomDataIndex: Int) {
        require(layers.isNotEmpty()) {
            "There must be a nonzero number of layers!"
        }
        for (layer in layers) {
            require(layer.size == 32) {
                "Every step of the tree must be a 256-bit number (32-length byte array)!"
            }
        }
        // Store the data
        this.layers = layers
        this.bottomData = bottomData
        this.bottomDataIndex = bottomDataIndex
    }

    /**
     * Constructs a BitcoinMerklePath object with the provided compact String representation
     */
    constructor(compactFormat: String) {
        val parts = compactFormat.split(":")
        require(parts.size >= 3) {
            "The compactFormat string must be in the format: \"bottomIndex:bottomData:layer0:...:layerN\""
        }
        require(parts[0].isPositiveInteger()) {
            "The compactFormat string must be in the format: \"bottomIndex:bottomData:layer0:...:layerN\""
        }
        for (i in 1 until parts.size) {
            require(!(parts[i].length != 64 || !parts[i].isHex())) {
                "The compactFormat string must be in the format: \"bottomIndex:bottomData:layer0:...:layerN\""
            }
        }
        bottomDataIndex = parts[0].toInt()
        bottomData = parts[1].asHexBytes()
        layers = Array(parts.size - 2) {
            parts[it + 2].asHexBytes()
        }
    }

    /**
     * Returns the Merkle root produced by following the layers up to the top of the tree.
     *
     * @return The Merkle root produced by following the path up to the top of the transaction tree, encoded in hexadecimal
     */
    fun getMerkleRoot(): String {
        val crypto = Crypto()
        var movingHash = ByteArray(32)
        // Account for the first layer's hash being an existing TxID
        System.arraycopy(bottomData, 0, movingHash, 0, 32)
        var layerIndex = bottomDataIndex
        for (layer in layers) {
            // Climb one layer up the tree by concatenating the current state with the next layer in the right order
            val first = if (layerIndex % 2 == 0) movingHash else layer
            val second = if (layerIndex % 2 == 0) layer else movingHash
            movingHash = crypto.SHA256D(first + second)
            // The position above on the tree will be floor(currentIndex / 2)
            layerIndex /= 2
        }
        return movingHash.flip().toHex()
    }

    /**
     * Returns a compact representation of this BitcoinMerklePath. For the purposes of alpha debugging, the path steps are stored in Hex.
     * Format: bottomIndex:bottomData:layer0:...:layerN
     *
     * @return A compact representation of this BitcoinmerklePath!
     */
    fun getCompactFormat(): String =
        "$bottomDataIndex:${bottomData.toHex()}:${layers.joinToString(":") { it.toHex() }}"
}
