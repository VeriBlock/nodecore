// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.core.contracts

import org.veriblock.core.utilities.extensions.toHex
import java.util.Objects

class BlockEndorsementHash(
    hash: String
) {
    /**
     * The block hash, or a fraction of it
     */
    val hash: String = hash.toUpperCase()

    /**
     * The least common denominator (the last 9 bytes) of [.hash]
     */
    val significantHash: String = extractSignificantHash(hash.toUpperCase())

    /**
     * Ability to create from a byte array
     */
    constructor(hash: ByteArray) : this(hash.toHex())

    /**
     * Returns true if this hash is a version of the other with more information
     * (they have the same common data of the last 9 bytes and it is longer)
     */
    infix fun isBetterThan(other: BlockEndorsementHash?): Boolean {
        if (other == null) {
            return true
        }
        if (!equals(other)) {
            return false
        }
        return this.hash.length > other.hash.length
    }

    /**
     * Extracts the least common denominator (the last 9 bytes) from the given hash
     */
    private fun extractSignificantHash(hash: String): String {
        return if (hash.length <= 18) hash else hash.substring(hash.length - 18)
    }

    override fun toString(): String {
        val extraHash = hash.removeSuffix(significantHash)
        val extraHashPrefix = if (extraHash.isNotEmpty()) "$extraHash|" else ""
        return "[$extraHashPrefix$significantHash]"
    }

    /**
     * Two hashes with the same [.significantHash] are considered equal
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is BlockEndorsementHash) {
            return false
        }
        return significantHash == other.significantHash
    }

    override fun hashCode(): Int {
        return Objects.hash(significantHash)
    }
}
