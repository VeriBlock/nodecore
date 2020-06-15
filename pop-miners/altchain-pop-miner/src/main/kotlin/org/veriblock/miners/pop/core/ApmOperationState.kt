package org.veriblock.miners.pop.core

import org.veriblock.miners.pop.core.ApmOperationState.ALL

object ApmOperationState {
    val INITIAL = MiningOperationState(MiningOperationState.INITIAL_ID, "Initial", "Retrieve Mining Instruction")
    val INSTRUCTION = MiningOperationState(1, "Mining Instruction retrieved", "Submit Endorsement Transaction", INITIAL)
    val ENDORSEMENT_TRANSACTION = MiningOperationState(2, "Endorsement Transaction submitted", "Confirm Endorsement Transaction", INSTRUCTION)
    val ENDORSEMENT_TX_CONFIRMED = MiningOperationState(3, "Endorsement Transaction Confirmed", "Determine Block of Proof", ENDORSEMENT_TRANSACTION)
    val BLOCK_OF_PROOF = MiningOperationState(4, "Block of Proof determined", "Prove Endorsement Transaction", ENDORSEMENT_TX_CONFIRMED)
    val PROVEN = MiningOperationState(5, "Endorsement Transaction proven", "Wait for Keystone of Proof", BLOCK_OF_PROOF)
    val KEYSTONE_OF_PROOF = MiningOperationState(6, "Keystone of Proof retrieved", "Get VBK Publications", PROVEN)
    val CONTEXT = MiningOperationState(7, "VBK Publications retrieved", "Submit PoP Transaction", KEYSTONE_OF_PROOF)
    val SUBMITTED_POP_DATA = MiningOperationState(8, "VBK Publications submitted", "Confirm PoP Transaction", CONTEXT)
    val POP_TX_CONFIRMED = MiningOperationState(9, "PoP Transaction Confirmed", "Wait for Payout Block", SUBMITTED_POP_DATA)
    val PAYOUT_DETECTED = MiningOperationState(10, "Payout detected", "Complete and save", POP_TX_CONFIRMED)

    val ALL = listOf(
        INITIAL, INSTRUCTION, ENDORSEMENT_TRANSACTION, ENDORSEMENT_TX_CONFIRMED, BLOCK_OF_PROOF, PROVEN,
        KEYSTONE_OF_PROOF, CONTEXT, SUBMITTED_POP_DATA, POP_TX_CONFIRMED, PAYOUT_DETECTED
    )
}

val MiningOperationState.nextState get() = ALL.find { it.previousState == this }
