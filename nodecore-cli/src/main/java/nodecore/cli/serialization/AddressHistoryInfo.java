// VeriBlock NodeCore CLI
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.serialization;

import com.google.gson.annotations.SerializedName;
import nodecore.api.grpc.VeriBlockMessages;
import org.veriblock.core.utilities.Utility;

import java.util.ArrayList;
import java.util.List;

public class AddressHistoryInfo {
    public AddressHistoryInfo(final VeriBlockMessages.AddressHistory history) {
        balance = Utility.formatAtomicLongWithDecimal(history.getBalance());
        blocksMined = history.getBlocksMined();
        for (final VeriBlockMessages.TransactionInfoUnion union : history.getConfirmedTransactionsList()) {
            confirmedTransactions.add(new TransactionInfoUnion(union));
        }
        for (final VeriBlockMessages.TransactionInfoUnion union : history.getPendingTransactionsList()) {
            pendingTransactions.add(new TransactionInfoUnion(union));
        }
    }

    public String balance;

    @SerializedName("blocks_mined")
    public int blocksMined;

    @SerializedName("confirmed_transactions")
    public List<TransactionInfoUnion> confirmedTransactions = new ArrayList<>();

    @SerializedName("pending_transactions")
    public List<TransactionInfoUnion> pendingTransactions = new ArrayList<>();
}
