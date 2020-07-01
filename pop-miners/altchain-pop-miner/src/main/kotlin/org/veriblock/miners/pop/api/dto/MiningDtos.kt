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

@Request("Configuration change request")
class SetConfigRequest(
    val key: String?,
    val value: String?
)

@Response("List of operations")
data class OperationSummaryListResponse(
    val operations: List<OperationSummaryResponse>
)

@Response("Basic mining operation information")
data class OperationSummaryResponse(
    val operationId: String,
    val chain: String,
    val endorsedBlockHeight: Int?,
    val state: String,
    val task: String
)

fun ApmOperation.toSummaryResponse() = OperationSummaryResponse(
    id,
    chain.key,
    endorsedBlockHeight,
    state.toString(),
    getStateDescription()
)

@Response("Detailed mining operation information")
data class OperationDetailResponse(
    val operationId: String,
    val chain: String,
    val endorsedBlockHeight: Int?,
    val state: String,
    val task: String,
    val stateDetail: Map<String, String>
)

fun ApmOperation.toDetailedResponse() = OperationDetailResponse(
    id,
    chain.key,
    endorsedBlockHeight,
    state.name,
    state.taskName,
    getDetailedInfo()
)

@Response("Mining operation workflow")
data class OperationWorkflow(
    val operationId: String,
    val stages: List<OperationWorkflowStage>
)

@Response("Mining operation workflow stage")
data class OperationWorkflowStage(
    val status: String,
    val taskName: String,
    val extraInformation: String
)

@Response("Miner's configured chains")
data class ConfiguredAltchainList(
    val altchains: List<ConfiguredAltchain>
)

@Response("Miner's configured chain")
data class ConfiguredAltchain(
    val id: Long,
    val key: String,
    val name: String,
    val payoutDelay: Int
)
