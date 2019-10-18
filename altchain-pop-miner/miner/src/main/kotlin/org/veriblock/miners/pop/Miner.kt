// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop

import org.veriblock.lite.NodeCoreLiteKit
import org.veriblock.lite.wallet.WalletTransaction
import org.veriblock.miners.pop.core.MiningOperation
import org.veriblock.miners.pop.core.OperationStatus
import org.veriblock.miners.pop.storage.OperationService
import org.veriblock.miners.pop.tasks.WorkflowAuthority
import org.veriblock.sdk.Sha256Hash
import org.veriblock.sdk.createLogger
import org.veriblock.shell.core.Result
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

private val logger = createLogger {}

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

    fun mine(chain: String, block: Int?): Result {
        // TODO: Check ready conditions

        val state = MiningOperation(
            chainId = chain,
            blockHeight = block
        )

        registerToStateChangedEvent(state)

        workflowAuthority.submit(state)
        operations[state.id] = state

        if (state.status != OperationStatus.UNKNOWN) {
            logger.info { "Created operation [${state.id}] on chain ${state.chainId}" }

            return Result(false)
        } else {
            return Result(true)
        }
    }

    @Throws(InterruptedException::class)
    fun shutdown() {
        nodeCoreLiteKit.shutdown()
    }

    private fun loadSuspendedOperations() {
        logger.info("Loading suspended operations")

        try {
            val txFactory: (String) -> WalletTransaction = { txId ->
                val hash = Sha256Hash.wrap(txId)
                nodeCoreLiteKit.transactionMonitor.getTransaction(hash)
            }

            val activeOperations = operationService.getActiveOperations(txFactory)
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
