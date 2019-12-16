// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop

import org.veriblock.lite.NodeCoreLiteKit
import org.veriblock.lite.core.Balance
import org.veriblock.lite.core.Context
import org.veriblock.miners.pop.core.MiningOperation
import org.veriblock.miners.pop.core.OperationStatus
import org.veriblock.miners.pop.service.OperationService
import org.veriblock.miners.pop.tasks.WorkflowAuthority
import org.veriblock.miners.pop.util.formatCoinAmount
import org.veriblock.sdk.Coin
import org.veriblock.core.utilities.Configuration
import org.veriblock.sdk.Sha256Hash
import org.veriblock.core.utilities.createLogger
import org.veriblock.shell.core.Result
import org.veriblock.shell.core.failure
import org.veriblock.shell.core.success
import java.io.IOException
import java.util.*
import java.util.concurrent.ConcurrentHashMap

private val logger = createLogger {}

class MinerConfig(
    val feePerByte: Long = 1_000,
    val maxFee: Long = 10_000_000
)

val minerConfig = Configuration.extract("miner") ?: MinerConfig()

class Miner(
    private val workflowAuthority: WorkflowAuthority,
    private val nodeCoreLiteKit: NodeCoreLiteKit,
    private val operationService: OperationService
) {
    private val operations = ConcurrentHashMap<String, MiningOperation>()

    private enum class ReadyCondition {
        SUFFICIENT_FUNDS,
        NODECORE_CONNECTED
    }
    private val readyConditions: EnumSet<ReadyCondition> = EnumSet.noneOf(ReadyCondition::class.java)

    private var operationsSubmitted = false

    init {
        // Restore operations (including re-attach listeners) before the network starts
        this.nodeCoreLiteKit.beforeNetworkStart = { loadSuspendedOperations() }

        nodeCoreLiteKit.network.healthyEvent.register(this) {
            addReadyCondition(ReadyCondition.NODECORE_CONNECTED)
        }
        nodeCoreLiteKit.network.unhealthyEvent.register(this) {
            logger.warn { "Unable to connect to NodeCore!" }
            removeReadyCondition(ReadyCondition.NODECORE_CONNECTED)
        }
        nodeCoreLiteKit.balanceChangedEvent.register(this) {
            logger.info { "Current balance: ${it.confirmedBalance.formatCoinAmount()} ${Context.vbkTokenName}" }
            if (it.confirmedBalance.atomicUnits >= minerConfig.maxFee) {
                addReadyCondition(ReadyCondition.SUFFICIENT_FUNDS)
            } else {
                removeReadyCondition(ReadyCondition.SUFFICIENT_FUNDS)
            }
        }
    }

    // FIXME: This is a hack to force-trigger balance retrieval in the ready check
    private var currentBalance: Balance? = null

    private fun isReady(): Boolean {
        // FIXME: This is a hack to force-trigger balance retrieval in the ready check
        getBalance()?.let {
            if (it != currentBalance) {
                currentBalance = it
                nodeCoreLiteKit.balanceChangedEvent.trigger(it)
            }
        }
        return readyConditions.size == ReadyCondition.values().size
    }

    private fun addReadyCondition(condition: ReadyCondition) {
        val wasReady = isReady()
        readyConditions.add(condition)
        if (!wasReady && isReady()) {
            logger.info { "Miner is ready!" }
            if (!operationsSubmitted) {
                submitSuspendedOperations()
            }
        }
    }

    private fun removeReadyCondition(condition: ReadyCondition) {
        val wasReady = isReady()
        readyConditions.remove(condition)
        if (wasReady) {
            logger.info { "Miner is no longer ready:" }
            logger.info { condition.getNotReadyMessage() }
        }
    }

    private fun ReadyCondition.getNotReadyMessage() = when (this) {
        ReadyCondition.NODECORE_CONNECTED -> "Waiting for connection to NodeCore"
        ReadyCondition.SUFFICIENT_FUNDS -> {
            val currentBalance = getBalance()?.confirmedBalance ?: Coin.ZERO
            """
                PoP wallet does not contain sufficient funds
                         Current balance: ${currentBalance.atomicUnits.formatCoinAmount()} ${Context.vbkTokenName}
                         Minimum required: ${minerConfig.maxFee.formatCoinAmount()}, need ${(minerConfig.maxFee - currentBalance.atomicUnits).formatCoinAmount()} more
                         Send VBK coins to: ${nodeCoreLiteKit.addressManager.defaultAddress.hash}
            """.trimIndent()
        }
    }

    private fun listNotReadyConditions() = EnumSet.complementOf(readyConditions).map {
        it.getNotReadyMessage()
    }

    fun start() {
        logger.info("Starting miner...")
        try {
            nodeCoreLiteKit.start()
        } catch (e: IOException) {
            logger.error("Unable to start the NodeCore kit", e)
            throw IllegalStateException("Miner could not be started", e)
        }
    }

    fun listOperations() = operations.values.asSequence().sortedBy {
        it.timestamp
    }.map {
        "${it.id}: ${it.chainId} | ${it.state}"
    }.toList()

    fun getOperation(id: String): MiningOperation? {
        return operations[id]
    }

    fun getAddress(): String = nodeCoreLiteKit.getAddress()

    fun getBalance(): Balance? = if (nodeCoreLiteKit.network.isHealthy()) {
        nodeCoreLiteKit.network.getBalance()
    } else {
        null
    }

    fun mine(chain: String, block: Int?): Result {
        if (!isReady()) {
            return failure {
                addMessage(
                    "V412",
                    "Miner is not ready",
                    listNotReadyConditions(),
                    true
                )
            }
        }
        if (!nodeCoreLiteKit.network.isHealthy()) {
            return failure {
                addMessage("V010", "Unable to mine", "Cannot connect to NodeCore", true)
            }
        }

        val state = MiningOperation(
            chainId = chain,
            blockHeight = block
        )

        registerToStateChangedEvent(state)

        workflowAuthority.submit(state)
        operations[state.id] = state

        if (state.status != OperationStatus.UNKNOWN) {
            logger.info { "Created operation [${state.id}] on chain ${state.chainId}" }

            return success()
        } else {
            return failure()
        }
    }

    @Throws(InterruptedException::class)
    fun shutdown() {
        nodeCoreLiteKit.shutdown()
    }

    private fun loadSuspendedOperations() {
        logger.info("Loading suspended operations")

        try {
            val activeOperations = operationService.getActiveOperations { txId ->
                val hash = Sha256Hash.wrap(txId)
                nodeCoreLiteKit.transactionMonitor.getTransaction(hash)
            }

            for (state in activeOperations) {
                registerToStateChangedEvent(state)
                operations[state.id] = state
            }
        } catch (e: Exception) {
            logger.error("Unable to load suspended operations", e)
        }
    }

    private fun submitSuspendedOperations() {
        if (operations.isEmpty()) {
            return
        }

        logger.info("Submitting suspended operations")

        try {
            operations.forEachValue(4L) {
                workflowAuthority.restore(it)
            }
        } catch (e: Exception) {
            logger.error("Unable to resume suspended operations", e)
        }

        operationsSubmitted = true
    }

    private fun registerToStateChangedEvent(operation: MiningOperation) {
        operation.stateChangedEvent.register(operationService) {
            operationService.storeOperation(operation)
        }
    }
}
