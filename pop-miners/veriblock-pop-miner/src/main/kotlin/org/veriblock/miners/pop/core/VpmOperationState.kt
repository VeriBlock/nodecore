package org.veriblock.miners.pop.core

import org.veriblock.miners.pop.core.VpmOperationState.ALL

object VpmOperationState {
    val INITIAL = MiningOperationState(MiningOperationState.INITIAL_ID, "Initial", "Retrieve Mining Instruction")
    val INSTRUCTION = MiningOperationState(1, "Mining Instruction retrieved", "Submit Endorsement Transaction", INITIAL)
    val ENDORSEMENT_TRANSACTION = MiningOperationState(2, "Endorsement Transaction submitted", "Confirm Endorsement Transaction", INSTRUCTION)
    val CONFIRMED = MiningOperationState(3, "Endorsement Transaction Confirmed", "Wait for Block of Proof", ENDORSEMENT_TRANSACTION)
    val BLOCK_OF_PROOF = MiningOperationState(4, "Block of Proof received", "Prove Endorsement Transaction", CONFIRMED)
    val PROVEN = MiningOperationState(5, "Endorsement Transaction proven", "Build Context", BLOCK_OF_PROOF)
    val CONTEXT = MiningOperationState(6, "Context determined", "Submit PoP Transaction", PROVEN)
    val SUBMITTED_POP_DATA = MiningOperationState(7, "Publications submitted", "Wait for Payout Block", CONTEXT)
    val PAYOUT_DETECTED = MiningOperationState(8, "Payout detected", "Complete and save", SUBMITTED_POP_DATA)

    val ALL = listOf(
        INITIAL, INSTRUCTION, ENDORSEMENT_TRANSACTION, CONFIRMED, BLOCK_OF_PROOF, PROVEN, CONTEXT, SUBMITTED_POP_DATA, PAYOUT_DETECTED
    )
}

val MiningOperationState.nextState get() = ALL.find { it.previousState == this }
