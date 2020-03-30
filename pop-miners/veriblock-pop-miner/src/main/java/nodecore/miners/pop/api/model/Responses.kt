// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.api.model

class OperationDetailResponse(
    val operationId: String,
    val status: String,
    val currentAction: String,
    val detail: Any // Not defined as a map because doing so breaks the Swagger library
)

class OperationSummaryListResponse(
    val operations: List<OperationSummaryResponse>
)

class OperationSummaryResponse(
    val operationId: String,
    val endorsedBlockNumber: Int,
    val state: String,
    val action: String,
    val message: String
)

open class ResultResponse(
    val failed: Boolean,
    val messages: List<ResultMessageResponse>
)

class ResultMessageResponse(
    val code: String,
    val message: String,
    val details: List<String>,
    val error: Boolean
)

class MineResultResponse(
    failed: Boolean,
    messages: List<ResultMessageResponse>,
    val operationId: String
) : ResultResponse(failed, messages)

class MinerInfoResponse(
    val minerAddress: String?,
    val bitcoinAddress: String?,
    val bitcoinBalance: Long?,
    val walletSeed: List<String>?
)
