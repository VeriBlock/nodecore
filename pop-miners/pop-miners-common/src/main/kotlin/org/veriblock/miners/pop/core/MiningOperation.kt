package org.veriblock.miners.pop.core

import ch.qos.logback.classic.Level
import kotlinx.coroutines.Job
import org.veriblock.core.contracts.MiningInstruction
import org.veriblock.core.utilities.createLogger
import org.veriblock.miners.pop.service.Metrics
import java.time.LocalDateTime
import java.util.concurrent.CopyOnWriteArrayList

private val logger = createLogger {}

abstract class MiningOperation(
    val id: String,
    var endorsedBlockHeight: Int?,
    val createdAt: LocalDateTime,
    logs: List<OperationLog>,
    var reconstituting: Boolean
) {
    var state: OperationState = OperationState.INITIAL
        private set

    var job: Job? = null

    var payoutBlockHash: String? = null
        private set
    var payoutAmount: Long? = null
        private set

    var failureReason: String? = null
        private set

    private val logs: MutableList<OperationLog> = CopyOnWriteArrayList(logs)

    init {
        if (!reconstituting) {
            Metrics.startedOperationsCounter.increment()
        }
    }

    protected fun setState(state: OperationState) {
        if (state.id > 0 && this.state.id != state.id - 1) {
            error("Trying to set state from ${this.state} directly to $state")
        }
        this.state = state
        if (!reconstituting) {
            logger.debug(this, "New state: $state")
            onStateChanged()
        }
    }

    open fun onStateChanged() {
    }

    fun complete(payoutBlockHash: String, payoutAmount: Long) {
        if (state != OperationState.SUBMITTED_POP_DATA) {
            error("Trying to mark the process as complete without having submitted the PoP data")
        }
        this.payoutBlockHash = payoutBlockHash
        this.payoutAmount = payoutAmount
        setState(OperationState.COMPLETED)

        onCompleted()
        Metrics.completedOperationsCounter.increment()
        Metrics.miningRewardCounter.increment(payoutAmount.toDouble())
    }

    open fun onCompleted() {}

    fun fail(reason: String) {
        logger.warn(this, "Failed: $reason")
        failureReason = reason
        setState(OperationState.FAILED)

        onFailed()
        cancelJob()
        Metrics.failedOperationsCounter.increment()
    }

    open fun onFailed() {}

    fun isFailed() = state == OperationState.FAILED

    fun cancelJob() {
        job?.cancel()
        job = null
    }

    fun getStateDescription() = if (isFailed()) {
        "Failed: $failureReason"
    } else {
        state.description
    }

    open fun isLoggingEnabled(level: Level): Boolean = true

    fun addLog(log: OperationLog) {
        logs.add(log)
    }

    fun getLogs(level: Level = Level.TRACE): List<OperationLog> {
        return logs.filter {
            Level.toLevel(it.level).isGreaterOrEqual(level)
        }
    }

    abstract fun getDetailedInfo(): Map<String, String>

    override fun toString(): String {
        return "MiningOperation(id='$id', state='${state.description}')"
    }
}
