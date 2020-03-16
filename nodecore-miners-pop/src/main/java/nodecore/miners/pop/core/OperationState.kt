// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.core

import nodecore.miners.pop.model.PopMiningInstruction
import org.bitcoinj.core.Block
import org.bitcoinj.core.Transaction

enum class OperationStateType(
    val id: Int,
    val description: String
) {
    INITIAL(0, "Initial state, to be started"),
    INSTRUCTION(1, "Mining Instruction retrieved, Endorsement BTC Transaction to be submitted"),
    ENDORSEMEMT_TRANSACTION(2, "Endorsement BTC Transaction submitted and to be confirmed"),
    CONFIRMED(3, "Endorsement BTC Transaction confirmed, waiting for Block of Proof"),
    BLOCK_OF_PROOF(4, "Block of Proof received, waiting for Endorsement Transaction to be proven"),
    PROVEN(5, "Endorsement BTC Transaction proven, building BTC Context"),
    CONTEXT(6, "BTC Context determined, waiting for submission response"),
    SUBMITTED_POP_DATA(7, "Publications submitted, waiting for VBK Endorsement Transaction to be confirmed"),
    VBK_ENDORSEMENT_TRANSACTION_CONFIRMED(8, "VBK Endorsement Transaction confirmed, waiting for payout block"),
    COMPLETE(9, "Completed"),
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

    open val endorsementTransaction: Transaction? get() = null

    open fun getDetailedInfo(): List<String> = emptyList()

    override fun toString(): String = type.description

    infix fun hasType(type: OperationStateType) = this.type hasType type

    object Initial : OperationState() {
        override val type = OperationStateType.INITIAL
    }

    open class Instruction(
        val miningInstruction: PopMiningInstruction
    ) : OperationState() {
        override val type = OperationStateType.INSTRUCTION
        override fun getDetailedInfo() = super.getDetailedInfo() +
            miningInstruction.getDetailedInfo()
    }

    open class EndorsementTransaction(
        previous: Instruction,
        override val endorsementTransaction: Transaction
    ) : Instruction(previous.miningInstruction) {
        override val type = OperationStateType.ENDORSEMEMT_TRANSACTION
        override fun getDetailedInfo() = super.getDetailedInfo() +
            "Endorsement Transaction: ${endorsementTransaction.txId}"
    }

    open class Confirmed(
        previous: EndorsementTransaction
    ) : EndorsementTransaction(previous, previous.endorsementTransaction) {
        override val type = OperationStateType.CONFIRMED
    }

    open class BlockOfProof(
        previous: Confirmed,
        val blockOfProof: Block
    ) : Confirmed(previous) {
        override val type = OperationStateType.BLOCK_OF_PROOF
        override fun getDetailedInfo() = super.getDetailedInfo() +
            "Block of Proof: ${blockOfProof.hashAsString}"
    }

    open class Proven(
        previous: BlockOfProof,
        val merklePath: String
    ) : BlockOfProof(previous, previous.blockOfProof) {
        override val type = OperationStateType.PROVEN
        override fun getDetailedInfo() = super.getDetailedInfo() +
            "Merkle Path: $merklePath"
    }

    open class Context(
        previous: Proven,
        val bitcoinContextBlocks: List<Block>
    ) : Proven(previous, previous.merklePath) {
        override val type = OperationStateType.CONTEXT
        override fun getDetailedInfo() = super.getDetailedInfo() + listOf(
            "BTC Context Blocks: ${bitcoinContextBlocks.joinToString { it.hashAsString }}"
        )
    }

    open class SubmittedPopData(
        previous: Context,
        val proofOfProofId: String
    ) : Context(previous, previous.bitcoinContextBlocks) {
        override val type = OperationStateType.SUBMITTED_POP_DATA
        override fun getDetailedInfo() = super.getDetailedInfo() +
            "Proof of Proof Id: $proofOfProofId"
    }

    open class VbkEndorsementTransactionConfirmed(
        previous: SubmittedPopData
    ) : SubmittedPopData(previous, previous.proofOfProofId) {
        override val type = OperationStateType.VBK_ENDORSEMENT_TRANSACTION_CONFIRMED
    }

    class Completed(
        previous: VbkEndorsementTransactionConfirmed,
        val payoutBlockHash: String,
        val payoutAmount: Double
    ) : VbkEndorsementTransactionConfirmed(previous) {
        override val type = OperationStateType.COMPLETE
        override fun getDetailedInfo() = super.getDetailedInfo() + listOf(
            "Payout Block Hash: $payoutBlockHash",
            "Payout Amount: $payoutAmount"
        )
    }

    open class Failed(
        val previous: OperationState,
        private val reason: String
    ) : OperationState() {
        override val type = OperationStateType.FAILED
        override fun toString() = "Failed: $reason"
        override fun getDetailedInfo() = previous.getDetailedInfo()
    }
}
