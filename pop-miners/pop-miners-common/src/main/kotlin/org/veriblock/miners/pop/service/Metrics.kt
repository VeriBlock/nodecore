package org.veriblock.miners.pop.service

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Timer
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics
import io.micrometer.core.instrument.binder.logging.LogbackMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.core.instrument.binder.system.UptimeMetrics
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import org.veriblock.miners.pop.core.OperationState

object Metrics {
    val registry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    val meterBinders = listOf(
        UptimeMetrics(),
        ProcessorMetrics(),
        LogbackMetrics(),
        JvmThreadMetrics(),
        JvmMemoryMetrics(),
        JvmGcMetrics(),
        ClassLoaderMetrics()
    )

    internal val startedOperationsCounter = Counter.builder("pop_miner.operations")
        .tags("action", "started")
        .description("Number of mining operations that were started")
        .register(registry)

    internal val completedOperationsCounter = Counter.builder("pop_miner.operations")
        .tags("action", "completed")
        .description("Number of mining operations that were completed")
        .register(registry)

    internal val failedOperationsCounter = Counter.builder("pop_miner.operations")
        .tags("action", "failed")
        .description("Number of mining operations that failed")
        .register(registry)

    internal val spentFeesCounter = Counter.builder("pop_miner.spent_fees")
        .description("Total amount of spent fees")
        .register(registry)

    internal val miningRewardCounter = Counter.builder("pop_miner.mining_rewards")
        .description("Total amount of mining rewards")
        .register(registry)

    internal val operationStateTimersByTargetState = mapOf(
        OperationState.INSTRUCTION to createOperationStateTimer("retrieveMiningInstruction"),
        OperationState.ENDORSEMENT_TRANSACTION to createOperationStateTimer("createEndorsementTransaction"),
        OperationState.CONFIRMED to createOperationStateTimer("confirmEndorsementTransaction"),
        OperationState.BLOCK_OF_PROOF to createOperationStateTimer("determineBlockOfProof"),
        OperationState.PROVEN to createOperationStateTimer("proveEndorsementTransaction"),
        OperationState.CONTEXT to createOperationStateTimer("buildPublicationContext"),
        OperationState.SUBMITTED_POP_DATA to createOperationStateTimer("submitPopEndorsement"),
        OperationState.COMPLETED to createOperationStateTimer("confirmPayout")
    )

    private fun createOperationStateTimer(taskName: String) = Timer.builder("pop_miner.operations_timer")
        .description("Total time spent in the $taskName task")
        .tags("task", taskName)
        .publishPercentiles(0.5, 0.9, 0.95, 0.99)
        .register(registry)
}
