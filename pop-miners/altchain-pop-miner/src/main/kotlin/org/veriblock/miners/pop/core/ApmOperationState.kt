package org.veriblock.miners.pop.core

import org.veriblock.miners.pop.core.ApmOperationState.ALL

object ApmOperationState {
    val INITIAL = MiningOperationState(MiningOperationState.INITIAL_ID, "Initial", "Retrieve Mining Instruction")
    val INSTRUCTION = MiningOperationState(1, "Mining Instruction retrieved", "Submit Endorsement Transaction", INITIAL)
    val ENDORSEMENT_TRANSACTION = MiningOperationState(2, "Endorsement Transaction submitted", "Confirm Endorsement Transaction", INSTRUCTION)
    val ENDORSEMENT_TX_CONFIRMED = MiningOperationState(3, "Endorsement Transaction Confirmed", "Determine Block of Proof", ENDORSEMENT_TRANSACTION)
    val BLOCK_OF_PROOF = MiningOperationState(4, "Block of Proof determined", "Prove Endorsement Transaction", ENDORSEMENT_TX_CONFIRMED)
    val PROVEN = MiningOperationState(5, "Endorsement Transaction proven", "Submit PoP Transaction", BLOCK_OF_PROOF)
    val SUBMITTED_POP_DATA = MiningOperationState(8, "VBK Publications submitted", "Confirm ATV", PROVEN)
    val ATV_CONFIRMED = MiningOperationState(9, "ATV Confirmed", "Wait for Payout Block", SUBMITTED_POP_DATA)
    val PAYOUT_DETECTED = MiningOperationState(10, "Payout detected", "Complete and save", ATV_CONFIRMED)

    val ALL = listOf(
        INITIAL, INSTRUCTION, ENDORSEMENT_TRANSACTION, ENDORSEMENT_TX_CONFIRMED,
        BLOCK_OF_PROOF, PROVEN, SUBMITTED_POP_DATA, ATV_CONFIRMED, PAYOUT_DETECTED
    )
}

val MiningOperationState.nextState get() = ALL.find { it.previousState == this }
