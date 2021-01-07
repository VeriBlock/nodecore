// VeriBlock NodeCore CLI
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.cli.serialization

import nodecore.api.grpc.VeriBlockMessages
import nodecore.api.grpc.VeriBlockMessages.TransactionUnion

class TransactionInfoUnion(
    val type: String? = null,
    val unsigned: TransactionInfo? = null,
    val signed: SignedTransactionInfo? = null
)

fun TransactionUnion.toTransactionInfoUnion() = when (transactionCase) {
    TransactionUnion.TransactionCase.UNSIGNED -> TransactionInfoUnion(
        type = "unsigned",
        unsigned = TransactionInfo(unsigned)
    )
    TransactionUnion.TransactionCase.SIGNED -> TransactionInfoUnion(
        type = "signed",
        signed = signed.toSignedTransactionInfo()
    )
    TransactionUnion.TransactionCase.TRANSACTION_NOT_SET -> TransactionInfoUnion(
        type = "unknown"
    )
    else -> TransactionInfoUnion()
}

fun VeriBlockMessages.TransactionInfoUnion.toModel() = when (transactionCase) {
    VeriBlockMessages.TransactionInfoUnion.TransactionCase.UNSIGNED -> TransactionInfoUnion(
        type = "unsigned",
        unsigned = TransactionInfo(unsigned.transaction)
    )
    VeriBlockMessages.TransactionInfoUnion.TransactionCase.SIGNED -> TransactionInfoUnion(
        type = "signed",
        signed = signed.toModel()
    )
    VeriBlockMessages.TransactionInfoUnion.TransactionCase.TRANSACTION_NOT_SET -> TransactionInfoUnion(
        type = "unknown"
    )
    else -> TransactionInfoUnion()
}
