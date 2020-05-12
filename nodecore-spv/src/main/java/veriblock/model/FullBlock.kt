// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package veriblock.model

import org.veriblock.core.crypto.Sha256Hash
import org.veriblock.sdk.models.VBlakeHash
import org.veriblock.sdk.models.VeriBlockBlock

class FullBlock(
    height: Int,
    version: Short,
    previousBlock: VBlakeHash?,
    previousKeystone: VBlakeHash?,
    secondPreviousKeystone: VBlakeHash?,
    merkleRoot: Sha256Hash?,
    timestamp: Int,
    difficulty: Int,
    nonce: Int
) : VeriBlockBlock(
    height, version, previousBlock, previousKeystone, secondPreviousKeystone, merkleRoot, timestamp, difficulty, nonce
) {
    var normalTransactions: List<StandardTransaction>? = null

    var popTransactions: List<PopTransactionLight>? = null

    var metaPackage: BlockMetaPackage? = null
}
