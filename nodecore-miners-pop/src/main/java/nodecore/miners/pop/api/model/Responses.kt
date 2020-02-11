// VeriBlock NodeCore
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.api.model

class OperationDetailResponse(
    val operationId: String?,
    val status: String?,
    val currentAction: String?,
    val popData: PoPDataResponse,
    val transaction: String?,
    val submittedTransactionId: String?,
    val bitcoinBlockHeaderOfProof: String?,
    val bitcoinContextBlocks: List<String?>?,
    val merklePath: String?,
    val alternateBlocksOfProof: List<String?>?,
    val detail: String?,
    val popTransactionId: String?
)

class PoPDataResponse(
    val publicationData: String?,
    val endorsedBlockHeader: String?,
    val minerAddress: String?
)

class OperationSummaryListResponse(
    val operations: List<OperationSummaryResponse>
)

class OperationSummaryResponse(
    val operationId: String?,
    val endorsedBlockNumber: Int,
    val state: String?,
    val action: String?,
    val message: String?
)

open class ResultResponse(
    val failed: Boolean,
    val messages: List<ResultMessageResponse>
)

class ResultMessageResponse(
    val code: String?,
    val message: String?,
    val details: List<String>?,
    val error: Boolean
)

class MineResultResponse(
    failed: Boolean,
    messages: List<ResultMessageResponse>,
    val operationId: String? = null
) : ResultResponse(failed, messages)

class MinerInfoResponse(
    val minerAddress: String?,
    val bitcoinAddress: String?,
    val bitcoinBalance: Long?,
    val walletSeed: List<String>?
)
