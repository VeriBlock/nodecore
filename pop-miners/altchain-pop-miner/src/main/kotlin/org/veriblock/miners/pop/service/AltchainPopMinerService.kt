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
import org.veriblock.sdk.alt.SecurityInheritingChain
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

    private var operationsSubmitted = false
    private var lastConfirmedBalance = Coin.ZERO

    override fun initialize() {
        nodeCoreLiteKit.initialize()

        // Restore operations (including re-attach listeners) before the network starts
        nodeCoreLiteKit.beforeNetworkStart = { loadSuspendedOperations() }

        // Resubmit the suspended operations
        coroutineScope.launch {
            while (!operationsSubmitted) {
                if (nodeCoreLiteKit.network.isAccessible() && nodeCoreLiteKit.network.isOnSameNetwork() && nodeCoreLiteKit.network.isSynchronized()) {
                    submitSuspendedOperations()
                }
                delay(5 * 1000)
            }
        }

        EventBus.nodeCoreAccessibleEvent.register(this) {
            logger.info { "Successfully connected to NodeCore, waiting for the sync status..." }
        }
        EventBus.nodeCoreNotAccessibleEvent.register(this) {
            logger.info { "Unable to connect to NodeCore at this time, trying to reconnect..." }
        }
        EventBus.nodeCoreSynchronizedEvent.register(this) {
            logger.info { "The connected NodeCore is synchronized" }
        }
        EventBus.nodeCoreNotSynchronizedEvent.register(this) { }
        EventBus.nodeCoreSameNetworkEvent.register(this){
            logger.info { "The connected NodeCore and the APM are running on the same configured network" }
        }
        EventBus.nodeCoreNotSameNetworkEvent.register(this) { }
        EventBus.balanceChangedEvent.register(this) {
            if (lastConfirmedBalance != it.confirmedBalance) {
                lastConfirmedBalance = it.confirmedBalance
                logger.info { "Current balance: ${it.confirmedBalance.formatCoinAmount()} ${context.vbkTokenName}" }
            }
        }
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

    override fun getOperations() = operations.values.sortedBy { it.createdAt }

    override fun getOperation(id: String): ApmOperation? {
        return operations[id] ?: operationService.getOperation(id) { txId ->
            val hash = Sha256Hash.wrap(txId)
            nodeCoreLiteKit.transactionMonitor.getTransaction(hash)
        }
    }

    override fun getStoredOperationsByState(state: Int?, limit: Int): List<ApmOperation> {
        return operationService.getOperationsByState(state, limit) { txId ->
            val hash = Sha256Hash.wrap(txId)
            nodeCoreLiteKit.transactionMonitor.getTransaction(hash)
        }.map {
            it
        }.toList()
    }

    override fun getAddress(): String = nodeCoreLiteKit.getAddress()

    override fun getBalance(): Balance? = if (nodeCoreLiteKit.network.isAccessible()) {
        nodeCoreLiteKit.network.getBalance()
    } else {
        null
    }

    override fun sendCoins(destinationAddress: String, atomicAmount: Long): List<String> = if (nodeCoreLiteKit.network.isAccessible()) {
        nodeCoreLiteKit.network.sendCoins(destinationAddress, atomicAmount)
    } else {
        throw CommunicationException("NodeCore is not healthy")
    }

    private fun verifyReadyConditions(chain: SecurityInheritingChain, block: Int?) {
        // Check the last operation time
        val lastOperationTime = getOperations().maxBy { it.createdAt }?.createdAt
        val currentTime = LocalDateTime.now()
        if (lastOperationTime != null && currentTime < lastOperationTime.plusSeconds(1)) {
            throw MineException("It's been less than a second since you started the previous mining operation! Please, wait at least 1 second to start a new mining operation.")
        }
        // Verify if the miner is shutting down
        if (isShuttingDown) {
            throw MineException("Unable to mine, the miner is currently shutting down")
        }
        // Specific checks for the NodeCore
        if (!nodeCoreLiteKit.network.isAccessible()) {
            throw MineException("Unable to connect to NodeCore at ${context.networkParameters.rpcHost}@${context.networkParameters.rpcPort}, is it reachable?")
        }
        // Verify the balance
        val currentBalance = getBalance()?.confirmedBalance ?: Coin.ZERO
        if (currentBalance.atomicUnits < config.maxFee) {
            throw MineException("""
                PoP wallet does not contain sufficient funds, 
                Current balance: ${currentBalance.atomicUnits.formatCoinAmount()} ${context.vbkTokenName},
                Minimum required: ${config.maxFee.formatCoinAmount()}, need ${(config.maxFee - currentBalance.atomicUnits).formatCoinAmount()} more
                Send ${context.vbkTokenName} coins to: ${nodeCoreLiteKit.addressManager.defaultAddress.hash}
            """.trimIndent())
        }
        // Get the NodeCore state info
        val nodecoreStateInfo = nodeCoreLiteKit.network.getNodeCoreStateInfo()
        // Verify the NodeCore configured Network
        if (!nodeCoreLiteKit.network.isOnSameNetwork()) {
            throw MineException("Network misconfiguration, APM is configured at the ${context.networkParameters.name} network while NodeCore is at ${nodecoreStateInfo.networkVersion}")
        }
        // Verify the synchronized status
        if (!nodecoreStateInfo.isSynchronized) {
            throw MineException("The connected NodeCore is not synchronized: Local Block: ${nodecoreStateInfo.localBlockchainHeight}, Network Block: ${nodecoreStateInfo.networkHeight}, Block Difference: ${nodecoreStateInfo.blockDifference}")
        }
        // Specific checks for the alt chain
        runBlocking {
            // Verify the connection with the alt chain daemon
            if (!chain.isConnected()) {
                throw MineException("The miner is not connected to the ${chain.name} chain")
            }
            // Get the alt chain information
            val altChainBlockChainInfo = chain.getBlockChainInfo()
            // Verify if the alt chain daemon is running on the same network as the APM
            if (!context.networkParameters.name.replace("net", "").equals(altChainBlockChainInfo.networkVersion, true)) {
                throw MineException("Network misconfiguration, APM is configured at the ${context.networkParameters.name} network while the $chain daemon is at ${altChainBlockChainInfo.networkVersion}.")
            }
            // Verify the synchronized status
            if (!altChainBlockChainInfo.isSynchronized) {
                throw MineException("The chain ${chain.name} is not synchronized, ${altChainBlockChainInfo.blockDifference} blocks left (LocalHeight=${altChainBlockChainInfo.localBlockchainHeight} NetworkHeight=${altChainBlockChainInfo.networkHeight} InitialBlockDownload=${altChainBlockChainInfo.initialblockdownload})")
            }
            // Verify if the block is too old to be mined
            if (block != null && block < altChainBlockChainInfo.localBlockchainHeight - chain.getPayoutInterval() * 0.8) {
                throw MineException("The block @ $block is too old to be mined. Its endorsement wouldn't be accepted by the ${chain.name} network.")
            }
        }
        // Verify if there are suspended operations to be submitted, all the previous conditions should be fine to submit the suspended operations
        if (!operationsSubmitted && operations.isNotEmpty()) {
            throw MineException("Unable to mine, waiting to verify if there are suspended operations to be submitted...")
        }
    }

    override fun mine(chainId: String, block: Int?): String {
        val chain = pluginService[chainId]
            ?: throw MineException("Unable to find altchain plugin '$chainId'")
        val chainMonitor = securityInheritingService.getMonitor(chainId)
            ?: error("Unable to load altchain monitor $chainId")

        verifyReadyConditions(chain, block)

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
            logger.info { "There are no suspended operations to submitted..." }
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

    fun cancel(operation: ApmOperation) {
        if (operation.job == null) {
            error("Trying to cancel operation [${operation.id}] while it doesn't have a running job!")
        }
        operation.fail("Cancellation requested by the user")
    }
}
