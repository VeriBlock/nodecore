// VeriBlock NodeCore
// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.api.model

import java.time.format.DateTimeFormatter
import org.veriblock.miners.pop.core.VpmOperation
import org.veriblock.miners.pop.model.OperationSummary
import org.veriblock.miners.pop.model.result.MineResult
import org.veriblock.miners.pop.model.result.Result
import org.veriblock.miners.pop.model.result.ResultMessage

fun VpmOperation.toResponse() = OperationDetailResponse(
    operationId = id,
    status = state.name,
    currentAction = state.taskName,
    detail = getDetailedInfo(),
    createdAt = createdAt.format(DateTimeFormatter.ISO_DATE_TIME)
)

fun OperationSummary.toResponse() = OperationSummaryResponse(
    operationId = operationId,
    endorsedBlockNumber = endorsedBlockNumber,
    state = status,
    action = action,
    createdAt =  createdAt.format(DateTimeFormatter.ISO_DATE_TIME)
)

fun Result.toResponse() = ResultResponse(
    failed = didFail(),
    messages = messages.map { it.toResponse() }
)

fun MineResult.toResponse() = MineResultResponse(
    operationId = operationId,
    failed = didFail(),
    messages = messages.map { it.toResponse() }
)


fun ResultMessage.toResponse() = ResultMessageResponse(
    code = code,
    message = message,
    details = details,
    error = isError
)
