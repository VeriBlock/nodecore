// VeriBlock NodeCore CLI
// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.cli.serialization

import com.google.gson.annotations.SerializedName
import nodecore.api.grpc.VeriBlockMessages.AddressHistory
import org.veriblock.core.utilities.extensions.formatAtomicLongWithDecimal

class AddressHistoryInfo(
    history: AddressHistory
) {
    val balance: String = history.balance.formatAtomicLongWithDecimal()

    @SerializedName("blocks_mined")
    val blocksMined = history.blocksMined

    @SerializedName("confirmed_transactions")
    val confirmedTransactions = history.confirmedTransactionsList.map { transactionInfoUnion ->
        transactionInfoUnion.toModel()
    }

    @SerializedName("pending_transactions")
    val pendingTransactions = history.pendingTransactionsList.map { transactionInfoUnion ->
        transactionInfoUnion.toModel()
    }
}
