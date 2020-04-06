// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.miners.pop.model.merkle

import org.apache.commons.lang3.reflect.FieldUtils
import org.bitcoinj.core.PartialMerkleTree
import org.bitcoinj.core.Sha256Hash
import org.bitcoinj.core.Utils
import org.bitcoinj.core.VerificationException
import org.veriblock.core.utilities.createLogger
import org.veriblock.core.utilities.extensions.toHex
import java.util.Arrays
import java.util.HashMap

private val logger = createLogger {}

class MerkleProof(
    private val hashes: List<Sha256Hash>,
    private val matchedChildBits: ByteArray,
    private val transactionCount: Int
) {
    private val tree: Array<Array<Sha256Hash?>>
    private val positions: MutableMap<Sha256Hash, Int>

    init {
        var height = 0
        while (getTreeWidth(transactionCount, height) > 1) {
            height++
        }

        tree = Array(height) {
            val width = getTreeWidth(transactionCount, it)
            arrayOfNulls<Sha256Hash?>(width)
        }
        positions = HashMap()
        val used = ValuesUsed()

        recursiveExtractHashes(height, 0, used)

        if ((used.bitsUsed + 7) / 8 != matchedChildBits.size ||
            // verify that all hashes were consumed
            used.hashesUsed != hashes.size
        ) {
            throw VerificationException("Got a CPartialMerkleTree that didn't need all the data it provided")
        }
    }

    fun getCompactPath(txId: Sha256Hash): String {
        val txPosition = positions[txId]
            ?: error("Transaction $txId not found in merkle proof")

        val path = StringBuilder("$txPosition:${txId.reversedBytes.toHex()}")

        var pos = txPosition
        // Fill up the path with the corresponding nodes
        for (i in tree.indices) {
            var elementIndex = if (pos % 2 == 0) pos + 1 else pos - 1
            if (elementIndex == tree[i].size) {
                elementIndex -= 1
            }

            // Get the complementary element (left or right) at the next layer
            val element = get(i, elementIndex).reversedBytes.toHex()
            path.append(":$element")

            // Index in above layer will be floor(foundIndex / 2)
            pos /= 2
        }
        return path.toString()
    }

    private operator fun get(height: Int, offset: Int): Sha256Hash {
        val actualOffset = if (tree[height].size == offset) {
            offset - 1
        } else {
            offset
        }
        val element = tree[height][actualOffset]
        if (element != null) {
            return element
        }

        // Return a ZERO HASH because we can't descend any further
        return if (height == 0) {
            Sha256Hash.ZERO_HASH
        } else {
            val left = get(height - 1, actualOffset * 2).bytes
            val right = get(height - 1, actualOffset * 2 + 1).bytes
            combineLeftRight(left, right)
        }
    }

    // recursive function that traverses tree nodes, consuming the bits and hashes produced by TraverseAndBuild.
    // it returns the hash of the respective node.
    private fun recursiveExtractHashes(
        height: Int,
        pos: Int,
        used: ValuesUsed
    ): Sha256Hash {
        if (used.bitsUsed >= matchedChildBits.size * 8) {
            // overflowed the bits array - failure
            throw VerificationException("PartialMerkleTree overflowed its bits array")
        }
        val parentOfMatch = Utils.checkBitLE(matchedChildBits, used.bitsUsed++)
        return if (height == 0 || !parentOfMatch) {
            // if at height 0, or nothing interesting below, use stored hash and do not descend
            if (used.hashesUsed >= hashes.size) {
                // overflowed the hash array - failure
                throw VerificationException("PartialMerkleTree overflowed its hash array")
            }
            val hash = hashes[used.hashesUsed++]
            tree[height][pos] = hash
            if (height == 0 && parentOfMatch) {
                positions[hash] = pos
            }
            hash
        } else {
            // otherwise, descend into the subtrees to extract matched txids and hashes
            val left = recursiveExtractHashes(height - 1, pos * 2, used).bytes
            if (pos * 2 + 1 < getTreeWidth(transactionCount, height - 1)) {
                val right = recursiveExtractHashes(height - 1, pos * 2 + 1, used).bytes
                if (Arrays.equals(right, left)) {
                    throw VerificationException("Invalid merkle tree with duplicated left/right branches")
                }
                // and combine them before returning
                combineLeftRight(left, right)
            } else {
                // combine left with left if there's no right
                combineLeftRight(left, left)
            }
        }
    }

    companion object {
        @JvmStatic
        fun parse(partialMerkleTree: PartialMerkleTree): MerkleProof? {
            try {
                val hashes: List<Sha256Hash> = partialMerkleTree.getPrivateField("hashes")
                val bits: ByteArray = partialMerkleTree.getPrivateField("matchedChildBits")
                return MerkleProof(hashes, bits, partialMerkleTree.transactionCount)
            } catch (e: IllegalAccessException) {
                logger.error("Unable to parse Partial Merkle Tree", e)
            }
            return null
        }
    }
}

// Below code replicated from bitcoinj
// helper function to efficiently calculate the number of nodes at given height in the merkle tree
private fun getTreeWidth(transactionCount: Int, height: Int): Int {
    return transactionCount + (1 shl height) - 1 shr height
}

private fun combineLeftRight(left: ByteArray, right: ByteArray): Sha256Hash {
    return Sha256Hash.wrapReversed(
        Sha256Hash.hashTwice(
            Utils.reverseBytes(left), 0, 32, Utils.reverseBytes(right), 0, 32
        )
    )
}

private class ValuesUsed {
    var bitsUsed = 0
    var hashesUsed = 0
}

private inline fun <reified T> Any.getPrivateField(name: String): T =
    FieldUtils.readField(this, name, true) as T
