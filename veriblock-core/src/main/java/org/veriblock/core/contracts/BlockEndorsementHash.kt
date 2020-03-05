// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.core.contracts

import org.veriblock.core.utilities.Utility
import java.util.Objects

class BlockEndorsementHash(
    /**
     * The block hash, or a fraction of it
     */
    val hash: String
) {
    /**
     * The least common denominator (the last 9 bytes) of [.hash]
     */
    val significantHash: String = extractSignificantHash(hash)

    /**
     * Ability to create from a byte array
     */
    constructor(hash: ByteArray) : this(Utility.bytesToHex(hash))

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
        return "BlockHash(hash='$hash', significantHash='$significantHash')"
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
