// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.api.dto

import org.veriblock.miners.pop.core.ApmOperation

data class MinerInfoResponse(
    val vbkAddress: String,
    val vbkBalance: Long
)

data class MineRequest(
    val chainSymbol: String,
    val height: Int? = null
)

data class OperationSummaryListResponse(
    val operations: List<OperationSummaryResponse>
)

data class OperationSummaryResponse(
    val operationId: String,
    val chainId: String,
    val endorsedBlockNumber: Int?,
    val state: String
)

fun ApmOperation.toSummaryResponse() = OperationSummaryResponse(
    id,
    chainId,
    blockHeight,
    state.toString()
)

data class OperationDetailResponse(
    val operationId: String,
    val chainId: String,
    val status: String,
    val blockHeight: Int?,
    val state: String,
    val stateDetail: List<String>
)

fun ApmOperation.toDetailedResponse() = OperationDetailResponse(
    id,
    chainId,
    status.name,
    blockHeight,
    state.toString(),
    state.getDetailedInfo()
)
