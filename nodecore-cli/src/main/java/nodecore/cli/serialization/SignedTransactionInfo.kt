// VeriBlock NodeCore CLI
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.cli.serialization

import com.google.gson.annotations.SerializedName
import nodecore.api.grpc.VeriBlockMessages
import nodecore.api.grpc.VeriBlockMessages.SignedTransaction
import nodecore.api.grpc.utilities.ByteStringUtility

class SignedTransactionInfo {
    constructor(signed: SignedTransaction) {
        signatureIndex = signed.signatureIndex
        signature = signed.signature.toHex()
        publicKey = signed.publicKey.toHex()
        transaction = TransactionInfo(signed.transaction)
    }

    constructor(signed: VeriBlockMessages.SignedTransactionInfo) {
        signatureIndex = signed.signatureIndex
        signature = signed.signature.toHex()
        publicKey = signed.publicKey.toHex()
        transaction = TransactionInfo(signed.transaction.transaction)
        confirmations = signed.transaction.confirmations
        bitcoinConfirmations = signed.transaction.bitcoinConfirmations
    }

    var signature: String?

    @SerializedName("public_key")
    var publicKey: String?

    @SerializedName("signature_index")
    var signatureIndex: Long?

    var transaction: TransactionInfo?

    @SerializedName("confirmations")
    var confirmations = 0

    @SerializedName("bitcoinConfirmations")
    var bitcoinConfirmations = 0
}
