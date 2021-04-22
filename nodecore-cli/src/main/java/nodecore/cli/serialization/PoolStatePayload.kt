// VeriBlock NodeCore
// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.cli.serialization

import nodecore.api.grpc.RpcGetPoolStateReply
import nodecore.api.grpc.RpcPoolConfiguration
import nodecore.api.grpc.RpcPoolStats

class PoolStatePayload(
    message: RpcGetPoolStateReply
) {
    val running = message.running

    val configuration= if (RpcPoolConfiguration.getDefaultInstance() != message.configuration) {
        PoolConfigurationPayload(message.configuration)
    } else {
        null
    }

    val stats = if (RpcPoolStats.getDefaultInstance() != message.stats) {
        PoolStatsPayload(message.stats)
    } else {
        null
    }
}
