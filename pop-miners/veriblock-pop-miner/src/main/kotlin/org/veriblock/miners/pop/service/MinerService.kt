// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.miners.pop.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.tuple.Pair
import org.bitcoinj.core.Coin
import org.bitcoinj.core.InsufficientMoneyException
import org.bitcoinj.core.StoredBlock
import org.bitcoinj.utils.Threading
import org.veriblock.core.MineException
import org.veriblock.core.utilities.BlockUtility
import org.veriblock.core.utilities.createLogger
import org.veriblock.miners.pop.CoinsReceivedEventDto
import org.veriblock.miners.pop.Constants
import org.veriblock.miners.pop.EventBus
import org.veriblock.miners.pop.NewVeriBlockFoundEventDto
import org.veriblock.miners.pop.VpmConfig
import org.veriblock.miners.pop.common.CheckResult
import org.veriblock.miners.pop.common.amountToCoin
import org.veriblock.miners.pop.common.formatBTCFriendlyString
import org.veriblock.miners.pop.common.generateOperationId
import org.veriblock.miners.pop.core.VpmOperation
import org.veriblock.miners.pop.core.VpmOperationState
import org.veriblock.miners.pop.model.ApplicationExceptions.DuplicateTransactionException
import org.veriblock.miners.pop.model.ApplicationExceptions.ExceededMaxTransactionFee
import org.veriblock.miners.pop.model.ApplicationExceptions.UnableToAcquireTransactionLock
import org.veriblock.miners.pop.model.OperationSummary
import org.veriblock.miners.pop.model.result.MineResult
import org.veriblock.miners.pop.model.result.OperationNotFoundException
import org.veriblock.miners.pop.model.result.Result
import org.veriblock.miners.pop.storage.KeyValueRecord
import org.veriblock.miners.pop.storage.KeyValueRepository
import org.veriblock.miners.pop.tasks.ProcessManager
import org.veriblock.sdk.models.getSynchronizedMessage
import java.io.File
import java.io.IOException
import java.io.PrintWriter
import java.math.BigDecimal
import java.time.Instant
import java.util.HashSet
import java.util.concurrent.ConcurrentHashMap

private val logger = createLogger {}

