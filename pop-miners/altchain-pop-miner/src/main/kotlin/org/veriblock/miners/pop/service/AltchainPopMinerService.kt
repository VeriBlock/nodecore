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
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.veriblock.core.CommunicationException
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
import org.veriblock.sdk.models.Coin
import org.veriblock.core.crypto.Sha256Hash
import org.veriblock.core.utilities.debugError
import org.veriblock.miners.pop.EventBus
import java.io.IOException
import java.time.LocalDateTime
import java.util.EnumSet
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

    private enum class ReadyCondition {
        SUFFICIENT_FUNDS,
        NODECORE_CONNECTED,
        SYNCHRONIZED_NODECORE
    }

    private val readyConditions: EnumSet<ReadyCondition> = EnumSet.noneOf(ReadyCondition::class.java)

    private var operationsSubmitted = false
    private var lastConfirmedBalance = Coin.ZERO

    override fun initialize() {
        nodeCoreLiteKit.initialize()

        // Restore operations (including re-attach listeners) before the network starts
        this.nodeCoreLiteKit.beforeNetworkStart = { loadSuspendedOperations() }

        EventBus.nodeCoreHealthyEvent.register(this) {
            logger.info { "Successfully connected to NodeCore, waiting for the sync status..." }
            addReadyCondition(ReadyCondition.NODECORE_CONNECTED)
        }
        EventBus.nodeCoreUnhealthyEvent.register(this) {
            logger.info { "Unable to connect to NodeCore at this time, trying to reconnect..." }
            removeReadyCondition(ReadyCondition.NODECORE_CONNECTED)
        }
        EventBus.nodeCoreHealthySyncEvent.register(this) {
            logger.info { "The connected NodeCore is synchronized" }
            addReadyCondition(ReadyCondition.SYNCHRONIZED_NODECORE)
        }
        EventBus.nodeCoreUnhealthySyncEvent.register(this) {
            removeReadyCondition(ReadyCondition.SYNCHRONIZED_NODECORE)
        }
        EventBus.balanceChangedEvent.register(this) {
            if (lastConfirmedBalance != it.confirmedBalance) {
                lastConfirmedBalance = it.confirmedBalance
                logger.info { "Current balance: ${it.confirmedBalance.formatCoinAmount()} ${context.vbkTokenName}" }
            }
            if (it.confirmedBalance.atomicUnits >= config.maxFee) {
                addReadyCondition(ReadyCondition.SUFFICIENT_FUNDS)
            } else {
                removeReadyCondition(ReadyCondition.SUFFICIENT_FUNDS)
            }
        }
        EventBus.operationStateChangedEvent.register(this) {
            operationService.storeOperation(it)
        }
        EventBus.operationFinishedEvent.register(this) {
            operations.remove(it.id)
        }
    }

    private fun isReady(): Boolean {
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
        ReadyCondition.SYNCHRONIZED_NODECORE -> {
           val nodeCoreStateInfo = nodeCoreLiteKit.network.getNodeCoreStateInfo()
            "Waiting for NodeCore to synchronize. ${nodeCoreStateInfo.blockDifference} blocks left (LocalHeight=${nodeCoreStateInfo.localBlockchainHeight} NetworkHeight=${nodeCoreStateInfo.networkHeight})"
        }
        ReadyCondition.SUFFICIENT_FUNDS -> {
            val currentBalance = getBalance()?.confirmedBalance ?: Coin.ZERO
            """
                PoP wallet does not contain sufficient funds
                         Current balance: ${currentBalance.atomicUnits.formatCoinAmount()} ${context.vbkTokenName}
                         Minimum required: ${config.maxFee.formatCoinAmount()}, need ${(config.maxFee - currentBalance.atomicUnits).formatCoinAmount()} more
                         Send ${context.vbkTokenName} coins to: ${nodeCoreLiteKit.addressManager.defaultAddress.hash}
            """.trimIndent()
        }
    }

    private fun listNotReadyConditions() = EnumSet.complementOf(readyConditions).map {
        it.getNotReadyMessage()
    }

    override fun start() {
        logger.info("Starting miner...")
        try {
            nodeCoreLiteKit.start()
        } catch (e: IOException) {
            throw IllegalStateException("Miner could not be started", e)
        }
    }

    override fun getOperations() = operations.values.sortedBy { it.createdAt }

    override fun getOperation(id: String): ApmOperation? {
        return operations[id] ?: operationService.getOperation(id) { txId ->
            val hash = Sha256Hash.wrap(txId)
            nodeCoreLiteKit.transactionMonitor.getTransaction(hash)
        }
    }

    override fun getStoredOperationsByState(state: Int, limit: Int): List<ApmOperation> {
        return operationService.getOperationsByState(state, limit) { txId ->
            val hash = Sha256Hash.wrap(txId)
            nodeCoreLiteKit.transactionMonitor.getTransaction(hash)
        }.map {
            it
        }.toList()
    }

    override fun getAddress(): String = nodeCoreLiteKit.getAddress()

    override fun getBalance(): Balance? = if (nodeCoreLiteKit.network.isHealthy()) {
        nodeCoreLiteKit.network.getBalance()
    } else {
        null
    }

    override fun sendCoins(destinationAddress: String, atomicAmount: Long): List<String> = if (nodeCoreLiteKit.network.isHealthy()) {
        nodeCoreLiteKit.network.sendCoins(destinationAddress, atomicAmount)
    } else {
        throw CommunicationException("NodeCore is not healthy")
    }

    override fun mine(chainId: String, block: Int?): String {
        val chain = pluginService[chainId]
            ?: throw MineException("Unable to find altchain plugin '$chainId'")

        if (!isReady()) {
            throw MineException("Miner is not ready: ${listNotReadyConditions()}")
        }
        if (!nodeCoreLiteKit.network.isHealthy()) {
            throw CommunicationException("Cannot connect to NodeCore")
        }
        if (isShuttingDown) {
            throw MineException("The miner is currently shutting down")
        }
        runBlocking {
            if (!chain.isConnected()) {
                throw MineException("The miner is not connected to the ${chain.name} chain")
            }
            val blockChainInfo = chain.getBlockChainInfo()
            if (!blockChainInfo.isSynchronized) {
                throw MineException("The chain ${chain.name} is not synchronized, ${blockChainInfo.blockDifference} blocks left (LocalHeight=${blockChainInfo.localBlockchainHeight} NetworkHeight=${blockChainInfo.networkHeight} InitialBlockDownload=${blockChainInfo.initialblockdownload})")
            }
            if (!context.networkParameters.name.replace("net", "").equals(blockChainInfo.networkVersion, true)) {
                throw MineException("Network misconfiguration, APM is configured at the ${context.networkParameters.name} network while the $chain daemon is at ${blockChainInfo.networkVersion}.")
            }
            if (block != null && block < blockChainInfo.localBlockchainHeight - chain.getPayoutInterval() * 0.8) {
                throw MineException("The block @ $block is too old to be mined. Its endorsement wouldn't be accepted by the ${chain.name} network.")
            }
        }

        val lastOperationTime = getOperations().maxBy { it.createdAt }?.createdAt
        val currentTime = LocalDateTime.now()

        if (lastOperationTime != null && currentTime < lastOperationTime.plusSeconds(1)) {
            throw MineException("It's been less than a second since you started the previous mining operation! Please, wait at least 1 second to start a new mining operation.")
        }

        val chainMonitor = securityInheritingService.getMonitor(chainId)
            ?: error("Unable to load altchain monitor $chainId")

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

    override fun resubmit(operation: ApmOperation) {
        if (operation.publicationData == null) {
            error("The operation [${operation.id}] has no context to be resubmitted!")
        }

        // Copy the operation
        val newOperation = ApmOperation(
            chain = operation.chain,
            chainMonitor = operation.chainMonitor,
            endorsedBlockHeight = operation.endorsedBlockHeight,
            reconstituting = true
        )

        // Replicate its state up until prior to the PoP data submission
        newOperation.setMiningInstruction(operation.miningInstruction!!)
        newOperation.setTransaction(operation.endorsementTransaction!!)
        newOperation.setConfirmed()
        newOperation.setBlockOfProof(operation.blockOfProof!!)
        newOperation.setMerklePath(operation.merklePath!!)
        newOperation.setContext(operation.publicationData!!)
        newOperation.reconstituting = false

        // Submit new operation
        submit(newOperation)
        operations[newOperation.id] = newOperation

        logger.info { "Resubmitted operation [${operation.id}] as new operation [${newOperation.id}]" }
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

    private fun loadSuspendedOperations() {
        try {
            val activeOperations = operationService.getActiveOperations { txId ->
                val hash = Sha256Hash.wrap(txId)
                nodeCoreLiteKit.transactionMonitor.getTransaction(hash)
            }

            for (state in activeOperations) {
                operations[state.id] = state
            }
            logger.info("Loaded ${activeOperations.size} suspended operations")
        } catch (e: Exception) {
            logger.debugError(e) {"Unable to load suspended operations" }
        }
    }

    private fun submitSuspendedOperations() {
        if (operations.isEmpty()) {
            operationsSubmitted = true
            return
        }

        logger.info("Submitting suspended operations")

        try {
            for (operation in operations.values) {
                if (!operation.state.isDone() && operation.job == null) {
                    submit(operation)
                }
            }
        } catch (e: Exception) {
            logger.debugError(e) { "Unable to resume suspended operations" }
        }

        operationsSubmitted = true
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

    fun cancel(operation: ApmOperation) {
        if (operation.job == null) {
            error("Trying to cancel operation [${operation.id}] while it doesn't have a running job!")
        }
        operation.fail("Cancellation requested by the user")
    }
}
