// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.serialization;

import com.google.gson.annotations.SerializedName;
import nodecore.api.grpc.VeriBlockMessages;

public class PoolConfigurationPayload {
    public boolean solo;
    public String type;

    @SerializedName("pool_target")
    public String poolTarget;
    @SerializedName("pool_address")
    public String poolAddress;
    @SerializedName("operator_address")
    public String operatorAddress;
    @SerializedName("operator_fee")
    public double operatorFee;
    @SerializedName("coinbase_comment")
    public String coinbaseComment;

    public PoolConfigurationPayload(VeriBlockMessages.PoolConfiguration message) {
        this.solo = message.getSolo();
        this.type = message.getType();
        this.poolTarget = message.getPoolTarget();
        this.poolAddress = message.getPoolAddress();
        this.operatorAddress = message.getOperatorAddress();
        this.operatorFee = message.getOperatorFee();
        this.coinbaseComment = message.getCoinbaseComment();
    }
}
