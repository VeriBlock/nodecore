// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.sdk.models

import org.veriblock.core.crypto.Sha256Hash
import org.veriblock.core.utilities.Preconditions
import org.veriblock.sdk.services.SerializeDeserializeService.getId
import java.util.ArrayList
import java.util.Arrays

class VeriBlockPopTransaction(
    val address: Address,
    val publishedBlock: VeriBlockBlock,
    val bitcoinTransaction: BitcoinTransaction,
    val merklePath: MerklePath,
    val blockOfProof: BitcoinBlock,
    val blockOfProofContext: List<BitcoinBlock>,
    var signature: ByteArray,
    var publicKey: ByteArray,
    val networkByte: Byte?
) {
    val id by lazy { getId(this) }

    init {
        check(signature.isNotEmpty()) {
            "Signature cannot be empty"
        }
        check(publicKey.isNotEmpty()) {
            "Public key cannot be empty"
        }
    }

    fun getBlocks(): List<BitcoinBlock> {
        return blockOfProofContext + blockOfProof
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val obj = other as VeriBlockPopTransaction
        return getId(this) == getId(obj) &&
            publicKey.contentEquals(obj.publicKey) &&
            signature.contentEquals(obj.signature)
    }
}
