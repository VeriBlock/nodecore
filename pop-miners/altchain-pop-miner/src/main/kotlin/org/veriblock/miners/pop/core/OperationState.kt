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

enum class OperationStateType(
    val id: Int,
    val description: String
) {
    INITIAL(0, "Initial state, to be started"),
    INSTRUCTION(1, "Mining Instruction retrieved, Endorsement VBK Transaction to be submitted"),
    ENDORSEMEMT_TRANSACTION(2, "Endorsement VBK Transaction submitted and to be confirmed"),
    CONFIRMED(3, "Endorsement VBK Transaction confirmed, waiting for Block of Proof"),
    BLOCK_OF_PROOF(4, "Block of Proof received, Endorsement VBK Transaction to be proved"),
    TRANSACTION_PROVED(5, "Endorsement VBK Transaction Proved, waiting for Keystone of Proof"),
    KEYSTONE_OF_PROOF(6, "VBK Keystone of Proof received, waiting for VBK Publications"),
    VERIBLOCK_PUBLICATIONS(7, "VBK Publications received, waiting for submission response"),
    SUBMITTED_POP_DATA(8, "Publications submitted, waiting Alt Endorsement Transaction to be confirmed"),
    ALT_ENDORSEMENT_TRANSACTION_CONFIRMED(9, "Alt Endorsement Transaction confirmed, waiting for Endorsing Block to be confirmed"),
    ALT_ENDORSED_BLOCK_CONFIRMED(10, "Alt Endorsed Block confirmed, waiting for payout block"),
    COMPLETE(11, "Completed"),
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

    open val miningInstruction: MiningInstruction? = null
    open val endorsementTransaction: WalletTransaction? = null
    open val blockOfProof: VeriBlockBlock? = null
    open val merklePath: VeriBlockMerklePath? = null
    open val keystoneOfProof: VeriBlockBlock? = null
    open val veriBlockPublications: List<VeriBlockPublication>? = null
    open val proofOfProofId: String? = null
    open val altEndorsementBlockHash: String? = null
    open val payoutBlockHash: String? = null
    open val payoutAmount: Double? = null

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
        override val type = OperationStateType.ENDORSEMEMT_TRANSACTION
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

    open class TransactionProved(
        previous: BlockOfProof,
        override val merklePath: VeriBlockMerklePath
    ) : BlockOfProof(previous, previous.blockOfProof) {
        override val type = OperationStateType.TRANSACTION_PROVED
        override fun getDetailedInfo() = super.getDetailedInfo() +
            "Endorsement Transaction Merkle Path: ${merklePath.toCompactString()}"
    }

    open class KeystoneOfProof(
        previous: TransactionProved,
        override val keystoneOfProof: VeriBlockBlock
    ) : TransactionProved(previous, previous.merklePath) {
        override val type = OperationStateType.KEYSTONE_OF_PROOF
        override fun getDetailedInfo() = super.getDetailedInfo() +
            "Keystone of Proof: ${keystoneOfProof.hash.bytes.toHex()}"
    }

    open class VeriBlockPublications(
        previous: KeystoneOfProof,
        override val veriBlockPublications: List<VeriBlockPublication>
    ) : KeystoneOfProof(previous, previous.keystoneOfProof) {
        override val type = OperationStateType.VERIBLOCK_PUBLICATIONS
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

    open class AltEndorsementTransactionConfirmed(
        previous: SubmittedPopData
    ) : SubmittedPopData(previous, previous.proofOfProofId) {
        override val type = OperationStateType.ALT_ENDORSEMENT_TRANSACTION_CONFIRMED
    }

    open class AltEndorsedBlockConfirmed(
        previous: AltEndorsementTransactionConfirmed,
        override val altEndorsementBlockHash: String
    ) : AltEndorsementTransactionConfirmed(previous) {
        override val type = OperationStateType.ALT_ENDORSED_BLOCK_CONFIRMED
        override fun getDetailedInfo() = super.getDetailedInfo() +
            "Alt Endorsement Block Hash: $altEndorsementBlockHash"
    }

    class Completed(
        previous: AltEndorsedBlockConfirmed,
        override val payoutBlockHash: String,
        override val payoutAmount: Double
    ) : AltEndorsedBlockConfirmed(previous, previous.altEndorsementBlockHash) {
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

        override val miningInstruction = previous.miningInstruction
        override val endorsementTransaction = previous.endorsementTransaction
        override val blockOfProof = previous.blockOfProof
        override val merklePath = previous.merklePath
        override val keystoneOfProof = previous.keystoneOfProof
        override val veriBlockPublications = previous.veriBlockPublications
        override val proofOfProofId = previous.proofOfProofId
        override val altEndorsementBlockHash = previous.altEndorsementBlockHash

        override fun toString() = "Failed: $reason"
        override fun getDetailedInfo() = previous.getDetailedInfo()
    }
}
