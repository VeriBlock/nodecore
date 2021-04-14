// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.spv.util

import nodecore.api.grpc.RpcEvent
import java.util.concurrent.atomic.AtomicLong

private val identity = AtomicLong(0)

fun nextMessageId(): String {
    return identity.incrementAndGet().toString()
}

inline fun buildMessage(
    id: String = nextMessageId(),
    acknowledge: Boolean = false,
    buildBlock: RpcEvent.Builder.() -> Unit
): RpcEvent = RpcEvent.newBuilder()
    .setId(id)
    .setAcknowledge(acknowledge)
    .apply(buildBlock)
    .build()
