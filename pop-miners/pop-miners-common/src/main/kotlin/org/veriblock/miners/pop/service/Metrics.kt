package org.veriblock.miners.pop.service

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Timer
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics
import io.micrometer.core.instrument.binder.logging.Log4j2Metrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.core.instrument.binder.system.UptimeMetrics
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import org.veriblock.miners.pop.core.MiningOperationState

object Metrics {
    val registry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    val meterBinders = listOf(
        UptimeMetrics(),
        ProcessorMetrics(),
        Log4j2Metrics(),
        JvmThreadMetrics(),
        JvmMemoryMetrics(),
        JvmGcMetrics(),
        ClassLoaderMetrics()
    )

    val startedOperationsCounter: Counter = Counter.builder("pop_miner.operations")
        .tags("action", "started")
        .description("Number of mining operations that were started")
        .register(registry)

    val completedOperationsCounter: Counter = Counter.builder("pop_miner.operations")
        .tags("action", "completed")
        .description("Number of mining operations that were completed")
        .register(registry)

    val failedOperationsCounter: Counter = Counter.builder("pop_miner.operations")
        .tags("action", "failed")
        .description("Number of mining operations that failed")
        .register(registry)

    val spentFeesCounter: Counter = Counter.builder("pop_miner.spent_fees")
        .description("Total amount of spent fees")
        .register(registry)

    val miningRewardCounter: Counter = Counter.builder("pop_miner.mining_rewards")
        .description("Total amount of mining rewards")
        .register(registry)

    private val operationStateTimersByTargetState = mutableMapOf<MiningOperationState, Timer>()

    fun getOperationStateTimerByState(state: MiningOperationState) = operationStateTimersByTargetState.getOrPut(state) {
        createOperationStateTimer(state.taskName.toLowerCase().replace(' ', '_'))
    }

    private fun createOperationStateTimer(taskName: String) = Timer.builder("pop_miner.operations_timer")
        .description("Total time spent in the $taskName task")
        .tags("task", taskName)
        .publishPercentiles(0.5, 0.9, 0.95, 0.99)
        .register(registry)
}
