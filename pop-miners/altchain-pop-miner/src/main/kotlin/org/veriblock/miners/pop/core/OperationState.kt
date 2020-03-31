// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.core

import org.veriblock.core.utilities.extensions.toHex
import org.veriblock.lite.transactionmonitor.WalletTransaction
import org.veriblock.sdk.alt.MiningInstruction
import org.veriblock.sdk.models.VeriBlockBlock
import org.veriblock.sdk.models.VeriBlockMerklePath
import org.veriblock.sdk.models.VeriBlockPublication

/**
 * This class holds the state data of an operation.
 * Each new state is child of the previous one and accumulates the data.
 */
sealed class OperationState {

    abstract val type: OperationStateType

    open val miningInstruction: MiningInstruction? = null
    open val endorsementTransaction: WalletTransaction? = null
    open val blockOfProof: VeriBlockBlock? = null
    open val merklePath: VeriBlockMerklePath? = null
    open val veriBlockPublications: List<VeriBlockPublication>? = null
    open val proofOfProofId: String? = null
    open val payoutBlockHash: String? = null
    open val payoutAmount: String? = null

    open fun getDetailedInfo(): List<String> = emptyList()

    override fun toString(): String = type.description

    infix fun hasType(type: OperationStateType) = this.type hasType type

    object Initial : OperationState() {
        override val type = OperationStateType.INITIAL
    }

    open class Instruction(
        override val miningInstruction: MiningInstruction
    ) : OperationState() {
        override val type = OperationStateType.INSTRUCTION
        override fun getDetailedInfo() = super.getDetailedInfo() +
            miningInstruction.getDetailedInfo()
    }

    open class EndorsementTransaction(
        previous: Instruction,
        override val endorsementTransaction: WalletTransaction
    ) : Instruction(previous.miningInstruction) {
        override val type = OperationStateType.ENDORSEMENT_TRANSACTION
        override fun getDetailedInfo() = super.getDetailedInfo() +
            "Endorsement Transaction: ${endorsementTransaction.id.bytes.toHex()}"
    }

    open class Confirmed(
        previous: EndorsementTransaction
    ) : EndorsementTransaction(previous, previous.endorsementTransaction) {
        override val type = OperationStateType.CONFIRMED
    }

    open class BlockOfProof(
        previous: Confirmed,
        override val blockOfProof: VeriBlockBlock
    ) : Confirmed(previous) {
        override val type = OperationStateType.BLOCK_OF_PROOF
        override fun getDetailedInfo() = super.getDetailedInfo() +
            "Block of Proof: ${blockOfProof.hash.bytes.toHex()}"
    }

    open class Proven(
        previous: BlockOfProof,
        override val merklePath: VeriBlockMerklePath
    ) : BlockOfProof(previous, previous.blockOfProof) {
        override val type = OperationStateType.PROVEN
        override fun getDetailedInfo() = super.getDetailedInfo() +
            "Endorsement Transaction Merkle Path: ${merklePath.toCompactString()}"
    }

    open class VeriBlockPublications(
        previous: Proven,
        override val veriBlockPublications: List<VeriBlockPublication>
    ) : Proven(previous, previous.merklePath) {
        override val type = OperationStateType.CONTEXT
        override fun getDetailedInfo() = super.getDetailedInfo() + listOf(
            "VTB Transactions: ${veriBlockPublications.joinToString { it.transaction.id.bytes.toHex() }}",
            "VTB BTC Blocks: ${veriBlockPublications.joinToString { it.firstBitcoinBlock.hash.bytes.toHex() }}"
        )
    }

    open class SubmittedPopData(
        previous: VeriBlockPublications,
        override val proofOfProofId: String
    ) : VeriBlockPublications(previous, previous.veriBlockPublications) {
        override val type = OperationStateType.SUBMITTED_POP_DATA
        override fun getDetailedInfo() = super.getDetailedInfo() +
            "Proof of Proof Id: $proofOfProofId"
    }

    class Completed(
        previous: SubmittedPopData,
        override val payoutBlockHash: String,
        override val payoutAmount: String
    ) : SubmittedPopData(previous, previous.proofOfProofId) {
        override val type = OperationStateType.COMPLETED
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

        override val miningInstruction = previous.miningInstruction
        override val endorsementTransaction = previous.endorsementTransaction
        override val blockOfProof = previous.blockOfProof
        override val merklePath = previous.merklePath
        override val veriBlockPublications = previous.veriBlockPublications
        override val proofOfProofId = previous.proofOfProofId

        override fun toString() = "Failed: $reason"
        override fun getDetailedInfo() = previous.getDetailedInfo()
    }
}
