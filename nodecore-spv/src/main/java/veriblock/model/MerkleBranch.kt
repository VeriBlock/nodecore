// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package veriblock.model

import org.veriblock.core.crypto.Sha256Hash
import org.veriblock.sdk.models.MerklePath
import veriblock.util.Utils
import java.util.ArrayList
import java.util.stream.Collectors

class MerkleBranch(
    index: Int,
    subject: Sha256Hash?,
    merklePathHashes: List<Sha256Hash?>?,
    val isRegular: Boolean
) {
    private val merklePath: MerklePath = MerklePath(index, subject, merklePathHashes)

    val index: Int
        get() = merklePath.index

    val subject: Sha256Hash
        get() = merklePath.subject

    val merklePathHashes: List<Sha256Hash>
        get() = merklePath.layers

    fun verify(root: Sha256Hash): Boolean {
        var calculated = subject
        for (j in merklePathHashes.indices) {
            calculated = if (j == merklePathHashes.size - 1) {
                // Transactions always come from the right child
                Utils.hash(merklePathHashes[j], calculated)
            } else if (j == merklePathHashes.size - 2) {
                // Transaction root calculated is determined by the regular flag
                if (isRegular) {
                    Utils.hash(merklePathHashes[j], calculated)
                } else {
                    Utils.hash(calculated, merklePathHashes[j])
                }
            } else {
                val idx = merklePath.index shr j
                if (idx % 2 == 0) {
                    Utils.hash(calculated, merklePathHashes[j])
                } else {
                    Utils.hash(merklePathHashes[j], calculated)
                }
            }
        }
        return Utils.matches(calculated, root)
    }

    override fun toString(): String {
        return (
            sequenceOf(merklePath.subject) + merklePathHashes.asSequence()
        ).joinToString(
            separator = ":"
        )
    }
}
