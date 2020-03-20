// VeriBlock NodeCore CLI
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.serialization;

import com.google.gson.annotations.SerializedName;
import nodecore.api.grpc.VeriBlockMessages;
import nodecore.api.grpc.utilities.ByteStringUtility;
import org.veriblock.core.utilities.Utility;

import java.util.ArrayList;
import java.util.List;

public class GetBlockTemplatePayload {
    public GetBlockTemplatePayload(final VeriBlockMessages.GetBlockTemplateReply reply) {
        target = reply.getTarget();
        sizeLimit = reply.getSizeLimit();
        blockHeight = reply.getBlockHeight();
        mutable.addAll(reply.getMutableList());
        coinbaseValue = Utility.formatAtomicLongWithDecimal(reply.getCoinbaseValue());
        minimumTimestamp = reply.getMinimumTimestamp();
        currentTimestamp = reply.getCurrentTimestamp();
        previousBlockHash = ByteStringUtility.byteStringToHex(reply.getPreviousBlockHash());
        for (final VeriBlockMessages.CandidateTransaction transaction : reply.getTransactionsList())
            transactions.add(new CandidateTransactionInfo(transaction));
    }

    public long target;

    @SerializedName("size_limit")
    public long sizeLimit;

    @SerializedName("block_height")
    public int blockHeight;

    @SerializedName("coinbase_value")
    public String coinbaseValue;

    @SerializedName("minimum_timestamp")
    public long minimumTimestamp;

    @SerializedName("current_timestamp")
    public long currentTimestamp;

    @SerializedName("previous_block_hash")
    public String previousBlockHash;

    public List<String> mutable = new ArrayList<>();

    public List<CandidateTransactionInfo> transactions = new ArrayList<>();

}
