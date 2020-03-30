// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.lite.transactionmonitor

import org.veriblock.lite.core.TransactionMeta
import org.veriblock.sdk.models.Address
import org.veriblock.sdk.models.Coin
import org.veriblock.sdk.models.Output
import org.veriblock.sdk.models.PublicationData
import org.veriblock.sdk.models.Sha256Hash
import org.veriblock.sdk.models.VeriBlockMerklePath
import org.veriblock.sdk.models.VeriBlockTransaction

class WalletTransaction(
    type: Byte,
    sourceAddress: Address,
    sourceAmount: Coin,
    outputs: List<Output>,
    signatureIndex: Long,
    publicationData: PublicationData?,
    signature: ByteArray,
    publicKey: ByteArray,
    networkByte: Byte?,
    val transactionMeta: TransactionMeta
) : VeriBlockTransaction(
    type,
    sourceAddress,
    sourceAmount,
    outputs,
    signatureIndex,
    publicationData,
    signature,
    publicKey,
    networkByte
) {
    var merklePath: VeriBlockMerklePath? = null

    constructor(
        id: Sha256Hash, type: Byte, sourceAddress: Address, sourceAmount: Coin, outputs: List<Output>, signatureIndex: Long,
        publicationData: PublicationData?, signature: ByteArray, publicKey: ByteArray, networkByte: Byte?
    ) : this(
        type, sourceAddress, sourceAmount, outputs, signatureIndex, publicationData, signature, publicKey, networkByte, TransactionMeta(id)
    )

    companion object {
        fun wrap(tx: VeriBlockTransaction): WalletTransaction {
            return WalletTransaction(
                tx.id,
                tx.type,
                tx.sourceAddress,
                tx.sourceAmount,
                tx.outputs,
                tx.signatureIndex,
                tx.publicationData,
                tx.signature,
                tx.publicKey,
                tx.networkByte
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as WalletTransaction

        if (transactionMeta != other.transactionMeta) return false
        if (merklePath != other.merklePath) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + transactionMeta.hashCode()
        result = 31 * result + (merklePath?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "WalletTransaction(super=${super.toString()},transactionMeta=$transactionMeta, merklePath=$merklePath)"
    }
}
