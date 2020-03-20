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

public class CoinbaseTransactionInfo {
    public CoinbaseTransactionInfo(final VeriBlockMessages.CoinbaseTransaction coinbaseTransaction)
    {
        coinbase_tx_hash = ByteStringUtility.byteStringToHex(coinbaseTransaction.getTxId());
        powCoinbaseAmount = Utility.formatAtomicLongWithDecimal(coinbaseTransaction.getPowCoinbaseAmount());
        popCoinbaseAmount = Utility.formatAtomicLongWithDecimal(coinbaseTransaction.getPopCoinbaseAmount());
        powFeeShare = Utility.formatAtomicLongWithDecimal(coinbaseTransaction.getPowFeeShare());
        popFeeShare = Utility.formatAtomicLongWithDecimal(coinbaseTransaction.getPopFeeShare());

        for (VeriBlockMessages.Output powOutput : coinbaseTransaction.getPowOutputsList()) {
            powOutputs.add(new OutputInfo(powOutput));
        }

        for (VeriBlockMessages.Output popOutput : coinbaseTransaction.getPopOutputsList()) {
            popOutputs.add(new OutputInfo(popOutput));
        }
    }

    @SerializedName("pow_coinbase_amount")
    public String powCoinbaseAmount;

    @SerializedName("pop_coinbase_amount")
    public String popCoinbaseAmount;

    @SerializedName("pow_fee_share")
    public String powFeeShare;

    @SerializedName("pop_fee_share")
    public String popFeeShare;

    @SerializedName("pow_outputs")
    public List<OutputInfo> powOutputs = new ArrayList<>();

    @SerializedName("pop_outputs")
    public List<OutputInfo> popOutputs = new ArrayList<>();

    public String coinbase_tx_hash;
}
