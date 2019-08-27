// VeriBlock NodeCore CLI
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.commands.serialization;

import com.google.gson.annotations.SerializedName;
import nodecore.api.grpc.CandidateTransaction;
import nodecore.api.grpc.utilities.ByteStringUtility;

import java.util.ArrayList;
import java.util.List;

public class CandidateTransactionInfo {
    public CandidateTransactionInfo(final CandidateTransaction candidate) {
        fee = candidate.getFee();
        weight = candidate.getWeight();
        depends = candidate.getDependsList();
        required = candidate.getRequired();
        transaction = new TransactionUnionInfo(candidate.getTransaction());
        transactionId = ByteStringUtility.byteStringToHex(candidate.getTxId());
    }

    public long fee;

    public long weight;

    public boolean required;

    @SerializedName("transaction_id")
    public String transactionId;

    public TransactionUnionInfo transaction;

    public List<Integer> depends = new ArrayList<>();
}
