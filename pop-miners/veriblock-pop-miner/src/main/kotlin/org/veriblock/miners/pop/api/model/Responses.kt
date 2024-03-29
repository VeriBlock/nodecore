// VeriBlock NodeCore
// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.api.model

import com.papsign.ktor.openapigen.annotations.Response

@Response("Detailed operation information")
class OperationDetailResponse(
    val operationId: String,
    val status: String,
    val currentAction: String,
    val detail: Map<String, String>,
    val createdAt: String
)

@Response("List of operations")
class OperationSummaryListResponse(
    val operations: List<OperationSummaryResponse>
)

@Response("Basic operation information")
class OperationSummaryResponse(
    val operationId: String,
    val endorsedBlockNumber: Int,
    val state: String,
    val action: String,
    val createdAt: String
)

@Response("List of result messages")
open class ResultResponse(
    val failed: Boolean,
    val messages: List<ResultMessageResponse>
)

@Response("Basic result message information")
class ResultMessageResponse(
    val code: String,
    val message: String,
    val details: List<String>,
    val error: Boolean
)

@Response("Mine operation result")
class MineResultResponse(
    failed: Boolean,
    messages: List<ResultMessageResponse>,
    val operationId: String
) : ResultResponse(failed, messages)

@Response("Basic miner information")
class MinerInfoResponse(
    val minerAddress: String?,
    val bitcoinAddress: String?,
    val bitcoinBalance: Long?,
    val walletSeed: List<String>?
)

@Response("The configured network")
data class NetworkInfoResponse(
    val name: String
)

@Response("The build version")
data class VersionResponse(
    val name: String
)

@Response("Auto mine configuration")
data class AutoMineConfigResponse(
    val round1: Boolean?,
    val round2: Boolean?,
    val round3: Boolean?,
    val round4: Boolean?
)

@Response("Btc fee configuration")
class BtcFeeConfigResponse(
    val maxFee: Long?,
    val feePerKB: Long?
)
