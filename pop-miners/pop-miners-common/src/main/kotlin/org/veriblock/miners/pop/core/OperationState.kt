package org.veriblock.miners.pop.core

enum class OperationState(
    val id: Int,
    val description: String
) {
    INITIAL(0, "Initial state, to be started"),
    INSTRUCTION(1, "Mining Instruction retrieved, Endorsement Transaction to be submitted"),
    ENDORSEMENT_TRANSACTION(2, "Endorsement Transaction submitted and to be confirmed"),
    CONFIRMED(3, "Endorsement Transaction confirmed, waiting for Block of Proof"),
    BLOCK_OF_PROOF(4, "Block of Proof received, waiting for Endorsement Transaction to be proven"),
    PROVEN(5, "Endorsement Transaction proven, building Context"),
    CONTEXT(6, "Context determined, waiting for submission response"),
    SUBMITTED_POP_DATA(7, "Publications submitted, waiting for payout block"),
    COMPLETED(8, "Completed"),
    FAILED(-1, "Failed");

    fun isRunning() = this != INITIAL && this != COMPLETED && this != FAILED
    fun isDone() = this == COMPLETED || this == FAILED

    infix fun hasType(type: OperationState): Boolean = if (type != FAILED) {
        id >= type.id
    } else {
        this == FAILED
    }
}

fun Int.asOperationState(): OperationState {
    return OperationState.values().find {
        it.id == this
    } ?: error("The Operation State with id '$this' does not exist!")
}
