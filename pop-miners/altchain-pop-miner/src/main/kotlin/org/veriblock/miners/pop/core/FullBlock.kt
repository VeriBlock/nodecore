// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.core

import org.veriblock.core.crypto.PreviousBlockVbkHash
import org.veriblock.core.crypto.PreviousKeystoneVbkHash
import org.veriblock.core.crypto.Sha256Hash
import org.veriblock.sdk.models.VeriBlockBlock
import org.veriblock.sdk.models.VeriBlockPopTransaction
import org.veriblock.sdk.models.VeriBlockTransaction
import org.veriblock.sdk.services.SerializeDeserializeService
import java.util.Arrays

class FullBlock(
    height: Int,
    version: Short,
    previousBlock: PreviousBlockVbkHash,
    previousKeystone: PreviousKeystoneVbkHash,
    secondPreviousKeystone: PreviousKeystoneVbkHash,
    merkleRoot: Sha256Hash,
    timestamp: Int,
    difficulty: Int,
    nonce: Long,
    val normalTransactions: List<VeriBlockTransaction>,
    val popTransactions: List<VeriBlockPopTransaction>,
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
