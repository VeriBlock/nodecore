// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.core

import nodecore.miners.pop.common.Utility
import nodecore.miners.pop.model.PopMiningInstruction
import org.bitcoinj.core.Block
import org.bitcoinj.core.Transaction

enum class OperationStateType(
    val id: Int,
    val description: String
) {
    INITIAL(0, "Initial state, to be started"),
    INSTRUCTION(1, "Mining Instruction retrieved, Endorsement BTC Transaction to be submitted"),
    ENDORSEMENT_TRANSACTION(2, "Endorsement BTC Transaction submitted and to be confirmed"),
    CONFIRMED(3, "Endorsement BTC Transaction confirmed, waiting for Block of Proof"),
    BLOCK_OF_PROOF(4, "Block of Proof received, waiting for Endorsement Transaction to be proven"),
    PROVEN(5, "Endorsement BTC Transaction proven, building BTC Context"),
    CONTEXT(6, "BTC Context determined, waiting for submission response"),
    SUBMITTED_POP_DATA(7, "Publications submitted, waiting for payout block"),
    COMPLETED(9, "Completed"),
    FAILED(-1, "Failed");

    infix fun hasType(type: OperationStateType): Boolean = if (type != FAILED) {
        id >= type.id
    } else {
        this == FAILED
    }
}

/**
 * This class holds the state data of an operation.
 * Each new state is child of the previous one and accumulates the data.
 */
sealed class OperationState {

    abstract val type: OperationStateType

    open val miningInstruction: PopMiningInstruction? = null
    open val endorsementTransaction: Transaction? = null
    open val endorsementTransactionBytes: ByteArray? = null
    open val blockOfProof: Block? = null
    open val merklePath: String? = null
    open val bitcoinContextBlocks: List<Block>? = null
    open val proofOfProofId: String? = null
    open val payoutBlockHash: String? = null
    open val payoutAmount: String? = null

    open fun getDetailedInfo(): Map<String, String> = emptyMap()

    override fun toString(): String = type.description

    infix fun hasType(type: OperationStateType) = this.type hasType type

    object Initial : OperationState() {
        override val type = OperationStateType.INITIAL
    }

    open class Instruction(
        override val miningInstruction: PopMiningInstruction
    ) : OperationState() {
        override val type = OperationStateType.INSTRUCTION
        override fun getDetailedInfo() = super.getDetailedInfo() +
            miningInstruction.detailedInfo
    }

    open class EndorsementTransaction(
        previous: Instruction,
        override val endorsementTransaction: Transaction,
        override val endorsementTransactionBytes: ByteArray
    ) : Instruction(previous.miningInstruction) {
        override val type = OperationStateType.ENDORSEMENT_TRANSACTION
        override fun getDetailedInfo() = super.getDetailedInfo() + mapOf(
            "endorsementTransactionId" to endorsementTransaction.txId.toString(),
            "endorsementTransactionFee" to Utility.formatBTCFriendlyString(endorsementTransaction.fee)
        )
    }

    open class Confirmed(
        previous: EndorsementTransaction
    ) : EndorsementTransaction(previous, previous.endorsementTransaction, previous.endorsementTransactionBytes) {
        override val type = OperationStateType.CONFIRMED
    }

    open class BlockOfProof(
        previous: Confirmed,
        override val blockOfProof: Block
    ) : Confirmed(previous) {
        override val type = OperationStateType.BLOCK_OF_PROOF
        override fun getDetailedInfo() = super.getDetailedInfo() +
            ("blockOfProof" to blockOfProof.hashAsString)
    }

    open class Proven(
        previous: BlockOfProof,
        override val merklePath: String
    ) : BlockOfProof(previous, previous.blockOfProof) {
        override val type = OperationStateType.PROVEN
        override fun getDetailedInfo() = super.getDetailedInfo() +
            ("merklePath" to merklePath)
    }

    open class Context(
        previous: Proven,
        override val bitcoinContextBlocks: List<Block>
    ) : Proven(previous, previous.merklePath) {
        override val type = OperationStateType.CONTEXT
        override fun getDetailedInfo() = super.getDetailedInfo() +
            ("btcContextBlocks" to bitcoinContextBlocks.joinToString { it.hashAsString })
    }

    open class SubmittedPopData(
        previous: Context,
        override val proofOfProofId: String
    ) : Context(previous, previous.bitcoinContextBlocks) {
        override val type = OperationStateType.SUBMITTED_POP_DATA
        override fun getDetailedInfo() = super.getDetailedInfo() +
            ("proofOfProofId" to proofOfProofId)
    }

    class Completed(
        previous: SubmittedPopData,
        override val payoutBlockHash: String,
        override val payoutAmount: String
    ) : SubmittedPopData(previous, previous.proofOfProofId) {
        override val type = OperationStateType.COMPLETED
        override fun getDetailedInfo() = super.getDetailedInfo() + mapOf(
            "Payout Block Hash" to payoutBlockHash,
            "Payout Amount" to payoutAmount
        )
    }

    open class Failed(
        val previous: OperationState,
        private val reason: String
    ) : OperationState() {
        override val type = OperationStateType.FAILED

        override val miningInstruction = previous.miningInstruction
        override val endorsementTransaction = previous.endorsementTransaction
        override val endorsementTransactionBytes = previous.endorsementTransactionBytes
        override val blockOfProof = previous.blockOfProof
        override val merklePath = previous.merklePath
        override val bitcoinContextBlocks = previous.bitcoinContextBlocks
        override val proofOfProofId = previous.proofOfProofId

        override fun toString() = "Failed: $reason"
        override fun getDetailedInfo() = previous.getDetailedInfo()
    }
}
