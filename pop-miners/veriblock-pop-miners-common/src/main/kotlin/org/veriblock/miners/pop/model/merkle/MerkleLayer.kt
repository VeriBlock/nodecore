// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.miners.pop.model.merkle

/**
 * A BitcoinMerkleLayer represents a layer in a BitcoinMerkleTree, and enables the access of elements (byte[] in
 * internal-endian order, not network-endian order) by their index.
 */
class MerkleLayer(
    private val data: Array<ByteArray>
) {
    /**
     * Creates the 'next' (higher, and half the size (round up if an odd number of data exist in this layer)) layer of
     * the Bitcoin merkle tree.
     *
     * @return The next layer of the Bitcoin merkle tree
     */
    fun createNextLayer(): MerkleLayer {
        // Create a 2D array for the new layer data that is half the size (round up, if fractional) of this layer's data
        val newData = Array(if (data.size % 2 == 0) data.size / 2 else data.size / 2 + 1) {
            // Element i of newData is SHA256D of the two corresponding elements beneath it.
            val first = data[it * 2]
            // If only one element is left, use it as both leaves.
            val second = if (data.size != it * 2 + 1) data[it * 2 + 1] else data[it * 2]
            Crypto().SHA256D(first + second)
        }
        return MerkleLayer(newData)
    }

    /**
     * Returns the number of elements in this layer
     *
     * @return the number of elements in this layer
     */
    fun numElementsInLayer(): Int {
        return data.size
    }

    /**
     * Returns the element at the provided index (elementNum)
     *
     * @param elementNum The index of the element to grab
     * @return A byte[], in internal order, of this layer's element at the provided index (elementNum)
     */
    fun getElement(elementNum: Int): ByteArray {
        return data[elementNum]
    }
}
