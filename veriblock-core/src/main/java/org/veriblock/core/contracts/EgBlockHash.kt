// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.core.contracts

import org.veriblock.core.crypto.AnyVbkHash
import org.veriblock.core.crypto.PreviousKeystoneVbkHash
import org.veriblock.core.crypto.asAnyVbkHash
import org.veriblock.core.utilities.extensions.asHexBytes
import java.util.Objects

/**
 * "Easy-going" hashes: these always return true when their last 9 bytes are equal.
 * Use case: BFI
 */
class EgBlockHash internal constructor(
    /**
     * The block hash, or a fraction of it
     */
    val hash: ByteArray
) {
    /**
     * The least common denominator (the last 9 bytes) of [hash]
     */
    val significantHash: ByteArray = hash.copyOfRange((hash.size - 9).coerceAtLeast(0), hash.size)

    /**
     * Returns true if this hash is a version of the other with more information
     * (they have the same common data of the last 9 bytes and it is longer)
     */
    infix fun isBetterThan(other: EgBlockHash?): Boolean {
        if (other == null) {
            return true
        }
        if (!equals(other)) {
            return false
        }
        return this.hash.size > other.hash.size
    }

    override fun toString(): String {
        return hash.toString()
    }

    /**
     * Two hashes with the same [.significantHash] are considered equal
     */
    override fun equals(other: Any?): Boolean {
        return other is EgBlockHash && significantHash.contentEquals(other.significantHash)
    }

    override fun hashCode(): Int {
        return Objects.hash(significantHash)
    }
}

fun ByteArray.asEgBlockHash(): EgBlockHash = EgBlockHash(this)
fun String.asEgBlockHash(): EgBlockHash = EgBlockHash(asHexBytes())
