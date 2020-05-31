package org.veriblock.miners.pop.core

import org.veriblock.miners.pop.core.ApmOperationState.ALL

object ApmOperationState {
    val INITIAL = MiningOperationState(MiningOperationState.INITIAL_ID, "Initial", "Retrieve Mining Instruction")
    val INSTRUCTION = MiningOperationState(1, "Mining Instruction retrieved", "Submit Endorsement Transaction", INITIAL)
    val ENDORSEMENT_TRANSACTION = MiningOperationState(2, "Endorsement Transaction submitted", "Confirm Endorsement Transaction", INSTRUCTION)
    val CONFIRMED = MiningOperationState(3, "Endorsement Transaction Confirmed", "Determine Block of Proof", ENDORSEMENT_TRANSACTION)
    val BLOCK_OF_PROOF = MiningOperationState(4, "Block of Proof determined", "Prove Endorsement Transaction", CONFIRMED)
    val PROVEN = MiningOperationState(5, "Endorsement Transaction proven", "Wait for Keystone of Proof", BLOCK_OF_PROOF)
    val KEYSTONE_OF_PROOF = MiningOperationState(6, "Keystone of Proof retrieved", "Get VBK Publications", PROVEN)
    val CONTEXT = MiningOperationState(7, "VBK Publications retrieved", "Submit PoP Transaction", KEYSTONE_OF_PROOF)
    val SUBMITTED_POP_DATA = MiningOperationState(8, "VBK Publications submitted", "Wait for Payout Block", CONTEXT)
    val PAYOUT_DETECTED = MiningOperationState(9, "Payout detected", "Complete and save", SUBMITTED_POP_DATA)

    val ALL = listOf(
        INITIAL, INSTRUCTION, ENDORSEMENT_TRANSACTION, CONFIRMED, BLOCK_OF_PROOF,
        PROVEN, KEYSTONE_OF_PROOF, CONTEXT, SUBMITTED_POP_DATA, PAYOUT_DETECTED
    )
}

val MiningOperationState.nextState get() = ALL.find { it.previousState == this }
