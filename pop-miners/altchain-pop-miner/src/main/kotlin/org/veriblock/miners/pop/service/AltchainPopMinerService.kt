// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.veriblock.core.MineException
import org.veriblock.core.utilities.createLogger
import org.veriblock.core.contracts.Balance
import org.veriblock.miners.pop.core.ApmContext
import org.veriblock.miners.pop.util.Threading
import org.veriblock.miners.pop.core.ApmOperation
import org.veriblock.miners.pop.core.warn
import org.veriblock.miners.pop.securityinheriting.SecurityInheritingService
import org.veriblock.miners.pop.util.formatCoinAmount
import org.veriblock.sdk.alt.plugin.PluginService
import org.veriblock.core.params.NetworkParameters
import org.veriblock.core.utilities.debugError
import org.veriblock.core.utilities.debugWarn
import org.veriblock.miners.pop.transactionmonitor.TM_FILE_EXTENSION
import org.veriblock.miners.pop.transactionmonitor.TransactionMonitor
import org.veriblock.miners.pop.transactionmonitor.loadTransactionMonitor
import org.veriblock.miners.pop.EventBus
import org.veriblock.miners.pop.MinerConfig
import org.veriblock.miners.pop.core.MiningOperationStatus
import org.veriblock.miners.pop.securityinheriting.SecurityInheritingMonitor
import org.veriblock.miners.pop.util.CheckResult
import org.veriblock.sdk.alt.SecurityInheritingChain
import org.veriblock.sdk.models.StateInfo
import org.veriblock.sdk.models.getSynchronizedMessage
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import org.veriblock.miners.pop.NodeCoreLiteKit

private val logger = createLogger {}

