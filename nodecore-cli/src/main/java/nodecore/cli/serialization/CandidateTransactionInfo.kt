// VeriBlock NodeCore CLI
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.cli.serialization

import com.google.gson.annotations.SerializedName
import nodecore.api.grpc.VeriBlockMessages.CandidateTransaction
import nodecore.api.grpc.utilities.ByteStringUtility
import nodecore.api.grpc.utilities.extensions.toHex

class CandidateTransactionInfo(
    candidate: CandidateTransaction
) {
    val fee = candidate.fee

    val weight = candidate.weight

    val required = candidate.required

    @SerializedName("transaction_id")
    val transactionId = candidate.txId.toHex()

    val transaction = candidate.transaction.toTransactionInfoUnion()

    val depends = candidate.dependsList
}
