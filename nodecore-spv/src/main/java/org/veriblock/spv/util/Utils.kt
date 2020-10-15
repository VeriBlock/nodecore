// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.spv.util

import org.veriblock.core.utilities.Utility
import org.veriblock.core.crypto.Sha256Hash

object Utils {
    fun hash(left: Sha256Hash, right: Sha256Hash): Sha256Hash {
        val bytes = Sha256Hash.hash(
            Utility.concat(left.bytes, right.bytes)
        )
        return Sha256Hash.wrap(bytes, bytes.size)
    }

    fun matches(first: Sha256Hash, other: Sha256Hash): Boolean {
        val sharedLength = Math.min(first.bytes.size, other.bytes.size)
        for (i in 0 until sharedLength) {
            if (first.bytes[i] != other.bytes[i]) return false
        }
        return true
    }
}
