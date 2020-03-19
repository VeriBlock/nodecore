// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.miners.pop.model.dto

import com.google.gson.annotations.SerializedName
import org.veriblock.core.crypto.Crypto
import org.veriblock.core.utilities.extensions.toHex

class PopOperationInfoDto(state: PopMiningOperationStateDto) {
    @SerializedName("operation_id")
    val operationId: String

    @SerializedName("status")
    val status: String?

    @SerializedName("detail")
    val detail: String

    @SerializedName("current_action")
    val currentAction: String?

    @SerializedName("pop_publication_data")
    val popPublicationData: String?

    @SerializedName("endorsed_block_header")
    val endorsedBlockHeader: String?

    @SerializedName("miner_address")
    val minerAddress: String?

    @SerializedName("endorsed_block_hash")
    val endorsedBlockHash: String?

    @SerializedName("bitcoin_transaction")
    val bitcoinTransaction: String?

    @SerializedName("bitcoin_transaction_id")
    val bitcoinTransactionId: String

    @SerializedName("bitcoin_block_header_of_proof")
    val bitcoinBlockHeaderOfProof: String?

    @SerializedName("bitcoin_context_blocks")
    val bitcoinContextBlocks: List<String>?

    @SerializedName("bitcoin_merkle_path")
    val bitcoinMerklePath: String

    @SerializedName("alternate_blocks_of_proof")
    val alternateBlocksOfProof: List<String>?

    @SerializedName("pop_transaction_id")
    val popTransactionId: String

    init {
        operationId = state.operationId
        status = if (state.status != null) state.status.toString() else null
        detail = state.detail
        currentAction = if (state.currentAction != null) state.currentAction.toString() else null
        if (state.miningInstruction != null) {
            popPublicationData = state.miningInstruction.publicationData.toHex()
            endorsedBlockHeader = state.miningInstruction.endorsedBlockHeader.toHex()
            minerAddress = state.miningInstruction.minerAddress
            endorsedBlockHash = Crypto().vBlakeReturnHex(state.miningInstruction.endorsedBlockHeader)
        } else {
            popPublicationData = null
            endorsedBlockHeader = null
            minerAddress = null
            endorsedBlockHash = null
        }
        bitcoinTransaction = state.transaction?.toHex()
        bitcoinTransactionId = state.submittedTransactionId
        bitcoinBlockHeaderOfProof = state.bitcoinBlockHeaderOfProof?.toHex()
        bitcoinContextBlocks = state.bitcoinContextBlocks?.map { it.toHex() }
        bitcoinMerklePath = state.merklePath
        alternateBlocksOfProof = state.alternateBlocksOfProof?.map { it.toHex() }
        popTransactionId = state.popTransactionId
    }
}
