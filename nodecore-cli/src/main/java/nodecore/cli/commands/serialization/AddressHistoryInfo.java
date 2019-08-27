// VeriBlock NodeCore CLI
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.commands.serialization;

import com.google.gson.annotations.SerializedName;
import nodecore.api.grpc.AddressHistory;
import nodecore.api.grpc.TransactionUnion;
import org.veriblock.core.utilities.Utility;

import java.util.ArrayList;
import java.util.List;

public class AddressHistoryInfo {
    public AddressHistoryInfo(final AddressHistory history) {
        balance = Utility.formatAtomicLongWithDecimal(history.getBalance());
        blocksMined = history.getBlocksMined();
        for (final TransactionUnion union : history.getConfirmedTransactionsList()) {
            confirmedTransactions.add(new TransactionUnionInfo(union));
        }
        for (final TransactionUnion union : history.getPendingTransactionsList()) {
            pendingTransactions.add(new TransactionUnionInfo(union));
        }
    }

    public String balance;

    @SerializedName("blocks_mined")
    public int blocksMined;

    @SerializedName("confirmed_transactions")
    public List<TransactionUnionInfo> confirmedTransactions = new ArrayList<>();

    @SerializedName("pending_transactions")
    public List<TransactionUnionInfo> pendingTransactions = new ArrayList<>();
}
