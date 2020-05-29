package org.veriblock.miners.pop.core

import ch.qos.logback.classic.Level
import kotlinx.coroutines.Job
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
    lateinit var state: MiningOperationState
        private set

    var job: Job? = null

    var failureReason: String? = null
        private set

    private val logs: MutableList<OperationLog> = CopyOnWriteArrayList(logs)

    init {
        if (!reconstituting) {
            Metrics.startedOperationsCounter.increment()
        }
    }

    protected fun setState(state: MiningOperationState) {
        this.state = state
        if (!reconstituting) {
            logger.debug(this, "New state: $state")
            onStateChanged()
        }
    }

    open fun onStateChanged() {
    }

    fun complete() {
        setState(MiningOperationState.COMPLETED)

        onCompleted()
        Metrics.completedOperationsCounter.increment()
    }

    open fun onCompleted() {
    }

    fun fail(reason: String) {
        logger.warn(this, "Failed: $reason")
        failureReason = reason
        setState(MiningOperationState.FAILED)

        onFailed()
        cancelJob()
        Metrics.failedOperationsCounter.increment()
    }

    open fun onFailed() {}

    fun isFailed() = failureReason != null

    fun cancelJob() {
        job?.cancel()
        job = null
    }

    fun getStateDescription() = if (isFailed()) {
        "Failed: $failureReason"
    } else {
        state.taskName
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
        return "MiningOperation(id='$id', state='${state.name}')"
    }
}