class MinerService(
    private val config: VpmConfig,
    private val bitcoinService: BitcoinService,
    private val nodeCoreGateway: NodeCoreGateway,
    private val nodeCoreService: NodeCoreService,
    private val stateService: PopStateService,
    private val keyValueRepository: KeyValueRepository,
    private val processManager: ProcessManager
) : Runnable {
    private val operations = ConcurrentHashMap<String, VpmOperation>()
    private var isShuttingDown = false
    private var isMinerReady = false

    private val coroutineScope = CoroutineScope(Threading.THREAD_POOL.asCoroutineDispatcher())

    override fun run() {
        // Operation Events
        EventBus.popMiningOperationCompletedEvent.register(this, ::onPoPMiningOperationCompleted)
        // Balance Events
        EventBus.sufficientFundsEvent.register(this) { balance ->
            logger.info { "Available Bitcoin balance: ${balance.formatBTCFriendlyString()}" }
            logger.info { "Send Bitcoin to: ${bitcoinService.currentReceiveAddress()}" }
            verifyMinerIsReady()
        }
        EventBus.insufficientFundsEvent.register(this) { balance ->
            val maximumTransactionFee = bitcoinService.getMaximumTransactionFee()
            logger.info("""PoP wallet does not contain sufficient funds
                        Current balance: ${balance.formatBTCFriendlyString()}
                        Minimum required: ${maximumTransactionFee.formatBTCFriendlyString()}, need ${maximumTransactionFee.subtract(balance).formatBTCFriendlyString()} more",
                        Send Bitcoin to: ${bitcoinService.currentReceiveAddress()}
            """.trimIndent())
            verifyMinerIsReady()
        }
        EventBus.balanceChangedEvent.register(this) { balance ->
            logger.info { "Current balance: ${balance.formatBTCFriendlyString()}" }
        }
        // Block Events
        EventBus.newVeriBlockFoundEvent.register(this, ::onNewVeriBlockFound)
        // Bitcoin Events
        EventBus.bitcoinServiceReadyEvent.register(this) {
            logger.info { "Bitcoin service is ready" }
            logger.info { "Send Bitcoin to: ${bitcoinService.currentReceiveAddress()}" }
            verifyMinerIsReady()
        }
        EventBus.bitcoinServiceNotReadyEvent.register(this) {
            logger.info { "Bitcoin service is not ready" }
            verifyMinerIsReady()
        }
        EventBus.blockchainDownloadedEvent.register(this) {
            logger.info("Bitcoin blockchain finished downloading")
            verifyMinerIsReady()
        }
        EventBus.blockchainNotDownloadedEvent.register(this) {
            logger.info { "Bitcoin blockchain is not downloaded" }
            verifyMinerIsReady()
        }
        // NodeCore Events
        EventBus.nodeCoreAccessibleEvent.register(this) {
            logger.info { "Successfully connected to NodeCore at ${config.nodeCoreRpc.host}:${config.nodeCoreRpc.port}" }
        }
        EventBus.nodeCoreNotAccessibleEvent.register(this) {
            logger.info { "Unable to connect to NodeCore at ${config.nodeCoreRpc.host}:${config.nodeCoreRpc.port}, trying to reconnect..." }
        }
        EventBus.nodeCoreSynchronizedEvent.register(this) { }
        EventBus.nodeCoreNotSynchronizedEvent.register(this) { }
        EventBus.nodeCoreSameNetworkEvent.register(this) {
            logger.info { "The connected NodeCore & VPM are running on the same configured network (${config.bitcoin.network.name})" }
        }
        EventBus.nodeCoreNotSameNetworkEvent.register(this) { }
        EventBus.nodeCoreReadyEvent.register(this) {
            logger.info { "The connected NodeCore is ready" }
            verifyMinerIsReady()
        }
        EventBus.nodeCoreNotReadyEvent.register(this) {
            logger.info { "The connected NodeCore is not ready" }
            verifyMinerIsReady()
        }

        bitcoinService.initialize()
        nodeCoreService.initialize()

        if (!config.skipAck) {
            val data = keyValueRepository[Constants.WALLET_SEED_VIEWED_KEY]
            if (data == null || data.value != "1") {
                EventBus.walletSeedAgreementMissingEvent.trigger()
            }
        }

        coroutineScope.launch {
            restoreOperations()
        }
    }


    private fun verifyMinerIsReady() {
        if (nodeCoreService.isReady() && bitcoinService.isServiceReady() && bitcoinService.isBlockchainDownloaded() && bitcoinService.isSufficientlyFunded()) {
            if (!isMinerReady) {
                isMinerReady = true
                logger.info { "The miner is ready to start mining. Type 'help' to see available commands. Type 'mine' to start mining. " }
            }
        }
    }

    fun shutdown() {
        // Operation Events
        EventBus.popMiningOperationCompletedEvent.unregister(this)
        // Balance Events
        EventBus.sufficientFundsEvent.unregister(this)
        EventBus.insufficientFundsEvent.unregister(this)
        EventBus.balanceChangedEvent.unregister(this)
        // Block Events
        EventBus.newVeriBlockFoundEvent.unregister(this)
        // Bitcoin Events
        EventBus.bitcoinServiceReadyEvent.unregister(this)
        EventBus.bitcoinServiceNotReadyEvent.unregister(this)
        EventBus.blockchainDownloadedEvent.unregister(this)
        EventBus.blockchainNotDownloadedEvent.unregister(this)
        // NodeCore Events
        EventBus.nodeCoreAccessibleEvent.unregister(this)
        EventBus.nodeCoreNotAccessibleEvent.unregister(this)
        EventBus.nodeCoreSynchronizedEvent.unregister(this)
        EventBus.nodeCoreNotSynchronizedEvent.unregister(this)
        EventBus.nodeCoreSameNetworkEvent.unregister(this)
        EventBus.nodeCoreNotSameNetworkEvent.unregister(this)
        EventBus.nodeCoreReadyEvent.unregister(this)
        EventBus.nodeCoreNotReadyEvent.unregister(this)

        processManager.shutdown()
        bitcoinService.shutdown()
        nodeCoreService.shutdown()
    }

    fun listOperations(): List<OperationSummary> {
        return operations.values.asSequence().map { operation ->
            val miningInstruction = operation.miningInstruction
            var blockNumber = -1
            if (miningInstruction != null) {
                blockNumber = BlockUtility.extractBlockHeightFromBlockHeader(miningInstruction.endorsedBlockHeader)
            }
            OperationSummary(operation.id, blockNumber, operation.state.name, operation.getStateDescription())
        }.sortedBy {
            it.endorsedBlockNumber
        }.toList()
    }

    fun getOperation(id: String) =
        stateService.getOperation(id)

    fun checkReadyConditions(): CheckResult {
        // Verify if the miner is shutting down
        if (isShuttingDown) {
            return CheckResult.Failure(MineException("Unable to mine, the miner is currently shutting down"))
        }
        // Specific checks for Bitcoin
        if (!bitcoinService.isServiceReady()) {
            return CheckResult.Failure(MineException("Bitcoin service is not ready"))
        }
        if (!bitcoinService.isBlockchainDownloaded()) {
            return CheckResult.Failure(MineException("Bitcoin blockchain is not downloaded"))
        }
        // TODO: This is pretty naive. Wallet right now uses DefaultCoinSelector which doesn't do a great job with
        // multiple UTXO and long mempool chains. If that was improved, this count algorithm wouldn't be necessary.
        val count = operations.values.count { VpmOperationState.ENDORSEMENT_TRANSACTION.hasType(it.state) }
        if (count >= Constants.MEMPOOL_CHAIN_LIMIT) {
            return CheckResult.Failure(MineException("Too Many Pending Transaction operations. Creating additional operations at this time would result in rejection on the Bitcoin network"))
        }
        // Specific checks for the NodeCore
        if (!nodeCoreService.isAccessible()) {
            return CheckResult.Failure(MineException("Unable to connect to NodeCore at ${config.nodeCoreRpc.host}:${config.nodeCoreRpc.port}, is it reachable?"))
        }
        // Verify the NodeCore configured Network
        if (!nodeCoreService.isOnSameNetwork()) {
            return CheckResult.Failure(MineException("The connected NodeCore (${nodeCoreService.latestNodeCoreStateInfo.networkVersion}) & VPM (${config.bitcoin.network}) are not running on the same configured network"))
        }
        // Verify the balance
        if (!bitcoinService.isSufficientlyFunded()) {
            val maximumTransactionFee = bitcoinService.getMaximumTransactionFee()
            val balance = bitcoinService.getBalance()
            return CheckResult.Failure(
                MineException("""PoP wallet does not contain sufficient funds
                        Current balance: ${balance.formatBTCFriendlyString()}
                        Minimum required: ${maximumTransactionFee.formatBTCFriendlyString()}, need ${maximumTransactionFee.subtract(balance).formatBTCFriendlyString()} more",
                        Send Bitcoin to: ${bitcoinService.currentReceiveAddress()}
                        """.trimIndent()
                )
            )
        }
        // Verify the synchronized status
        if (!nodeCoreService.isSynchronized()) {
            return CheckResult.Failure(MineException("The connected NodeCore is not synchronized: ${nodeCoreService.latestNodeCoreStateInfo.getSynchronizedMessage()}"))
        }
        return CheckResult.Success()
    }

    fun mine(blockNumber: Int?): MineResult {
        // Verify all the mine pre-conditions
        val conditionResult = checkReadyConditions()
        if (conditionResult is CheckResult.Failure) {
            throw conditionResult.error
        }

        val operationId = generateOperationId()
        val result = MineResult(operationId)
        val operation = VpmOperation(operationId, endorsedBlockHeight = blockNumber)
        operations.putIfAbsent(operationId, operation)
        processManager.submit(operation)
        logger.info {
            "Mining operation ${operation.id} started" + if (blockNumber != null) " at block $blockNumber" else ""
        }
        result.addMessage(
            "V201", "Mining operation started", String.format("To view details, run command: getoperation %s", operationId), false
        )
        return result
    }

    fun resubmit(id: String): Result {
        // Verify all the mine pre-conditions
        val conditionResult = checkReadyConditions()
        if (conditionResult is CheckResult.Failure) {
            throw conditionResult.error
        }

        val result = Result()
        val operation = operations[id]
        if (operation == null) {
            result.fail()
            result.addMessage("V404", "Operation not found", String.format("Could not find operation with id '%s'", id), true)
            return result
        }

        // Copy the operation
        val newOperation = VpmOperation(
            endorsedBlockHeight = operation.endorsedBlockHeight,
            reconstituting = true
        )

        // Replicate its state up until prior to the PoP data submission
        operation.miningInstruction?.let {
            newOperation.setMiningInstruction(it)
        }
        operation.endorsementTransaction?.let {
            newOperation.setTransaction(it, operation.endorsementTransactionBytes!!)
            newOperation.setConfirmed()
        }
        operation.blockOfProof?.let {
            newOperation.setBlockOfProof(it)
        }
        operation.merklePath?.let {
            newOperation.setMerklePath(it)
        }
        operation.context?.let {
            newOperation.setContext(it)
        }
        newOperation.reconstituting = false

        processManager.submit(newOperation)
        operations[newOperation.id] = newOperation

        result.addMessage("V200", "Success", String.format("To view details, run command: getoperation %s", newOperation.id), false)
        return result
    }

    fun cancelOperation(id: String) {
        val operation = operations[id]
            ?: throw OperationNotFoundException(String.format("Could not find operation with id '%s'", id))
        processManager.cancel(operation)
    }

    fun getMinerAddress(): String? {
        return if (nodeCoreService.isAccessible()) {
            nodeCoreGateway.getMinerAddress()
        } else {
            null
        }
    }

    fun getBitcoinBalance(): Coin =
        bitcoinService.getBalance()

    fun getBitcoinPendingBalance(): Coin =
        bitcoinService.getPendingBalance()

    fun getLastBitcoinBlock(): StoredBlock =
        bitcoinService.lastBlock

    fun getBitcoinReceiveAddress(): String =
        bitcoinService.currentReceiveAddress()

    fun getWalletSeed(): List<String>? {
        val data = keyValueRepository[Constants.WALLET_SEED_VIEWED_KEY]
        if (data == null || data.value != "1") {
            agreeToWalletSeedRequirement()
            return bitcoinService.getMnemonicSeed()
        }
        return null
    }

    private fun agreeToWalletSeedRequirement() {
        keyValueRepository.insert(
            KeyValueRecord(
                key = Constants.WALLET_SEED_VIEWED_KEY,
                value = "1"
            )
        )
    }

    fun importWallet(seedWords: List<String>, creationDate: Long?): Boolean {
        return bitcoinService.importWallet(StringUtils.join(seedWords, " "), creationDate)
    }

    suspend fun sendBitcoinToAddress(address: String, amount: BigDecimal): Result {
        val result = Result()
        val coinAmount = amount.amountToCoin()
        try {
            val tx = bitcoinService.sendCoins(address, coinAmount)!!
            result.addMessage("V201", "Created", String.format("Transaction: %s", tx.txId.toString()), false)
        } catch (e: UnableToAcquireTransactionLock) {
            result.addMessage(
                "V409",
                "Temporarily Unable to Create Tx",
                "A previous transaction has not yet completed broadcasting to peers and new transactions would result in double spending. Wait a few seconds and try again.",
                true
            )
            result.fail()
        } catch (e: InsufficientMoneyException) {
            result.addMessage("V400", "Insufficient Funds", "Wallet does not contain sufficient funds to create transaction", true)
            result.fail()
        } catch (e: ExceededMaxTransactionFee) {
            result.addMessage(
                "V400",
                "Exceeded Max Fee",
                "Transaction fee was calculated to be more than the configured maximum transaction fee",
                true
            )
            result.fail()
        } catch (e: DuplicateTransactionException) {
            result.addMessage(
                "V409",
                "Duplicate Transaction",
                "Transaction created is a duplicate of a previously broadcast transaction",
                true
            )
            result.fail()
        } catch (e: Exception) {
            result.addMessage("V500", "Send Failed", "Unable to send coins: " + e.message, true)
            result.fail()
        }
        return result
    }

    suspend fun showRecentBitcoinFees(): Pair<Int, Long>? {
        return bitcoinService.calculateFeesFromLatestBlock()
    }

    fun resetBitcoinWallet(): Result {
        val result = Result()
        bitcoinService.resetWallet()
        result.addMessage("V200", "Success", "Wallet has been reset", false)
        return result
    }

    fun exportBitcoinPrivateKeys(): Result {
        val result = Result()
        try {
            val destination = String.format("keys-%d.txt", Instant.now().epochSecond)
            val export = File(destination)
            val created = export.createNewFile()
            if (created) {
                val keys = bitcoinService.exportPrivateKeys()
                PrintWriter(export).use { writer ->
                    for (key in keys) {
                        writer.println(key)
                    }
                }
                result.addMessage(
                    "V201", "Export Successful", String.format("Keys have been exported to %s", export.canonicalPath), false
                )
            } else {
                result.fail()
                result.addMessage("V409", "Export Failed", "The destination file already exists and could not be created", true)
            }
        } catch (e: IOException) {
            logger.error("Unable to export private keys", e)
            result.fail()
            result.addMessage("V500", "Export Failed", e.message!!, true)
        }
        return result
    }

    private suspend fun restoreOperations() {
        val preservedOperations = stateService.getActiveOperations()

        if (preservedOperations.isNotEmpty()) {
            for (miningOperation in preservedOperations) {
                operations[miningOperation.id] = miningOperation
            }
            logger.info { "Found ${preservedOperations.size} operations to restore" }

            val operationsToSubmit = ArrayList(preservedOperations)
            while (operationsToSubmit.isNotEmpty()) {
                val operationsToRemove = ArrayList<VpmOperation>()
                for (miningOperation in operationsToSubmit) {
                    try {
                        if (!miningOperation.isFailed() && nodeCoreService.isReady() &&
                            bitcoinService.isServiceReady() && bitcoinService.isBlockchainDownloaded() && bitcoinService.isSufficientlyFunded()) {
                            processManager.submit(miningOperation)
                            operationsToRemove.add(miningOperation)
                            logger.debug("Successfully restored operation {}", miningOperation.id)
                        }
                    } catch (e: Exception) {
                        logger.warn("Unable to restore previous operation {}", miningOperation.id)
                    }
                }
                operationsToSubmit.removeAll(operationsToRemove)
                delay(5 * 1000)
            }
            logger.info { "All the suspended operations have been submitted" }
        }
    }

    fun setIsShuttingDown(b: Boolean) {
        isShuttingDown = b
    }

    private fun onPoPMiningOperationCompleted(operationId: String) {
        try {
            operations.remove(operationId)
        } catch (e: Exception) {
            logger.error(e.message, e)
        }
    }

    private fun onCoinsReceived(event: CoinsReceivedEventDto) {
        try {
            logger.info { "Received pending tx '${event.tx.txId}', pending balance: '${event.newBalance.formatBTCFriendlyString()}'" }
        } catch (e: Exception) {
            logger.error(e.message, e)
        }
    }

    private fun onNewVeriBlockFound(event: NewVeriBlockFoundEventDto) {
        for (key in HashSet(operations.keys)) {
            val operation = operations[key]
            val operationState = operation?.state
            val blockHeight = operation?.endorsedBlockHeight ?: -1
            if (
                operationState != null && !operation.isFailed() && !(operationState hasType VpmOperationState.CONFIRMED) &&
                blockHeight < event.block.getHeight() - Constants.POP_SETTLEMENT_INTERVAL
            ) {
                operation.fail("Endorsement of block $blockHeight is no longer relevant")
            }
        }
    }
}
