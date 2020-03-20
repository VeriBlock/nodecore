// VeriBlock NodeCore CLI
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.serialization;

import com.google.gson.annotations.SerializedName;
import com.opencsv.bean.CsvBindByName;
import nodecore.api.grpc.VeriBlockMessages;
import org.veriblock.core.utilities.Utility;

public class GetInfoPayload {

    public GetInfoPayload(final VeriBlockMessages.GetInfoReply reply) {
        decoded_difficulty = reply.getDecodedDifficulty();
        numberOfBlocks = reply.getNumberOfBlocks();
        transactionFee = Utility.formatAtomicLongWithDecimal(reply.getTransactionFee());
        lastBlock = new BlockSummaryInfo(reply.getLastBlock());
        estimatedHashRate = reply.getEstimatedHashrate();
        defaultAddress = new AddressBalanceInfo(reply.getDefaultAddress());
    }

    @CsvBindByName
    @SerializedName("default_address")
    public AddressBalanceInfo defaultAddress;

    @CsvBindByName
    @SerializedName("estimated_hash_rate")
    public long estimatedHashRate;

    @CsvBindByName
    @SerializedName("number_of_blocks")
    public int numberOfBlocks;

    @CsvBindByName
    @SerializedName("transaction_fee")
    public String transactionFee;

    @SerializedName("last_block")
    public BlockSummaryInfo lastBlock;

    @CsvBindByName
    @SerializedName("decoded_difficulty")
    public long decoded_difficulty;

    public AddressBalanceInfo getDefaultAddress() {
        return defaultAddress;
    }

    public long getEstimatedHashRate() {
        return estimatedHashRate;
    }

    public int getNumberOfBlocks() {
        return numberOfBlocks;
    }

    public String getTransactionFee() {
        return transactionFee;
    }

    public BlockSummaryInfo getLastBlock() {
        return lastBlock;
    }

    public long getDecoded_difficulty() {
        return decoded_difficulty;
    }
}
