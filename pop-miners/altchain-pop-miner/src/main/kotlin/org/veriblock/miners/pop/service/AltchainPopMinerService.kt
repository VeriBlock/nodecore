// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

@file:JvmName("AltchainPoPMinerKt")

package org.veriblock.miners.pop.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.veriblock.core.MineException
import org.veriblock.core.utilities.createLogger
import org.veriblock.lite.NodeCoreLiteKit
import org.veriblock.core.contracts.Balance
import org.veriblock.lite.core.Context
import org.veriblock.lite.util.Threading
import org.veriblock.miners.pop.core.ApmOperation
import org.veriblock.miners.pop.core.warn
import org.veriblock.miners.pop.securityinheriting.SecurityInheritingService
import org.veriblock.miners.pop.util.formatCoinAmount
import org.veriblock.sdk.alt.plugin.PluginService
import org.veriblock.core.crypto.Sha256Hash
import org.veriblock.core.utilities.debugError
import org.veriblock.miners.pop.EventBus
import org.veriblock.miners.pop.core.MiningOperationStatus
import org.veriblock.miners.pop.securityinheriting.SecurityInheritingMonitor
import org.veriblock.miners.pop.util.CheckResult
import org.veriblock.sdk.alt.SecurityInheritingChain
import org.veriblock.sdk.models.getSynchronizedMessage
import java.io.IOException
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

private val logger = createLogger {}

