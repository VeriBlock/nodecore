// VeriBlock NodeCore CLI
// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.cli.serialization

import nodecore.api.grpc.VeriBlockMessages

class WalletTransactionsPayload(
    list: List<VeriBlockMessages.WalletTransaction>
) {
    val transactions = list.map { walletTransaction ->
        WalletTransactionInfo(walletTransaction)
    }
}
