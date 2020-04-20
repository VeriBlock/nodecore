// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.api.dto

import com.papsign.ktor.openapigen.annotations.Request
import com.papsign.ktor.openapigen.annotations.Response
import org.veriblock.miners.pop.core.ApmOperation

@Response("Basic miner information")
data class MinerInfoResponse(
    val vbkAddress: String,
    val vbkBalance: Long
)

@Request("Mining request with target altchain symbol and optional endorsed block height")
data class MineRequest(
    val chainSymbol: String,
    val height: Int? = null
)

@Response("List of operations")
data class OperationSummaryListResponse(
    val operations: List<OperationSummaryResponse>
)

@Response("Basic mining operation information")
data class OperationSummaryResponse(
    val operationId: String,
    val chain: String,
    val endorsedBlockNumber: Int?,
    val state: String,
    val stateDescription: String
)

fun ApmOperation.toSummaryResponse() = OperationSummaryResponse(
    id,
    chain.name,
    endorsedBlockHeight,
    state.toString(),
    getStateDescription()
)

@Response("Detailed mining operation information")
data class OperationDetailResponse(
    val operationId: String,
    val chain: String,
    val status: String,
    val blockHeight: Int?,
    val state: String,
    val stateDetail: Map<String, String>
)

fun ApmOperation.toDetailedResponse() = OperationDetailResponse(
    id,
    chain.name,
    state.name,
    endorsedBlockHeight,
    state.description,
    getDetailedInfo()
)
