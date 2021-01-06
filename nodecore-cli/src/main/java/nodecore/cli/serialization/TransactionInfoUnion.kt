// VeriBlock NodeCore CLI
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.cli.serialization

import nodecore.api.grpc.VeriBlockMessages
import nodecore.api.grpc.VeriBlockMessages.TransactionUnion

class TransactionInfoUnion {
    constructor(union: TransactionUnion) {
        when (union.transactionCase) {
            TransactionUnion.TransactionCase.UNSIGNED -> {
                type = "unsigned"
                unsigned = TransactionInfo(union.unsigned)
            }
            TransactionUnion.TransactionCase.SIGNED -> {
                type = "signed"
                signed = SignedTransactionInfo(union.signed)
            }
            TransactionUnion.TransactionCase.TRANSACTION_NOT_SET -> type = "unknown"
        }
    }

    constructor(union: VeriBlockMessages.TransactionInfoUnion) {
        when (union.transactionCase) {
            VeriBlockMessages.TransactionInfoUnion.TransactionCase.UNSIGNED -> {
                type = "unsigned"
                unsigned = TransactionInfo(union.unsigned.transaction)
            }
            VeriBlockMessages.TransactionInfoUnion.TransactionCase.SIGNED -> {
                type = "signed"
                signed = SignedTransactionInfo(union.signed)
            }
            VeriBlockMessages.TransactionInfoUnion.TransactionCase.TRANSACTION_NOT_SET -> type = "unknown"
        }
    }

    var type: String? = null
    var unsigned: TransactionInfo? = null
    var signed: SignedTransactionInfo? = null
}