class AltchainPopMinerService(
    private val config: MinerConfig,
    private val context: ApmContext,
    private val taskService: ApmTaskService,
    private val operationService: OperationService,
    private val pluginService: PluginService,
    private val securityInheritingService: SecurityInheritingService,
    private val nodeCoreLiteKit: NodeCoreLiteKit
) {
    private val operations = ConcurrentHashMap<String, ApmOperation>()
    private var isShuttingDown: Boolean = false

    private val coroutineScope = CoroutineScope(Threading.TASK_POOL.asCoroutineDispatcher())

    fun initialize() {
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

    fun start() {
        logger.info("Starting miner...")
        try {
            logger.info { "VeriBlock Network: ${context.networkParameters.name}" }

            logger.info { "Send funds to the ${context.vbkTokenName} wallet ${nodeCoreLiteKit.addressManager.defaultAddress.hash}" }
            logger.info { "Connecting to peers..." }

            // Restore & submit operations (including re-attach listeners) before the network starts
            loadAndSubmitSuspendedOperations()
            // Start the network monitoring
            nodeCoreLiteKit.start()
        } catch (e: IOException) {
            throw IllegalStateException("Miner could not be started", e)
        }
    }

    fun getOperations(altchainKey: String? = null, status: MiningOperationStatus = MiningOperationStatus.ACTIVE, limit: Int = 50, offset: Int = 0): List<ApmOperation> {
        return if (status == MiningOperationStatus.ACTIVE) {
            operations.values.asSequence()
                .filter { altchainKey == null || it.chain.key == altchainKey }
                .sortedWith(compareByDescending<ApmOperation> { it.createdAt }.thenByDescending { it.endorsedBlockHeight })
                .drop(offset)
                .take(limit)
                .toList()
        } else {
            operationService.getOperations(altchainKey, status, limit, offset) { txId ->
                nodeCoreLiteKit.transactionMonitor.getTransaction(txId)
            }.sortedByDescending { it.endorsedBlockHeight }
        }
    }

    fun getOperationsCount(altchainKey: String? = null, status: MiningOperationStatus = MiningOperationStatus.ACTIVE): Int {
        return if (status == MiningOperationStatus.ACTIVE) {
            operations.size
        } else {
            operationService.getOperationsCount(altchainKey, status)
        }
    }

    fun getOperation(id: String): ApmOperation? {
        return operations[id] ?: operationService.getOperation(id) { txId ->
            nodeCoreLiteKit.transactionMonitor.getTransaction(txId)
        }
    }

    fun getAddress(): String = nodeCoreLiteKit.getAddress()

    fun getBalance(): Balance = nodeCoreLiteKit.network.latestBalance

    fun checkReadyConditions(): CheckResult  {
        // Verify if the miner is shutting down
        if (isShuttingDown) {
            return CheckResult.Failure(MineException("Unable to mine, the miner is currently shutting down"))
        }
        // Specific checks for the NodeCore
        if (!nodeCoreLiteKit.network.isAccessible()) {
            return CheckResult.Failure(MineException("Unable to connect to VBK peers"))
        }
        // Verify the balance
        if (!nodeCoreLiteKit.network.isSufficientFunded()) {
            return CheckResult.Failure(MineException("""
                VBK PoP wallet does not contain sufficient funds, 
                Current balance: ${nodeCoreLiteKit.network.latestBalance.confirmedBalance.atomicUnits.formatCoinAmount()} ${context.vbkTokenName},
                Minimum required: ${config.maxFee.formatCoinAmount()}, need ${(config.maxFee - nodeCoreLiteKit.network.latestBalance.confirmedBalance.atomicUnits).formatCoinAmount()} more
                Send ${context.vbkTokenName} coins to: ${nodeCoreLiteKit.addressManager.defaultAddress.hash}
            """.trimIndent()))
        }
        // Verify the synchronized status
        if (!nodeCoreLiteKit.network.isSynchronized()) {
            return CheckResult.Failure(MineException("VBK SPV is not synchronized: ${nodeCoreLiteKit.network.latestNodeCoreStateInfo.getSynchronizedMessage()}"))
        }
        return CheckResult.Success()
    }

    private fun checkReadyConditions(chain: SecurityInheritingChain, monitor: SecurityInheritingMonitor, blockHeight: Int?): CheckResult  {
        // Verify if the miner is shutting down
        val globalReadyCheckResult = checkReadyConditions()
        if (globalReadyCheckResult !is CheckResult.Success) {
            return globalReadyCheckResult
        }
        // Specific checks for the alt chain
        val altchainConditions = checkAltChainReadyConditions(chain.key, chain, monitor)
        if (altchainConditions is CheckResult.Failure) {
            return altchainConditions
        }
        val latestKnownHeight = try {
            runBlocking { chain.getBestBlockHeight() }
        } catch (ignored: Exception) {
            monitor.latestBlockChainInfo.localBlockchainHeight
        }
        // Verify if the block is too old to be mined
        if (blockHeight != null && blockHeight < latestKnownHeight - chain.getPayoutDelay() * 0.8) {
            return CheckResult.Failure(MineException("The block @ $blockHeight is too old to be mined. Its endorsement wouldn't be accepted by the ${chain.name} network."))
        }
        // Verify if the block is too old to be mined
        if (blockHeight != null && blockHeight > latestKnownHeight) {
            return CheckResult.Failure(MineException("There is no block @ $blockHeight known by the ${chain.name} daemon. The latest known block is at height ${latestKnownHeight}."))
        }
        return CheckResult.Success()
    }

    fun checkAltChainReadyConditions(
        chainId: String,
        chain: SecurityInheritingChain = pluginService[chainId] ?: error("Unable to find altchain plugin '$chainId'"),
        monitor: SecurityInheritingMonitor = securityInheritingService.getMonitor(chainId) ?: error("Unable to load altchain monitor $chainId")
    ): CheckResult {
        // Verify if the miner is connected to the alt chain daemon
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
        return CheckResult.Success()
    }

    fun mine(chainId: String, block: Int?): String {
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

        logger.info { "Created operation [${operation.id}] on chain ${operation.chain.name} ${(block?.let { "at block @$block" } ?: "")}" }

        return operation.id
    }

    fun cancelOperation(id: String) {
        val operation = operations[id]
            ?: error(String.format("Could not find operation with id '%s'", id))
        cancel(operation)
    }

    fun shutdown() {
        nodeCoreLiteKit.shutdown()
    }

    fun setIsShuttingDown(b: Boolean) {
        isShuttingDown = b
    }

    private fun loadAndSubmitSuspendedOperations() {
        try {
            val activeOperations = operationService.getActiveOperations { txId ->
                nodeCoreLiteKit.transactionMonitor.getTransaction(txId)
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
                delay(5 * 1000L)
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

    fun getStateInfo(chainId: String): StateInfo? =
        securityInheritingService.getMonitor(chainId)?.latestBlockChainInfo
}