class AltchainPopMinerService(
    private val config: MinerConfig,
    private val context: Context,
    private val taskService: ApmTaskService,
    private val nodeCoreLiteKit: NodeCoreLiteKit,
    private val operationService: OperationService,
    private val pluginService: PluginService,
    private val securityInheritingService: SecurityInheritingService
) : MinerService {
    private val operations = ConcurrentHashMap<String, ApmOperation>()
    private var isShuttingDown: Boolean = false

    private val coroutineScope = CoroutineScope(Threading.TASK_POOL.asCoroutineDispatcher())

    override fun initialize() {
        nodeCoreLiteKit.initialize()

        // Restore & submit operations (including re-attach listeners) before the network starts
        nodeCoreLiteKit.beforeNetworkStart = { loadAndSubmitSuspendedOperations() }

        // NodeCore Events
        EventBus.nodeCoreAccessibleEvent.register(this) {
            logger.info { "Successfully connected to NodeCore at ${context.networkParameters.rpcHost}:${context.networkParameters.rpcPort}" }
        }
        EventBus.nodeCoreNotAccessibleEvent.register(this) {
            logger.info { "Unable to connect to NodeCore at ${context.networkParameters.rpcHost}:${context.networkParameters.rpcPort}, trying to reconnect..." }
        }
        EventBus.nodeCoreSynchronizedEvent.register(this) { }
        EventBus.nodeCoreNotSynchronizedEvent.register(this) { }
        EventBus.nodeCoreSameNetworkEvent.register(this) {
            logger.info { "The connected NodeCore & APM are running on the same configured network (${context.networkParameters.name})" }
        }
        EventBus.nodeCoreNotSameNetworkEvent.register(this) { }
        EventBus.nodeCoreReadyEvent.register(this) {
            logger.info { "The connected NodeCore is ready" }
        }
        EventBus.nodeCoreNotReadyEvent.register(this) {
            logger.info { "The connected NodeCore is not ready" }
        }
        // Altchain Events
        EventBus.altChainAccessibleEvent.register(this) {
            pluginService[it]?.let {
                logger.info { "Successfully connected to ${it.name} chain at ${it.config.host}" }
            }
        }
        EventBus.altChainNotAccessibleEvent.register(this) {
            pluginService[it]?.let {
                logger.info { "Unable to connect to ${it.name} chain at ${it.config.host}, trying to reconnect..." }
            }
        }
        EventBus.altChainSynchronizedEvent.register(this) { }
        EventBus.altChainNotSynchronizedEvent.register(this) { }
        EventBus.altChainSameNetworkEvent.register(this) {
            logger.info { "The connected $it chain & APM are running on the same configured network (${context.networkParameters.name})" }
        }
        EventBus.altChainNotSameNetworkEvent.register(this) { }
        EventBus.altChainReadyEvent.register(this) {
            logger.info { "$it chain is ready" }
        }
        EventBus.altChainNotReadyEvent.register(this) {
            logger.info { "$it chain is not ready" }
        }
        // Balance Events
        EventBus.sufficientBalanceEvent.register(this) { balance ->
            logger.info { "PoP wallet contains sufficient funds: ${balance.confirmedBalance.formatCoinAmount()} ${context.vbkTokenName}" }
        }
        EventBus.insufficientBalanceEvent.register(this) { }
        EventBus.balanceChangeEvent.register(this) {
            logger.info { "Current balance: ${it.confirmedBalance.formatCoinAmount()} ${context.vbkTokenName}" }
        }
        // Operation Events
        EventBus.operationStateChangedEvent.register(this) {
            operationService.storeOperation(it)
        }
        EventBus.operationFinishedEvent.register(this) {
            operations.remove(it.id)
        }
    }

    override fun start() {
        logger.info("Starting miner...")
        try {
            nodeCoreLiteKit.start()
        } catch (e: IOException) {
            throw IllegalStateException("Miner could not be started", e)
        }
    }

    override fun getOperations(status: MiningOperationStatus, limit: Int, offset: Int): List<ApmOperation> {
        return if (status == MiningOperationStatus.ACTIVE) {
            operations.values.asSequence().sortedBy {
                it.createdAt
            }.drop(
                offset.toInt()
            ).take(
                limit
            ).toList()
        } else {
            operationService.getOperations(status, limit, offset) { txId ->
                val hash = Sha256Hash.wrap(txId)
                nodeCoreLiteKit.transactionMonitor.getTransaction(hash)
            }.map {
                it
            }.toList()
        }
    }

    override fun getOperationsCount(status: MiningOperationStatus): Int {
        return if (status == MiningOperationStatus.ACTIVE) {
            operations.size
        } else {
            operationService.getOperationsCount(status)
        }
    }

    override fun getOperation(id: String): ApmOperation? {
        return operations[id] ?: operationService.getOperation(id) { txId ->
            val hash = Sha256Hash.wrap(txId)
            nodeCoreLiteKit.transactionMonitor.getTransaction(hash)
        }
    }

    override fun getAddress(): String = nodeCoreLiteKit.getAddress()

    override fun getBalance(): Balance = nodeCoreLiteKit.network.latestBalance

    private fun checkReadyConditions(chain: SecurityInheritingChain, monitor: SecurityInheritingMonitor, block: Int?): CheckResult  {
        // Check the last operation time
        val lastOperationTime = getOperations().maxOfOrNull { it.createdAt }
        val currentTime = LocalDateTime.now()
        if (lastOperationTime != null && currentTime < lastOperationTime.plusSeconds(1)) {
            return CheckResult.Failure(MineException("It's been less than a second since you started the previous mining operation! Please, wait at least 1 second to start a new mining operation"))
        }
        // Verify if the miner is shutting down
        if (isShuttingDown) {
            return CheckResult.Failure(MineException("Unable to mine, the miner is currently shutting down"))
        }
        // Specific checks for the NodeCore
        if (!nodeCoreLiteKit.network.isAccessible()) {
            return CheckResult.Failure(MineException("Unable to connect to NodeCore at ${context.networkParameters.rpcHost}:${context.networkParameters.rpcPort}, is it reachable?"))
        }
        // Verify the NodeCore configured Network
        if (!nodeCoreLiteKit.network.isOnSameNetwork()) {
            return CheckResult.Failure(MineException("The connected NodeCore (${nodeCoreLiteKit.network.latestNodeCoreStateInfo.networkVersion}) & APM (${context.networkParameters.name}) are not running on the same configured network"))
        }
        // Verify the balance
        if (!nodeCoreLiteKit.network.isSufficientFunded()) {
            return CheckResult.Failure(MineException("""
                PoP wallet does not contain sufficient funds, 
                Current balance: ${nodeCoreLiteKit.network.latestBalance.confirmedBalance.atomicUnits.formatCoinAmount()} ${context.vbkTokenName},
                Minimum required: ${config.maxFee.formatCoinAmount()}, need ${(config.maxFee - nodeCoreLiteKit.network.latestBalance.confirmedBalance.atomicUnits).formatCoinAmount()} more
                Send ${context.vbkTokenName} coins to: ${nodeCoreLiteKit.addressManager.defaultAddress.hash}
            """.trimIndent()))
        }
        // Verify the synchronized status
        if (!nodeCoreLiteKit.network.isSynchronized()) {
            return CheckResult.Failure(MineException("The connected NodeCore is not synchronized: ${nodeCoreLiteKit.network.latestNodeCoreStateInfo.getSynchronizedMessage()}"))
        }
        // Specific checks for the alt chain
        if (!monitor.isAccessible()) {
            return CheckResult.Failure(MineException("The miner is not connected to the ${chain.name} chain at ${chain.config.host}, is it reachable?"))
        }
        // Verify if the alt chain daemon is running on the same network as the APM
        if (!monitor.isOnSameNetwork()) {
            return CheckResult.Failure(MineException("The connected ${chain.name} (${monitor.latestBlockChainInfo.networkVersion}) & APM (${context.networkParameters.name}) are not running on the same configured network"))
        }
        // Verify the synchronized status
        if (!monitor.isSynchronized()) {
            return CheckResult.Failure(MineException("The chain ${chain.name} is not synchronized: ${monitor.latestBlockChainInfo.getSynchronizedMessage()}"))
        }
        // Verify if the block is too old to be mined
        if (block != null && block < monitor.latestBlockChainInfo.localBlockchainHeight - chain.getPayoutDelay() * 0.8) {
            return CheckResult.Failure(MineException("The block @ $block is too old to be mined. Its endorsement wouldn't be accepted by the ${chain.name} network."))
        }
        return CheckResult.Success()
    }

    override fun mine(chainId: String, block: Int?): String {
        val chain = pluginService[chainId]
            ?: throw MineException("Unable to find altchain plugin '$chainId'")
        val chainMonitor = securityInheritingService.getMonitor(chainId)
            ?: error("Unable to load altchain monitor $chainId")

        // Verify all the mine pre-conditions
        val result = checkReadyConditions(chain, chainMonitor, block)
        if (result is CheckResult.Failure) {
            throw result.error
        }

        val operation = ApmOperation(
            endorsedBlockHeight = block,
            chain = chain,
            chainMonitor = chainMonitor
        )

        operation.state
        submit(operation)
        operations[operation.id] = operation

        logger.info { "Created operation [${operation.id}] on chain ${operation.chain.name}" }

        return operation.id
    }

    override fun cancelOperation(id: String) {
        val operation = operations[id]
            ?: error(String.format("Could not find operation with id '%s'", id))
        cancel(operation)
    }

    override fun shutdown() {
        nodeCoreLiteKit.shutdown()
    }

    override fun setIsShuttingDown(b: Boolean) {
        isShuttingDown = b
    }

    private fun loadAndSubmitSuspendedOperations() {
        try {
            val activeOperations = operationService.getActiveOperations { txId ->
                val hash = Sha256Hash.wrap(txId)
                nodeCoreLiteKit.transactionMonitor.getTransaction(hash)
            }
            for (state in activeOperations) {
                operations[state.id] = state
            }
            logger.info("Loaded ${activeOperations.size} suspended operations")

            if (activeOperations.isNotEmpty()) {
                coroutineScope.launch {
                    submitSuspendedOperations(activeOperations)
                }
            } else {
                logger.info { "There are no suspended operations to submitted..." }
            }
        } catch (e: Exception) {
            logger.debugError(e) { "Unable to load suspended operations" }
        }
    }

    private suspend fun submitSuspendedOperations(activeOperations: List<ApmOperation>) {
        logger.info("Submitting suspended operations")
        try {
            val operationsToSubmit = ArrayList(activeOperations)
            while (operationsToSubmit.isNotEmpty()) {
                if (nodeCoreLiteKit.network.isReady()) {
                    val operationsToRemove = ArrayList<ApmOperation>()
                    for (operation in operationsToSubmit) {
                        if (!operation.state.isDone() && operation.job == null) {
                            val chainMonitor = securityInheritingService.getMonitor(operation.chain.key)
                            if (chainMonitor != null && chainMonitor.isReady()) {
                                submit(operation)
                                operationsToRemove.add(operation)
                            }
                        }
                    }
                    operationsToSubmit.removeAll(operationsToRemove)
                }
                delay(5 * 1000)
            }
        } catch (e: Exception) {
            logger.debugError(e) { "Unable to resume suspended operations" }
        }
        logger.info { "All the suspended operations have been submitted" }
    }

    private fun submit(operation: ApmOperation) {
        if (operation.job != null) {
            logger.warn(operation, "Trying to submit operation while it already had a running job!")
            return
        }
        operation.job = coroutineScope.launch {
            taskService.runTasks(operation)
        }
    }

    private fun cancel(operation: ApmOperation) {
        if (operation.job == null) {
            error("Trying to cancel operation [${operation.id}] while it doesn't have a running job!")
        }
        operation.fail("Cancellation requested by the user")
    }
}
