// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.lite.wallet

import org.veriblock.lite.core.TransactionMeta
import org.veriblock.sdk.*

class WalletTransaction(
        type: Byte,
        sourceAddress: Address,
        sourceAmount: Coin,
        outputs: List<Output>,
        signatureIndex: Long,
        data: ByteArray,
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
        data,
        signature,
        publicKey,
        networkByte
) {
    var merklePath: VeriBlockMerklePath? = null

    constructor(
        id: Sha256Hash, type: Byte, sourceAddress: Address, sourceAmount: Coin, outputs: List<Output>, signatureIndex: Long, data: ByteArray,
        signature: ByteArray, publicKey: ByteArray, networkByte: Byte?
    ) : this(
        type, sourceAddress, sourceAmount, outputs, signatureIndex, data, signature, publicKey, networkByte, TransactionMeta(id)
    )

    companion object {
        fun wrap(tx: VeriBlockTransaction): WalletTransaction {
            return WalletTransaction(tx.id,
                    tx.type,
                    tx.sourceAddress,
                    tx.sourceAmount,
                    tx.outputs,
                    tx.signatureIndex,
                    tx.data,
                    tx.signature,
                    tx.publicKey,
                    tx.networkByte)
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
