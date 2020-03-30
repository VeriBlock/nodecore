// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.lite.core

import org.veriblock.sdk.models.Sha256Hash
import org.veriblock.sdk.models.VBlakeHash
import org.veriblock.sdk.models.VeriBlockBlock
import org.veriblock.sdk.models.VeriBlockPoPTransaction
import org.veriblock.sdk.models.VeriBlockTransaction
import org.veriblock.sdk.services.SerializeDeserializeService
import java.util.Arrays

class FullBlock(
    height: Int,
    version: Short,
    previousBlock: VBlakeHash,
    previousKeystone: VBlakeHash,
    secondPreviousKeystone: VBlakeHash,
    merkleRoot: Sha256Hash,
    timestamp: Int,
    difficulty: Int,
    nonce: Int,
    val normalTransactions: List<VeriBlockTransaction>,
    val popTransactions: List<VeriBlockPoPTransaction>,
    val metaPackage: BlockMetaPackage
) : VeriBlockBlock(
    height, version, previousBlock, previousKeystone, secondPreviousKeystone, merkleRoot, timestamp, difficulty, nonce
) {

    override fun equals(other: Any?): Boolean {
        return this === other || other != null &&
            (this.javaClass == other.javaClass || VeriBlockBlock::class.java == other.javaClass) &&
            Arrays.equals(SerializeDeserializeService.serialize(this), SerializeDeserializeService.serialize(other as VeriBlockBlock))
    }
}
