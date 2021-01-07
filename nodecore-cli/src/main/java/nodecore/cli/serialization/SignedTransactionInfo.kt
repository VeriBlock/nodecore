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
import nodecore.api.grpc.utilities.extensions.toHex

class SignedTransactionInfo(
    val signature: String,
    @SerializedName("public_key")
    val publicKey: String?,
    @SerializedName("signature_index")
    val signatureIndex: Long?,
    val transaction: TransactionInfo?,
    @SerializedName("confirmations")
    val confirmations: Int = 0,
    @SerializedName("bitcoinConfirmations")
    val bitcoinConfirmations: Int = 0
)

fun SignedTransaction.toSignedTransactionInfo(): SignedTransactionInfo = SignedTransactionInfo(
    signatureIndex = signatureIndex,
    signature = signature.toHex(),
    publicKey = publicKey.toHex(),
    transaction = TransactionInfo(transaction)
)

fun VeriBlockMessages.SignedTransactionInfo.toModel(): SignedTransactionInfo = SignedTransactionInfo(
    signatureIndex = signatureIndex,
    signature = signature.toHex(),
    publicKey = publicKey.toHex(),
    transaction = TransactionInfo(transaction.transaction),
    confirmations = transaction.confirmations,
    bitcoinConfirmations = transaction.bitcoinConfirmations
)
