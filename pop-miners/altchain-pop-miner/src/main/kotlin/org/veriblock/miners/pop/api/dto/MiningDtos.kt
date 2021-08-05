// VeriBlock NodeCore
// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.api.dto

import com.papsign.ktor.openapigen.annotations.Request
import com.papsign.ktor.openapigen.annotations.Response
import java.time.format.DateTimeFormatter
import org.veriblock.miners.pop.core.ApmOperation
import org.veriblock.sdk.models.StateInfo

@Response("Basic miner information")
data class MinerInfoResponse(
    val vbkAddress: String,
    val vbkBalance: Long,
    val status: MinerStatusResponse
)

@Response("Basic miner status")
data class MinerStatusResponse(
    val isReady: Boolean,
    val reason: String?
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
    val operations: List<OperationSummaryResponse>,
    val totalCount: Int
)

@Response("Basic mining operation information")
data class OperationSummaryResponse(
    val operationId: String,
    val chain: String,
    val endorsedBlockHeight: Int?,
    val state: String,
    val task: String,
    val createdAt: String
)

fun ApmOperation.toSummaryResponse() = OperationSummaryResponse(
    operationId = id,
    chain = chain.key,
    endorsedBlockHeight = endorsedBlockHeight,
    state = state.toString(),
    task = getStateDescription(),
    createdAt = DateTimeFormatter.ISO_INSTANT.format(createdAt)
)

@Response("Detailed mining operation information")
data class OperationDetailResponse(
    val operationId: String,
    val chain: String,
    val endorsedBlockHeight: Int?,
    val state: String,
    val task: String,
    val stateDetail: Map<String, String>,
    val createdAt: String
)

fun ApmOperation.toDetailedResponse() = OperationDetailResponse(
    operationId = id,
    chain = chain.key,
    endorsedBlockHeight = endorsedBlockHeight,
    state = state.name,
    task = state.taskName,
    stateDetail = getDetailedInfo(),
    createdAt = DateTimeFormatter.ISO_INSTANT.format(createdAt)
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
    val payoutDelay: Int,
    val syncStatus: AltChainSyncStatusResponse,
    val readyStatus: AltChainReadyStatusResponse,
    val explorerBaseUrls: ExplorerBaseUrlsResponse
)

data class AltChainReadyStatusResponse(
    val isReady: Boolean,
    val reason: String?
)

data class AltChainSyncStatusResponse(
    val networkHeight: Int,
    val localBlockchainHeight: Int
)

@Request("A request to withdraw VBKs to address")
data class WithdrawRequest(
    val amount: String,
    val destinationAddress: String
)

@Response("Withdraw transaction ids")
data class WithdrawResponse(
    val ids: List<String>
)

@Response("The configured network")
data class NetworkInfoResponse(
    val name: String,
    val explorerBaseUrls: ExplorerBaseUrlsResponse
)

@Response("The build version")
data class VersionResponse(
    val name: String
)

fun StateInfo?.toAltChainSyncStatusResponse(): AltChainSyncStatusResponse = AltChainSyncStatusResponse(
    networkHeight = this?.networkHeight ?: 0,
    localBlockchainHeight = this?.localBlockchainHeight ?: 0
)
