package org.veriblock.miners.pop.core

class MiningOperationState(
    val id: Int,
    val name: String,
    val taskName: String = "",
    val previousState: MiningOperationState? = null
) {
    fun isDone() = this == COMPLETED || this == FAILED

    infix fun hasType(type: MiningOperationState): Boolean = if (type != FAILED) {
        id >= type.id
    } else {
        this == FAILED
    }

    override fun toString(): String = name

    companion object {
        const val INITIAL_ID = 0
        const val COMPLETED_ID = 100
        const val FAILED_ID = -1

        val COMPLETED = MiningOperationState(COMPLETED_ID, "Completed")
        val FAILED = MiningOperationState(FAILED_ID, "Failed")
    }
}
