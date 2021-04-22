// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.spv.model

import nodecore.api.grpc.RpcEvent
import org.veriblock.spv.net.SpvPeer

data class NetworkMessage(
    val sender: SpvPeer,
    val message: RpcEvent
)
