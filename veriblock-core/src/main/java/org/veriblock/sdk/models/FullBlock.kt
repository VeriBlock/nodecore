package org.veriblock.sdk.models

import org.veriblock.core.crypto.PreviousBlockVbkHash
import org.veriblock.core.crypto.PreviousKeystoneVbkHash
import org.veriblock.core.crypto.TruncatedMerkleRoot
import org.veriblock.sdk.services.SerializeDeserializeService

class FullBlock(
    height: Int,
    version: Short,
    previousBlock: PreviousBlockVbkHash,
    previousKeystone: PreviousKeystoneVbkHash,
    secondPreviousKeystone: PreviousKeystoneVbkHash,
    merkleRoot: TruncatedMerkleRoot,
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
            SerializeDeserializeService.serialize(this).contentEquals(SerializeDeserializeService.serialize(other as VeriBlockBlock))
    }
}
