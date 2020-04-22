package org.veriblock.miners.pop.service

import io.micrometer.core.instrument.Timer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.time.withTimeout
import kotlinx.coroutines.yield
import org.veriblock.core.contracts.MiningInstruction
import org.veriblock.core.contracts.WithDetailedInfo
import org.veriblock.core.utilities.createLogger
import org.veriblock.miners.pop.core.MerklePath
import org.veriblock.miners.pop.core.MiningOperation
import org.veriblock.miners.pop.core.OperationState
import org.veriblock.miners.pop.core.SpBlock
import org.veriblock.miners.pop.core.SpTransaction
import org.veriblock.miners.pop.core.info
import org.veriblock.miners.pop.core.warn
import java.time.Duration

val logger = createLogger {}

const val MAX_TASK_RETRIES = 10

abstract class TaskService<
    MO : MiningOperation<
        out MiningInstruction,
        out SpTransaction,
        out SpBlock,
        out MerklePath,
        out WithDetailedInfo
    >
> {
    suspend fun runTasks(operation: MO) {
        if (operation.state == OperationState.FAILED) {
            logger.warn(operation, "Attempted to run tasks for a failed operation!")
            return
        }

        try {
            retrieveMiningInstruction(operation)
            createEndorsementTransaction(operation)
            confirmEndorsementTransaction(operation)
            determineBlockOfProof(operation)
            proveEndorsementTransaction(operation)
            buildPublicationContext(operation)
            submitPopEndorsement(operation)
            confirmPayout(operation)
        } catch (e: CancellationException) {
            logger.info(operation, "Job was cancelled")
        } catch (e: OperationException) {
            operation.fail(e.message)
        } catch (t: Throwable) {
            logger.debug(t) { t.message }
            operation.fail(t.toString())
        }
    }

    abstract suspend fun retrieveMiningInstruction(operation: MO)
    abstract suspend fun createEndorsementTransaction(operation: MO)
    abstract suspend fun confirmEndorsementTransaction(operation: MO)
    abstract suspend fun determineBlockOfProof(operation: MO)
    abstract suspend fun proveEndorsementTransaction(operation: MO)
    abstract suspend fun buildPublicationContext(operation: MO)
    abstract suspend fun submitPopEndorsement(operation: MO)
    abstract suspend fun confirmPayout(operation: MO)

    protected suspend fun MO.runTask(
        taskName: String,
        targetState: OperationState,
        timeout: Duration,
        block: suspend () -> Unit
    ) {
        // Check if this operation needs to run this task first
        if (state hasType targetState) {
            return
        }

        val timer = Metrics.operationStateTimersByTargetState.getValue(targetState)

        var success = false
        var attempts = 1
        do {
            val timerSample = Timer.start(Metrics.registry)
            try {
                withTimeout(timeout) {
                    block()
                }
                success = true
                timerSample.stop(timer)
            } catch (e: TaskException) {
                logger.warn(this, "Task '$taskName' has failed: ${e.message}")
                if (attempts < MAX_TASK_RETRIES) {
                    attempts++
                    // Check if the task was cancelled before performing any reattempts
                    yield()
                    // Wait a growing amount of time before every reattempt
                    val secondsToWait = attempts * attempts * 10
                    logger.info(this, "Will try again in $secondsToWait seconds...")
                    delay(secondsToWait * 1000L)
                    logger.info(this, "Performing attempt #$attempts to $taskName...")
                } else {
                    logger.warn(this, "Maximum reattempt amount exceeded for task '$taskName'")
                    throw e
                }
            } catch (e: TimeoutCancellationException) {
                failOperation(
                    "Operation has been cancelled for taking too long during task '$taskName'."
                )
            }
        } while (!success)
    }
}

class TaskException(override val message: String) : RuntimeException()
class OperationException(override val message: String) : RuntimeException()

/**
 *  Throw an exception as the task failed. It is inline so that call stack is not polluted.
 */
inline fun failTask(reason: String): Nothing {
    throw TaskException(reason)
}

/**
 *  Throw an exception as the task failed. It is inline so that call stack is not polluted.
 */
inline fun failOperation(reason: String): Nothing {
    throw OperationException(reason)
}

inline val Int.sec get() = Duration.ofSeconds(this.toLong())
inline val Int.min get() = Duration.ofMinutes(this.toLong())
inline val Int.hr get() = Duration.ofHours(this.toLong())
