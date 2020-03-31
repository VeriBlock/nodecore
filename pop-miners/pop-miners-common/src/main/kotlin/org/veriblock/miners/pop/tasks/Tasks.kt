package org.veriblock.miners.pop.tasks

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.time.withTimeout
import kotlinx.coroutines.yield
import org.veriblock.core.utilities.createLogger
import org.veriblock.miners.pop.core.MiningOperation
import org.veriblock.miners.pop.core.OperationStateType
import org.veriblock.miners.pop.core.info
import org.veriblock.miners.pop.core.warn
import java.time.Duration

private val logger = createLogger {}

private const val MAX_TASK_RETRIES = 10

private suspend inline fun MiningOperation.runTask(
    taskName: String,
    targetState: OperationStateType,
    timeout: Duration,
    crossinline block: suspend () -> Unit
) {
    // Check if this operation needs to run this task first
    if (stateType hasType targetState) {
        return
    }

    var success = false
    var attempts = 1
    do {
        try {
            withTimeout(timeout) {
                block()
            }
            success = true
        } catch (e: TaskException) {
            logger.warn(this) { "Task '$taskName' has failed: ${e.message}" }
            if (attempts < MAX_TASK_RETRIES) {
                attempts++
                // Check if the task was cancelled before performing any reattempts
                yield()
                // Wait a growing amount of time before every reattempt
                val secondsToWait = attempts * attempts * 10
                logger.info(this) { "Will try again in $secondsToWait seconds..." }
                delay(secondsToWait * 1000L)
                logger.info(this) { "Performing attempt #$attempts to $taskName..." }
            } else {
                logger.warn(this) { "Maximum reattempt amount exceeded for task '$taskName'" }
                throw e
            }
        } catch (e: TimeoutCancellationException) {
            error("Operation has been cancelled for taking too long during task '$taskName'.")
        }
    } while (!success)
}

class TaskException(message: String) : RuntimeException(message)

/**
 *  Throw an exception as the task failed. It is inline so that call stack is not polluted.
 */
private inline fun failTask(reason: String): Nothing {
    throw TaskException(reason)
}

private inline val Int.sec get() = Duration.ofSeconds(this.toLong())
private inline val Int.min get() = Duration.ofMinutes(this.toLong())
private inline val Int.hr get() = Duration.ofHours(this.toLong())
