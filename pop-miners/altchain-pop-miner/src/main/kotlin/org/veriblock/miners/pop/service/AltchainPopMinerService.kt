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
import org.veriblock.miners.pop.net.SpvGateway
import org.veriblock.miners.pop.net.VeriBlockNetwork
import org.veriblock.miners.pop.transactionmonitor.TM_FILE_EXTENSION
import org.veriblock.miners.pop.transactionmonitor.TransactionMonitor
import org.veriblock.miners.pop.transactionmonitor.loadTransactionMonitor
import org.veriblock.miners.pop.EventBus
import org.veriblock.miners.pop.MEMPOOL_CHAIN_LIMIT
import org.veriblock.miners.pop.MinerConfig
import org.veriblock.miners.pop.core.ApmOperationState
import org.veriblock.miners.pop.core.MiningOperationStatus
import org.veriblock.miners.pop.securityinheriting.SecurityInheritingMonitor
import org.veriblock.miners.pop.util.CheckResult
import org.veriblock.sdk.alt.SecurityInheritingChain
import org.veriblock.sdk.models.Address
import org.veriblock.sdk.models.StateInfo
import org.veriblock.sdk.models.getSynchronizedMessage
import org.veriblock.spv.SpvConfig
import org.veriblock.spv.SpvContext
import org.veriblock.spv.model.DownloadStatusResponse
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

private val logger = createLogger {}

class AltchainPopMinerService(
    private val config: MinerConfig,
    private val context: ApmContext,
    private val taskService: ApmTaskService,
    private val operationService: OperationService,
    private val pluginService: PluginService,
    private val securityInheritingService: SecurityInheritingService
) {
    lateinit var spvContext: SpvContext
    lateinit var transactionMonitor: TransactionMonitor
    lateinit var gateway: SpvGateway
    lateinit var network: VeriBlockNetwork

    private val operations = ConcurrentHashMap<String, ApmOperation>()
    private var isShuttingDown: Boolean = false

    private val coroutineScope = CoroutineScope(Threading.TASK_POOL.asCoroutineDispatcher())

    fun initialize() {
        if (!context.directory.exists() && !context.directory.mkdirs()) {
            throw IOException("Unable to create directory")
        }

        spvContext = initSpvContext(context.networkParameters)
        gateway = SpvGateway(context.networkParameters, spvContext.spvService)
        transactionMonitor = createOrLoadTransactionMonitor()

        network = VeriBlockNetwork(
            config,
            context,
            gateway,
            transactionMonitor,
            spvContext.addressManager
        )

        transactionMonitor.start()

        // SPV Events
        EventBus.spvReadyEvent.register(this) {
            logger.info { "SPV is ready" }
        }
        EventBus.spvNotReadyEvent.register(this) {
            logger.info { "SPV is not ready" }
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

            logger.info { "Send funds to the ${context.vbkTokenName} wallet ${spvContext.addressManager.defaultAddress.hash}" }
            logger.info { "Connecting to peers..." }

            // Restore & submit operations (including re-attach listeners) before the network starts
            loadAndSubmitSuspendedOperations()
            // Start the network monitoring
            network.startAsync()
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
                transactionMonitor.getTransaction(txId)
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
            transactionMonitor.getTransaction(txId)
        }
    }

    fun getAddress(): String = spvContext.addressManager.defaultAddress.hash

    fun getBalance(): Balance = network.latestBalance

    private fun checkReadyConditions(chain: SecurityInheritingChain, monitor: SecurityInheritingMonitor, blockHeight: Int?): CheckResult  {
        // Verify if the miner is shutting down
        if (isShuttingDown) {
            return CheckResult.Failure(MineException("Unable to mine, the miner is currently shutting down"))
        }
        // Specific checks for the NodeCore
        if (!network.isAccessible()) {
            return CheckResult.Failure(MineException("Unable to connect to peers"))
        }
        // Verify the balance
        if (!network.isSufficientFunded()) {
            return CheckResult.Failure(MineException("""
                PoP wallet does not contain sufficient funds, 
                Current balance: ${network.latestBalance.confirmedBalance.atomicUnits.formatCoinAmount()} ${context.vbkTokenName},
                Minimum required: ${config.maxFee.formatCoinAmount()}, need ${(config.maxFee - network.latestBalance.confirmedBalance.atomicUnits).formatCoinAmount()} more
                Send ${context.vbkTokenName} coins to: ${spvContext.addressManager.defaultAddress.hash}
            """.trimIndent()))
        }
        // Verify the synchronized status
        if (!network.isSynchronized()) {
            return CheckResult.Failure(MineException("SPV is not synchronized: ${network.latestSpvStateInfo.getSynchronizedMessage()}"))
        }
        // Specific checks for the alt chain
        val altchainConditions = checkAltChainReadyConditions(chain.key, chain, monitor)
        if (altchainConditions is CheckResult.Failure) {
            return altchainConditions
        }
        // Verify the limit for the unconfirmed endorsement transactions
        val count = operations.values.count { ApmOperationState.ENDORSEMENT_TRANSACTION.hasType(it.state) }
        if (count >= MEMPOOL_CHAIN_LIMIT) {
            return CheckResult.Failure(MineException("Too Many Pending Transaction operations. Creating additional operations at this time would result in rejection on the VeriBlock network"))
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
        if (this::network.isInitialized) {
            network.shutdown()
        }
    }

    fun setIsShuttingDown(b: Boolean) {
        isShuttingDown = b
    }

    private fun loadAndSubmitSuspendedOperations() {
        try {
            val activeOperations = operationService.getActiveOperations { txId ->
                transactionMonitor.getTransaction(txId)
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
                if (network.isReady()) {
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

    private fun createOrLoadTransactionMonitor(): TransactionMonitor {
        val file = File(context.directory, context.filePrefix + TM_FILE_EXTENSION)
        return if (file.exists()) {
            try {
                file.loadTransactionMonitor(context, gateway)
            } catch (e: Exception) {
                logger.debugWarn(e) { "Unable to load the transaction monitoring data, trying to recreate..." }
                if (file.delete()) {
                    createTransactionMonitor()
                } else {
                    throw IOException("Unable to load the transaction monitoring data", e)
                }
            }
        } else {
            createTransactionMonitor()
        }
    }

    private fun createTransactionMonitor(): TransactionMonitor {
        val address = Address(spvContext.addressManager.defaultAddress.hash)
        return TransactionMonitor(context, gateway, address)
    }

    private fun initSpvContext(networkParameters: NetworkParameters): SpvContext {
        logger.info { "Initializing SPV..." }
        val spvContext = SpvContext(
            SpvConfig(
                networkParameters,
                dataDir = context.dataDir,
                connectDirectlyTo = config.connectDirectlyTo,
                trustPeerHashes = true
            )
        )
        spvContext.start()
        GlobalScope.launch {
            while (true) {
                val status: DownloadStatusResponse = spvContext.spvService.getDownloadStatus()
                if (status.downloadStatus.isDiscovering()) {
                    logger.info { "SPV: Waiting for peers response." }
                } else if (status.downloadStatus.isDownloading()) {
                    logger.info { "SPV: Blockchain is downloading. " + status.currentHeight + " / " + status.bestHeight }
                } else {
                    logger.info { "SPV: Blockchain is ready. Current height " + status.currentHeight }
                    break
                }
                delay(5000L)
            }
        }

        return spvContext
    }

    fun getStateInfo(chainId: String): StateInfo? =
        securityInheritingService.getMonitor(chainId)?.latestBlockChainInfo
}
