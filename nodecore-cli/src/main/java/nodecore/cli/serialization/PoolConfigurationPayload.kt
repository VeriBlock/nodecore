// VeriBlock NodeCore
// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.cli.serialization

import com.google.gson.annotations.SerializedName
import nodecore.api.grpc.VeriBlockMessages.PoolConfiguration

class PoolConfigurationPayload(
    message: PoolConfiguration
) {
    val solo = message.solo

    val type = message.type

    @SerializedName("pool_target")
    val poolTarget = message.poolTarget

    @SerializedName("pool_address")
    val poolAddress = message.poolAddress

    @SerializedName("operator_address")
    val operatorAddress = message.operatorAddress

    @SerializedName("operator_fee")
    val operatorFee = message.operatorFee

    @SerializedName("coinbase_comment")
    val coinbaseComment = message.coinbaseComment
}
