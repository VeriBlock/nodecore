package org.veriblock.miners.pop.core

import org.veriblock.miners.pop.core.ApmOperationState.ALL

object ApmOperationState {
    val INITIAL = MiningOperationState(MiningOperationState.INITIAL_ID, "Initial", "Retrieve Mining Instruction")
    val INSTRUCTION = MiningOperationState(1, "Mining Instruction retrieved", "Submit Endorsement Transaction", INITIAL)
    val ENDORSEMENT_TRANSACTION = MiningOperationState(2, "Endorsement Transaction submitted", "Confirm Endorsement Transaction", INSTRUCTION)
    val ENDORSEMENT_TX_CONFIRMED = MiningOperationState(3, "Endorsement Transaction Confirmed", "Determine Block of Proof", ENDORSEMENT_TRANSACTION)
    val BLOCK_OF_PROOF = MiningOperationState(4, "Block of Proof determined", "Prove Endorsement Transaction", ENDORSEMENT_TX_CONFIRMED)
    val PROVEN = MiningOperationState(5, "Endorsement Transaction proven", "Submit PoP Transaction", BLOCK_OF_PROOF)
    val SUBMITTED_POP_DATA = MiningOperationState(6, "VBK Publications submitted", "Detect Payout", PROVEN)
    val PAYOUT_DETECTED = MiningOperationState(7, "Payout detected", "Complete and save", SUBMITTED_POP_DATA)

    val ALL = listOf(
        INITIAL, INSTRUCTION, ENDORSEMENT_TRANSACTION, ENDORSEMENT_TX_CONFIRMED,
        BLOCK_OF_PROOF, PROVEN, SUBMITTED_POP_DATA, PAYOUT_DETECTED
    )
}

val MiningOperationState.nextState get() = ALL.find { it.previousState == this }
