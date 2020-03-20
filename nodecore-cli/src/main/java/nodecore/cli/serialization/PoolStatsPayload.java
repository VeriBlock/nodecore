// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.serialization;

import com.google.gson.annotations.SerializedName;
import nodecore.api.grpc.VeriBlockMessages;

public class PoolStatsPayload {
    @SerializedName("current_round")
    public int currentRound;
    @SerializedName("mining_block_number")
    public int miningBlockNumber;
    @SerializedName("last_block_number")
    public int lastBlockNumber;
    @SerializedName("recent_hash_rate")
    public String recentHashRate;

    public PoolStatsPayload(VeriBlockMessages.PoolStats message) {
        this.currentRound = message.getCurrentRound();
        this.miningBlockNumber = message.getMiningBlockNumber();
        this.lastBlockNumber = message.getLastBlockNumber();
        this.recentHashRate = message.getRecentHashRate();
    }
}
