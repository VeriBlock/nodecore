// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.lite.core

import org.veriblock.sdk.models.Sha256Hash
import org.veriblock.sdk.models.VeriBlockMerklePath

class MerkleTree private constructor(
    private val merkleRoot: MerkleNode
) {
    fun getMerklePath(hash: Sha256Hash, index: Int?, isRegularTx: Boolean): VeriBlockMerklePath {
        val path = ArrayList<Sha256Hash>()
        search(hash, merkleRoot, path)

        return VeriBlockMerklePath(
            "${if (isRegularTx) 1 else 0}:$index:$hash:${path.joinToString(":")}"
        )
    }

    private fun search(hash: Sha256Hash, node: MerkleNode, path: MutableList<Sha256Hash>): Boolean {
        if (node.hash == hash) {
            return true
        }

        if (node.leaves != null) {
            if (search(hash, node.leaves.left, path)) {
                path.add(node.leaves.right.hash)
                return true
            }
            if (search(hash, node.leaves.right, path)) {
                path.add(node.leaves.left.hash)
                return true
            }
        }

        return false
    }


    private class MerkleNode(
        val hash: Sha256Hash,
        val leaves: MerkleNodeLeaves? = null
    )

    private class MerkleNodeLeaves(
        val left: MerkleNode,
        val right: MerkleNode
    )

    companion object {

        fun of(block: FullBlock): MerkleTree {

            val pop = block.popTransactions.map { MerkleNode(it.id) }
            val popTree = build(pop)

            val regular = block.normalTransactions.map { MerkleNode(it.id) }
            val regularTree = build(regular)

            return MerkleTree(
                MerkleNode(
                    block.merkleRoot,
                    MerkleNodeLeaves(
                        left = MerkleNode(block.metaPackage.hash),
                        right = build(listOf(popTree, regularTree))
                    )
                )
            )
        }

        private fun build(nodes: List<MerkleNode>): MerkleNode {
            if (nodes.isEmpty()) {
                return MerkleNode(Sha256Hash.ZERO_HASH)
            }
            if (nodes.size == 1) {
                return nodes[0]
            }

            val layer = ArrayList<MerkleNode>()
            var i = 0
            while (i < nodes.size) {
                val left = nodes[i]
                val right = if (i + 1 < nodes.size) nodes[i + 1] else left

                val node = MerkleNode(Sha256Hash.of(left.hash.bytes, right.hash.bytes), MerkleNodeLeaves(left, right))
                layer.add(node)
                i += 2
            }

            return build(layer)
        }
    }

}
