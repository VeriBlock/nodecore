// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.spv.model

import org.veriblock.core.crypto.PreviousBlockVbkHash
import org.veriblock.core.crypto.PreviousKeystoneVbkHash
import org.veriblock.core.crypto.MerkleRoot
import org.veriblock.core.crypto.TruncatedMerkleRoot
import org.veriblock.sdk.models.VeriBlockBlock

class FullBlock(
    height: Int,
    version: Short,
    previousBlock: PreviousBlockVbkHash,
    previousKeystone: PreviousKeystoneVbkHash,
    secondPreviousKeystone: PreviousKeystoneVbkHash,
    merkleRoot: TruncatedMerkleRoot,
    timestamp: Int,
    difficulty: Int,
    nonce: Long
) : VeriBlockBlock(
    height, version, previousBlock, previousKeystone, secondPreviousKeystone, merkleRoot, timestamp, difficulty, nonce
) {
    var normalTransactions: List<StandardTransaction>? = null

    var popTransactions: List<PopTransactionLight>? = null

    var metaPackage: BlockMetaPackage? = null
}
