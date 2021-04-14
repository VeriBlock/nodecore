// VeriBlock NodeCore CLI
// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.cli.serialization

import nodecore.api.grpc.RpcTransactionInfoUnion
import nodecore.api.grpc.RpcTransactionUnion

class TransactionInfoUnion(
    val type: String? = null,
    val unsigned: TransactionInfo? = null,
    val signed: SignedTransactionInfo? = null
)

fun RpcTransactionUnion.toTransactionInfoUnion() = when (transactionCase) {
    RpcTransactionUnion.TransactionCase.UNSIGNED -> TransactionInfoUnion(
        type = "unsigned",
        unsigned = TransactionInfo(unsigned)
    )
    RpcTransactionUnion.TransactionCase.SIGNED -> TransactionInfoUnion(
        type = "signed",
        signed = signed.toSignedTransactionInfo()
    )
    RpcTransactionUnion.TransactionCase.TRANSACTION_NOT_SET -> TransactionInfoUnion(
        type = "unknown"
    )
    else -> TransactionInfoUnion()
}

fun RpcTransactionInfoUnion.toModel() = when (transactionCase) {
    RpcTransactionInfoUnion.TransactionCase.UNSIGNED -> TransactionInfoUnion(
        type = "unsigned",
        unsigned = TransactionInfo(unsigned.transaction)
    )
    RpcTransactionInfoUnion.TransactionCase.SIGNED -> TransactionInfoUnion(
        type = "signed",
        signed = signed.toModel()
    )
    RpcTransactionInfoUnion.TransactionCase.TRANSACTION_NOT_SET -> TransactionInfoUnion(
        type = "unknown"
    )
    else -> TransactionInfoUnion()
}
