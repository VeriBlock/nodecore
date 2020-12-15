package org.veriblock.miners.pop.service

import io.micrometer.core.instrument.Timer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.time.withTimeout
import kotlinx.coroutines.yield
import org.veriblock.core.utilities.createLogger
import org.veriblock.miners.pop.core.MiningOperation
import org.veriblock.miners.pop.core.MiningOperationState
import org.veriblock.miners.pop.core.debug
import org.veriblock.miners.pop.core.debugWarn
import org.veriblock.miners.pop.core.info
import org.veriblock.miners.pop.core.warn
import java.time.Duration

private val logger = createLogger {}

const val MAX_TASK_RETRIES = 10

abstract class TaskService<MO : MiningOperation> {
    suspend fun runTasks(operation: MO) {
        if (operation.state == MiningOperationState.FAILED) {
            logger.warn(operation, "Attempted to run tasks for a failed operation!")
            return
        }

        try {
            runTasksInternal(operation)
        } catch (e: CancellationException) {
            logger.info(operation, "Job was cancelled")
        } catch (e: OperationException) {
            operation.fail(e.message, e.cause)
        } catch (t: Throwable) {
            operation.fail(t.message ?: "An unexpected error has occurred. Please check the logs for further details", t)
        }
    }

    protected abstract suspend fun runTasksInternal(operation: MO)

    protected suspend fun MO.runTask(
        taskName: String,
        targetState: MiningOperationState,
        timeout: Duration,
        block: suspend () -> Unit
    ) {
        // Check if this operation needs to run this task first
        if (state hasType targetState) {
            return
        }

        val timer = Metrics.getOperationStateTimerByState(
            targetState.previousState
                ?: error("Trying to create metrics for target state $targetState!")
        )

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
                logger.debugWarn(this, e, "Task '$taskName' has failed")
                if (attempts < MAX_TASK_RETRIES) {
                    attempts++
                    // Check if the task was cancelled before performing sany reattempts
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
                failOperation("Operation has been cancelled for taking too long during task '$taskName'", e)
            }
        } while (!success)
    }
}

class TaskException(override val message: String) : RuntimeException()
class OperationException(override val message: String, override val cause: Throwable?) : RuntimeException()

/**
 *  Throw an exception as the task failed. It is inline so that call stack is not polluted.
 */
inline fun failTask(reason: String): Nothing {
    throw TaskException(reason)
}

/**
 *  Throw an exception as the task failed. It is inline so that call stack is not polluted.
 */
inline fun failOperation(reason: String, cause: Throwable? = null): Nothing {
    throw OperationException(reason, cause)
}

inline val Int.sec get() = Duration.ofSeconds(this.toLong())
inline val Int.min get() = Duration.ofMinutes(this.toLong())
inline val Int.hr get() = Duration.ofHours(this.toLong())
inline val Int.days get() = Duration.ofDays(this.toLong())
