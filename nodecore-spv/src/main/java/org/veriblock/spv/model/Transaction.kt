// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.spv.model

import nodecore.api.grpc.VeriBlockMessages.SignedTransaction
import org.veriblock.core.crypto.Sha256Hash
import org.veriblock.core.params.NetworkParameters

abstract class Transaction {
    lateinit var txId: Sha256Hash
    var inputAddress: AddressLight? = null
    var transactionMeta: TransactionMeta? = null
    var signature: ByteArray? = null
    var publicKey: ByteArray? = null

    constructor()

    constructor(txId: Sha256Hash) {
        this.txId = txId
        transactionMeta = TransactionMeta(txId)
    }

    abstract fun getOutputs(): List<Output>
    abstract fun getSignatureIndex(): Long
    abstract fun getTransactionFee(): Long
    abstract fun toByteArray(networkParameters: NetworkParameters): ByteArray?
    abstract fun getSignedMessageBuilder(networkParameters: NetworkParameters): SignedTransaction.Builder?

    abstract val data: ByteArray?
    abstract val transactionTypeIdentifier: TransactionTypeIdentifier

    fun addSignature(signature: ByteArray?, publicKey: ByteArray?) {
        this.signature = signature
        this.publicKey = publicKey
    }

}
