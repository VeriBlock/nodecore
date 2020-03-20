// VeriBlock NodeCore CLI
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.serialization;

import nodecore.api.grpc.VeriBlockMessages;

import java.util.ArrayList;
import java.util.List;

public class WalletTransactionsPayload {
    public WalletTransactionsPayload(final List<VeriBlockMessages.WalletTransaction> list) {
        for (final VeriBlockMessages.WalletTransaction transaction : list)
            transactions.add(new WalletTransactionInfo(transaction));
    }

    public List<WalletTransactionInfo> transactions = new ArrayList<>();
}
