// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop

import org.veriblock.lite.NodeCoreLiteKit
import org.veriblock.miners.pop.core.MiningOperation
import org.veriblock.miners.pop.core.OperationStatus
import org.veriblock.miners.pop.storage.OperationService
import org.veriblock.miners.pop.tasks.WorkflowAuthority
import org.veriblock.sdk.Configuration
import org.veriblock.sdk.Sha256Hash
import org.veriblock.sdk.createLogger
import org.veriblock.shell.core.Result
import org.veriblock.shell.core.failure
import org.veriblock.shell.core.success
import java.io.IOException
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

    init {
        // Restore operations (including re-attach listeners) before the network starts
        this.nodeCoreLiteKit.beforeNetworkStart = { this.loadSuspendedOperations() }
        this.nodeCoreLiteKit.afterNetworkStart = { this.submitSuspendedOperations() }
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

    fun getBalance() = nodeCoreLiteKit.network.getBalance()

    fun mine(chain: String, block: Int?): Result {
        if (!nodeCoreLiteKit.network.isHealthy()) {
            return failure {
                addMessage("V010", "Unable to mine", "Cannot connect to NodeCore", true)
            }
        }

        val currentBalance = getBalance().confirmedBalance.atomicUnits
        if (currentBalance < minerConfig.maxFee) {
            return failure {
                addMessage(
                    "V011",
                    "Insufficient funds",
                    "Current confirmed balance is $currentBalance while the configured maximum fee is ${minerConfig.maxFee}",
                    true
                )
                addMessage(
                    "V011",
                    "Please send VBK coins to ${nodeCoreLiteKit.addressManager.defaultAddress}",
                    "You should send at least ${minerConfig.maxFee - currentBalance} atomic units of VBK",
                    false
                )
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
    }

    private fun registerToStateChangedEvent(operation: MiningOperation) {
        operation.stateChangedEvent.register(operationService) {
            operationService.storeOperation(operation)
        }
    }
}
