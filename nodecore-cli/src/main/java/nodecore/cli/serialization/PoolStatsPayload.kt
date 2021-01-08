// VeriBlock NodeCore
// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.cli.serialization

import com.google.gson.annotations.SerializedName
import nodecore.api.grpc.VeriBlockMessages

class PoolStatsPayload(
    message: VeriBlockMessages.PoolStats
) {
    @SerializedName("current_round")
    val currentRound = message.currentRound

    @SerializedName("mining_block_number")
    val miningBlockNumber = message.miningBlockNumber

    @SerializedName("last_block_number")
    val lastBlockNumber = message.lastBlockNumber

    @SerializedName("recent_hash_rate")
    val recentHashRate = message.recentHashRate
}
