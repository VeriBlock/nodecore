// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.api.model

import nodecore.miners.pop.common.Utility
import nodecore.miners.pop.contracts.OperationSummary
import nodecore.miners.pop.contracts.PoPMiningInstruction
import nodecore.miners.pop.contracts.PreservedPoPMiningOperationState
import nodecore.miners.pop.contracts.result.MineResult
import nodecore.miners.pop.contracts.result.Result
import nodecore.miners.pop.contracts.result.ResultMessage

fun PreservedPoPMiningOperationState.toResponse() = OperationDetailResponse(
    operationId = operationId,
    status = status.name,
    currentAction = currentAction.name,
    popData = miningInstruction.toResponse(),
    transaction = transaction.mapHex(),
    submittedTransactionId = submittedTransactionId,
    bitcoinBlockHeaderOfProof = bitcoinBlockHeaderOfProof.mapHex(),
    bitcoinContextBlocks = bitcoinContextBlocks.mapHex(),
    merklePath = merklePath,
    alternateBlocksOfProof = alternateBlocksOfProof.mapHex(),
    detail = detail,
    popTransactionId = popTransactionId
)

private fun PoPMiningInstruction.toResponse() = PoPDataResponse(
    publicationData = publicationData.mapHex(),
    endorsedBlockHeader = endorsedBlockHeader.mapHex(),
    minerAddress = minerAddress.mapBase58()
)

fun OperationSummary.toResponse() = OperationSummaryResponse(
    operationId = operationId,
    endorsedBlockNumber = endorsedBlockNumber,
    state = state,
    action = action,
    message = message
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

private fun ByteArray?.mapHex() = this?.let { Utility.bytesToHex(it) }

private fun Collection<ByteArray?>?.mapHex() = this?.map { it.mapHex() }

private fun ByteArray?.mapBase58() = this?.let { Utility.bytesToBase58(it) }
