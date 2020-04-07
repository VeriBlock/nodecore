// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.miners.pop.model.merkle

import org.veriblock.core.utilities.extensions.asHexBytes
import org.veriblock.core.utilities.extensions.flip
import org.veriblock.core.utilities.extensions.toHex
import java.util.ArrayList

/**
 * The BitcoinMerkleTree class provides a variety of ways to interact with Bitcoin transaction merkle trees.
 * Bitcoin transaction merkle trees make proving a given transaction exists in a given block incredibly efficient, since
 * only the path up the tree (which grows logarithmically with relation to the number of total entries in the tree).
 *
 *
 * All methods in this code accept and return data in network-endian order. However, all appropriate endian switching
 * required (and by some considered to be a Bitcoin design flaw) is handled internally, so that data input into
 * a BitcoinMerkleTree and data returned from a BitcoinMerkleTree will always be consistent with the data as they
 * appear when querying Bitcoin using standard RPC.
 *
 *
 * Merkle tree calculation can be lazy or eager, but requesting a merkle path will, by necessity, require the entire
 * tree to be built, so it is generally advisable to request eager computation of the merkle tree, which stores it in a
 * local cache to speed up future queries. The first time a merkle path is requested, if the tree has not already been
 * computed, the tree will be computed and cached for subsequent access.
 */
class BitcoinMerkleTree(
    evaluateEagerly: Boolean,
    txIds: List<String>
) {
    private val layers = ArrayList<MerkleLayer>()
    private var builtTree = false

    /**
     * Construct a BitcoinMerkleTree given the provided List<String> of ordered txIDs.
     *
     * @param evaluateEagerly Whether or not to compute the entire tree now, or when next needed
     * @param txIDs           All of the TxIDs included in the transaction merkle tree from Bitcoin
     */
    init {
        val completeTxIds = if (txIds.size % 2 == 1) {
            txIds + txIds[txIds.size - 1]
        } else {
            txIds
        }

        // All of the data, in internal-endian-order, of the tree
        val floorData = Array(completeTxIds.size) {
            // Convert all of the TxIDs to byte[]s, flip them for the correct endianness
            completeTxIds[it].asHexBytes().flip()
        }
        // Create, at a minimum, the bottom floor
        layers.add(MerkleLayer(floorData))
        if (evaluateEagerly) {
            buildTree()
        }
    }

    private fun buildTree() {
        // When the top layer has a single element, the tree is finished
        while (layers[layers.size - 1].numElementsInLayer() > 1) {
            // Create the next layer, save it above the current highest layer
            layers.add(layers[layers.size - 1].createNextLayer())
        }

        // Tree is built, set this to true
        builtTree = true
    }

    /**
     * Returns the merkle root of this tree, in network-endian-order (as would be seen in Bitcoin-RPC responses).
     *
     * @return The merkle root of this tree, in network-endian-order (as would be seen in Bitcoin-RPC responses)
     */
    fun getMerkleRoot(): String {
        // Build the tree if it hasn't already been done
        if (!builtTree) {
            buildTree()
        }
        // Get the (only) element from the top layer, flip it, convert to hex
        return layers[layers.size - 1].getElement(0).flip().toHex()
    }

    /**
     * Returns the BitcoinMerklePath which allows a given txID, provided in network-order hexadecimal String format,
     * to be mapped up to the transaction merkle root.
     *
     * @param txID The transaction ID, in network-order hexadecimal String format, to get the merkle path for
     * @return The BitcoinMerklePath which allows the provided txID to be authenticated, null if this txID is not part of this transaction tree
     */
    fun getPathFromTxID(txID: String): BitcoinMerklePath? {
        var foundIndex = 0
        // The stored TxID will be in reversed-byte-order from the network-byte-order used by Bitcoin-RPC
        val txIDBytes = txID.asHexBytes().flip()
        val bottomLayer = layers[0]
        while (foundIndex < bottomLayer.numElementsInLayer()) {
            // Found a matching TxID in the bottom layer of the tree, where all TxIDs are stored
            if (bottomLayer.getElement(foundIndex).contentEquals(txIDBytes)) {
                break
            }
            foundIndex++
        }
        // The TxID provided is not part of this tree
        if (foundIndex == bottomLayer.numElementsInLayer()) {
            return null
        }
        // Save the index, since it'll be manipulated layer for calculating the correct corresponding node at each tree layer
        val indexAtBottom = foundIndex
        // Path will not include the merkle root
        val path = Array(layers.size - 1) {
            // Fill up the path with the corresponding nodes
            var elementIndex = if (foundIndex % 2 == 0) foundIndex + 1 else foundIndex - 1
            if (elementIndex == layers[it].numElementsInLayer()) {
                elementIndex -= 1
            }
            // Index in above layer will be floor(foundIndex / 2)
            foundIndex /= 2
            // Get the complementary element (left or right) at the next layer
            layers[it].getElement(elementIndex)
        }
        return BitcoinMerklePath(path, txIDBytes, indexAtBottom)
    }
}
