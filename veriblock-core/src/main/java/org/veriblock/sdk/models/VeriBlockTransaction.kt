// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.sdk.models

import org.veriblock.core.crypto.Sha256Hash
import org.veriblock.core.crypto.VbkTxId
import org.veriblock.core.utilities.Preconditions
import org.veriblock.sdk.services.SerializeDeserializeService
import java.util.Arrays
import java.util.Collections

open class VeriBlockTransaction(
    type: Byte,
    sourceAddress: Address,
    sourceAmount: Coin,
    outputs: List<Output>?,
    signatureIndex: Long,
    publicationData: PublicationData?,
    signature: ByteArray,
    publicKey: ByteArray,
    networkByte: Byte?
) {
    val id: VbkTxId
    val type: Byte
    val sourceAddress: Address
    val sourceAmount: Coin
    val outputs: List<Output>
    val signatureIndex: Long
    val publicationData: PublicationData?
    val signature: ByteArray
    val publicKey: ByteArray
    val networkByte: Byte?

    init {
        check(signatureIndex >= 0) {
            "Signature index must be positive"
        }
        check(signature.isNotEmpty()) {
            "Signature cannot be empty"
        }
        check(publicKey.isNotEmpty()) {
            "Public key cannot be empty"
        }
        this.type = type
        this.sourceAddress = sourceAddress
        this.sourceAmount = sourceAmount
        this.outputs = outputs ?: emptyList()
        this.signatureIndex = signatureIndex
        this.publicationData = publicationData
        this.signature = signature
        this.publicKey = publicKey
        this.networkByte = networkByte
        id = SerializeDeserializeService.getId(this)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val obj = other as VeriBlockTransaction
        return SerializeDeserializeService.getId(this) == SerializeDeserializeService.getId(obj) &&
            publicKey.contentEquals(obj.publicKey) &&
            signature.contentEquals(obj.signature)
    }
}
