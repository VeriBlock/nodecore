// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.miners.pop.service

import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.tuple.Pair
import org.bitcoinj.core.Coin
import org.bitcoinj.core.InsufficientMoneyException
import org.bitcoinj.core.StoredBlock
import org.veriblock.core.utilities.BlockUtility
import org.veriblock.core.utilities.createLogger
import org.veriblock.miners.pop.CoinsReceivedEventDto
import org.veriblock.miners.pop.Constants
import org.veriblock.miners.pop.EventBus
import org.veriblock.miners.pop.NewVeriBlockFoundEventDto
import org.veriblock.miners.pop.VpmConfig
import org.veriblock.miners.pop.common.amountToCoin
import org.veriblock.miners.pop.common.formatBTCFriendlyString
import org.veriblock.miners.pop.common.generateOperationId
import org.veriblock.miners.pop.core.OperationState
import org.veriblock.miners.pop.core.VpmOperation
import org.veriblock.miners.pop.model.ApplicationExceptions.DuplicateTransactionException
import org.veriblock.miners.pop.model.ApplicationExceptions.ExceededMaxTransactionFee
import org.veriblock.miners.pop.model.ApplicationExceptions.UnableToAcquireTransactionLock
import org.veriblock.miners.pop.model.OperationSummary
import org.veriblock.miners.pop.model.PopMinerDependencies
import org.veriblock.miners.pop.model.result.DefaultResultMessage
import org.veriblock.miners.pop.model.result.MineResult
import org.veriblock.miners.pop.model.result.OperationNotFoundException
import org.veriblock.miners.pop.model.result.Result
import org.veriblock.miners.pop.storage.KeyValueRecord
import org.veriblock.miners.pop.storage.KeyValueRepository
import org.veriblock.miners.pop.tasks.ProcessManager
import java.io.File
import java.io.IOException
import java.io.PrintWriter
import java.math.BigDecimal
import java.time.Instant
import java.util.ArrayList
import java.util.EnumSet
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
    private var stateRestored: Boolean = false
    private val readyConditions: EnumSet<PopMinerDependencies> = EnumSet.noneOf(PopMinerDependencies::class.java)

    private fun readyToMine(): Boolean {
        ensureBitcoinServiceReady()
        ensureBlockchainDownloaded()
        ensureSufficientFunds()
        return isReady()
    }

    override fun run() {
        EventBus.popMiningOperationCompletedEvent.register(this, ::onPoPMiningOperationCompleted)
        EventBus.insufficientFundsEvent.register(this, ::onInsufficientFunds)
        EventBus.bitcoinServiceReadyEvent.register(this, ::onBitcoinServiceReady)
        EventBus.bitcoinServiceNotReadyEvent.register(this, ::onBitcoinServiceNotReady)
        EventBus.blockchainDownloadedEvent.register(this, ::onBlockchainDownloaded)
        EventBus.nodeCoreHealthyEvent.register(this, ::onNodeCoreHealthy)
        EventBus.nodeCoreUnhealthyEvent.register(this, ::onNodeCoreUnhealthy)
        EventBus.nodeCoreSynchronizedEvent.register(this, ::onNodeCoreSynchronized)
        EventBus.nodeCoreDesynchronizedEvent.register(this, ::onNodeCoreDesynchronized)
        EventBus.newVeriBlockFoundEvent.register(this, ::onNewVeriBlockFound)
        bitcoinService.initialize()
        if (!config.skipAck) {
            val data = keyValueRepository[Constants.WALLET_SEED_VIEWED_KEY]
            if (data == null || data.value != "1") {
                EventBus.walletSeedAgreementMissingEvent.trigger()
            }
        }
    }

    fun shutdown() {
        EventBus.popMiningOperationCompletedEvent.unregister(this)
        EventBus.insufficientFundsEvent.unregister(this)
        EventBus.bitcoinServiceReadyEvent.unregister(this)
        EventBus.bitcoinServiceNotReadyEvent.unregister(this)
        EventBus.blockchainDownloadedEvent.unregister(this)
        EventBus.nodeCoreHealthyEvent.unregister(this)
        EventBus.nodeCoreUnhealthyEvent.unregister(this)
        EventBus.nodeCoreSynchronizedEvent.unregister(this)
        EventBus.nodeCoreDesynchronizedEvent.unregister(this)
        EventBus.newVeriBlockFoundEvent.unregister(this)

        processManager.shutdown()
        bitcoinService.shutdown()
        nodeCoreService.shutdown()
    }

    fun isReady(): Boolean =
        PopMinerDependencies.SATISFIED == readyConditions

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

    fun mine(blockNumber: Int?): MineResult {
        val operationId = generateOperationId()
        val result = MineResult(operationId)
        if (!readyToMine()) {
            result.fail()
            val reasons = listPendingReadyConditions()
            result.addMessage(DefaultResultMessage("V412", "Miner is not ready", reasons, true))
            return result
        }
        if (isShuttingDown) {
            result.addMessage(DefaultResultMessage("V412", "Miner is not ready", "The miner is currently shutting down", true))
            return result
        }

        // TODO: This is pretty naive. Wallet right now uses DefaultCoinSelector which doesn't do a great job with
        // multiple UTXO and long mempool chains. If that was improved, this count algorithm wouldn't be necessary.
        val count = operations.values.count { OperationState.ENDORSEMENT_TRANSACTION.hasType(it.state) }
        if (count >= Constants.MEMPOOL_CHAIN_LIMIT) {
            result.fail()
            result.addMessage(
                DefaultResultMessage(
                    "V412",
                    "Too Many Pending Transaction operations",
                    "Creating additional operations at this time would result in rejection on the Bitcoin network",
                    true
                )
            )
            return result
        }
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
        val result = Result()
        if (!readyToMine()) {
            result.fail()
            val reasons = listPendingReadyConditions()
            result.addMessage("V412", "Miner is not ready", reasons.joinToString(";"), true)
            return result
        }

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
        newOperation.setMiningInstruction(operation.miningInstruction!!)
        newOperation.setTransaction(operation.endorsementTransaction!!)
        newOperation.setConfirmed()
        newOperation.setBlockOfProof(operation.blockOfProof!!)
        newOperation.setMerklePath(operation.merklePath!!)
        newOperation.setContext(operation.context!!)
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
        return if (readyConditions.contains(PopMinerDependencies.NODECORE_CONNECTED)) {
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

    fun importWallet(seedWords: List<String?>?, creationDate: Long?): Boolean {
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
            result.addMessage("V500", "Send Failed", "Unable to send coins, view logs for details", true)
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

    private fun ensureBlockchainDownloaded() {
        if (!readyConditions.contains(PopMinerDependencies.BLOCKCHAIN_DOWNLOADED) && bitcoinService.blockchainDownloaded()) {
            addReadyCondition(PopMinerDependencies.BLOCKCHAIN_DOWNLOADED)
        }
    }

    private fun ensureBitcoinServiceReady() {
        if (!readyConditions.contains(PopMinerDependencies.BITCOIN_SERVICE_READY) && bitcoinService.serviceReady()) {
            addReadyCondition(PopMinerDependencies.BITCOIN_SERVICE_READY)
        }
    }

    private fun ensureSufficientFunds() {
        val maximumTransactionFee = bitcoinService.getMaximumTransactionFee()
        if (!bitcoinService.getBalance().isLessThan(maximumTransactionFee)) {
            if (!readyConditions.contains(PopMinerDependencies.SUFFICIENT_FUNDS)) {
                logger.info("PoP wallet is sufficiently funded")
                EventBus.fundsAddedEvent.trigger()
            }
            addReadyCondition(PopMinerDependencies.SUFFICIENT_FUNDS)
        } else {
            removeReadyCondition(PopMinerDependencies.SUFFICIENT_FUNDS)
        }
    }

    private fun addReadyCondition(flag: PopMinerDependencies) {
        val previousReady = isReady()
        readyConditions.add(flag)
        if (!previousReady && isReady()) {
            if (!stateRestored) {
                restoreOperations()
            }
            logger.info("PoP Miner: READY")
        }
    }

    private fun removeReadyCondition(flag: PopMinerDependencies) {
        val removed = readyConditions.remove(flag)
        if (removed) {
            logger.warn("PoP Miner: NOT READY ({})", getMessageForDependencyCondition(flag))
            EventBus.popMinerNotReadyEvent.trigger(flag)
        }
    }

    private fun listPendingReadyConditions(): List<String> {
        val reasons: MutableList<String> = ArrayList()
        val pending = EnumSet.complementOf(readyConditions)
        for (flag in pending) {
            reasons.add(getMessageForDependencyCondition(flag))
        }
        return reasons
    }

    private fun getMessageForDependencyCondition(flag: PopMinerDependencies): String {
        return when (flag) {
            PopMinerDependencies.BLOCKCHAIN_DOWNLOADED -> "Bitcoin blockchain is not downloaded"
            PopMinerDependencies.SUFFICIENT_FUNDS -> {
                val maximumTransactionFee = bitcoinService.getMaximumTransactionFee()
                val balance = bitcoinService.getBalance()
                "PoP wallet does not contain sufficient funds" + System.lineSeparator() + "  Current balance: " +
                    balance.formatBTCFriendlyString() + System.lineSeparator() + String.format(
                    "  Minimum required: %1\$s, need %2\$s more",
                    maximumTransactionFee.formatBTCFriendlyString(),
                    maximumTransactionFee.subtract(balance).formatBTCFriendlyString()
                ) + System.lineSeparator() + "  Send Bitcoin to: " +
                    bitcoinService.currentReceiveAddress()
            }
            PopMinerDependencies.NODECORE_CONNECTED -> "Waiting for connection to NodeCore"
            PopMinerDependencies.SYNCHRONIZED_NODECORE -> "Waiting for NodeCore to synchronize"
            PopMinerDependencies.BITCOIN_SERVICE_READY -> "Bitcoin service is not ready"
        }
    }

    private fun restoreOperations() {
        val preservedOperations = stateService.getActiveOperations()
        logger.info { "Found ${preservedOperations.size} operations to restore" }
        for (miningOperation in preservedOperations) {
            try {
                if (!miningOperation.isFailed()) {
                    processManager.submit(miningOperation)
                }
                operations[miningOperation.id] = miningOperation
                logger.debug("Successfully restored operation {}", miningOperation.id)
            } catch (e: Exception) {
                logger.warn("Unable to restore previous operation {}", miningOperation.id)
            }
        }
        stateRestored = true
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

    private fun onInsufficientFunds() {
        try {
            removeReadyCondition(PopMinerDependencies.SUFFICIENT_FUNDS)
        } catch (e: Exception) {
            logger.error(e.message, e)
        }
    }

    private fun onBitcoinServiceReady() {
        try {
            addReadyCondition(PopMinerDependencies.BITCOIN_SERVICE_READY)
        } catch (e: Exception) {
            logger.error(e.message, e)
        }
    }

    private fun onBitcoinServiceNotReady() {
        try {
            removeReadyCondition(PopMinerDependencies.BITCOIN_SERVICE_READY)
        } catch (e: Exception) {
            logger.error(e.message, e)
        }
    }

    private fun onBlockchainDownloaded() {
        try {
            addReadyCondition(PopMinerDependencies.BLOCKCHAIN_DOWNLOADED)
            ensureSufficientFunds()
            logger.info(
                "Available Bitcoin balance: " + bitcoinService.getBalance().formatBTCFriendlyString()
            )
            logger.info("Send Bitcoin to: " + bitcoinService.currentReceiveAddress())
        } catch (e: Exception) {
            logger.error(e.message, e)
        }
    }

    fun onNodeCoreHealthy() {
        try {
            addReadyCondition(PopMinerDependencies.NODECORE_CONNECTED)
        } catch (e: Exception) {
            logger.error(e.message, e)
        }
        return
    }

    private fun onNodeCoreUnhealthy() {
        try {
            removeReadyCondition(PopMinerDependencies.NODECORE_CONNECTED)
        } catch (e: Exception) {
            logger.error(e.message, e)
        }
    }

    private fun onNodeCoreSynchronized() {
        try {
            addReadyCondition(PopMinerDependencies.SYNCHRONIZED_NODECORE)
        } catch (e: Exception) {
            logger.error(e.message, e)
        }
    }

    private fun onNodeCoreDesynchronized() {
        try {
            removeReadyCondition(PopMinerDependencies.SYNCHRONIZED_NODECORE)
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
                operationState != null && !operation.isFailed() && !(operationState hasType OperationState.CONFIRMED) &&
                blockHeight < event.block.getHeight() - Constants.POP_SETTLEMENT_INTERVAL
            ) {
                operation.fail("Endorsement of block $blockHeight is no longer relevant")
            }
        }
    }
}
