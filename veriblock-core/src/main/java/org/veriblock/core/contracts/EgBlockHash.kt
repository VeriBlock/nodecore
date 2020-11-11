// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.core.contracts

import org.veriblock.core.utilities.extensions.asHexBytes
import org.veriblock.core.utilities.extensions.toHex
import java.util.Objects

/**
 * "Easy-going" hashes: these always return true when their last 9 bytes are equal.
 * Use case: BFI
 */
class EgBlockHash internal constructor(
    /**
     * The block hash, or a fraction of it
     */
    val bytes: ByteArray
) {
    /**
     * The least common denominator (the last 9 bytes) of [bytes]
     */
    val significantBytes: ByteArray = bytes.copyOfRange((bytes.size - 9).coerceAtLeast(0), bytes.size)

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
        return this.bytes.size > other.bytes.size
    }

    override fun toString(): String {
        return bytes.toHex()
    }

    /**
     * Two hashes with the same [.significantHash] are considered equal
     */
    override fun equals(other: Any?): Boolean {
        return other is EgBlockHash && significantBytes.contentEquals(other.significantBytes)
    }

    override fun hashCode(): Int {
        return Objects.hash(significantBytes)
    }
}

fun ByteArray.asEgBlockHash(): EgBlockHash = EgBlockHash(this)
fun String.asEgBlockHash(): EgBlockHash = EgBlockHash(asHexBytes())
