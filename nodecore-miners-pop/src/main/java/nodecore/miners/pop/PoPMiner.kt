// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.miners.pop

import nodecore.miners.pop.common.Utility
import nodecore.miners.pop.core.MiningOperation
import nodecore.miners.pop.core.OperationState
import nodecore.miners.pop.core.OperationStateType
import nodecore.miners.pop.events.CoinsReceivedEventDto
import nodecore.miners.pop.events.EventBus
import nodecore.miners.pop.events.NewVeriBlockFoundEventDto
import nodecore.miners.pop.model.ApplicationExceptions.DuplicateTransactionException
import nodecore.miners.pop.model.ApplicationExceptions.ExceededMaxTransactionFee
import nodecore.miners.pop.model.ApplicationExceptions.SendTransactionException
import nodecore.miners.pop.model.ApplicationExceptions.UnableToAcquireTransactionLock
import nodecore.miners.pop.model.OperationSummary
import nodecore.miners.pop.model.PoPMinerDependencies
import nodecore.miners.pop.model.dto.PopMiningOperationStateDto
import nodecore.miners.pop.model.result.DefaultResultMessage
import nodecore.miners.pop.model.result.MineResult
import nodecore.miners.pop.model.result.Result
import nodecore.miners.pop.services.BitcoinService
import nodecore.miners.pop.services.NodeCoreService
import nodecore.miners.pop.services.PoPStateService
import nodecore.miners.pop.storage.KeyValueData
import nodecore.miners.pop.storage.KeyValueRepository
import nodecore.miners.pop.tasks.ProcessManager
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.tuple.Pair
import org.bitcoinj.core.Coin
import org.bitcoinj.core.InsufficientMoneyException
import org.bitcoinj.core.StoredBlock
import org.veriblock.core.utilities.BlockUtility
import org.veriblock.core.utilities.createLogger
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

class PoPMiner(
    private val configuration: Configuration,
    private val bitcoinService: BitcoinService,
    private val nodeCoreService: NodeCoreService,
    private val stateService: PoPStateService,
    private val keyValueRepository: KeyValueRepository,
    private val processManager: ProcessManager
) : Runnable {
    private val operations = ConcurrentHashMap<String, MiningOperation>()
    private var isShuttingDown = false
    private var stateRestored: Boolean = false
    private val readyConditions: EnumSet<PoPMinerDependencies> = EnumSet.noneOf(PoPMinerDependencies::class.java)

    private fun readyToMine(): Boolean {
        ensureBitcoinServiceReady()
        ensureBlockchainDownloaded()
        ensureSufficientFunds()
        return isReady()
    }

    override fun run() {
        EventBus.popMiningOperationCompletedEvent.register(this, ::onPoPMiningOperationCompleted)
        EventBus.coinsReceivedEvent.register(this, ::onCoinsReceived)
        EventBus.insufficientFundsEvent.register(this, ::onInsufficientFunds)
        EventBus.bitcoinServiceReadyEvent.register(this, ::onBitcoinServiceReady)
        EventBus.bitcoinServiceNotReadyEvent.register(this, ::onBitcoinServiceNotReady)
        EventBus.blockchainDownloadedEvent.register(this, ::onBlockchainDownloaded)
        EventBus.nodeCoreHealthyEvent.register(this, ::onNodeCoreHealthy)
        EventBus.nodeCoreUnhealthyEvent.register(this, ::onNodeCoreUnhealthy)
        EventBus.nodeCoreSynchronizedEvent.register(this, ::onNodeCoreSynchronized)
        EventBus.nodeCoreDesynchronizedEvent.register(this, ::onNodeCoreDesynchronized)
        EventBus.configurationChangedEvent.register(this, ::onConfigurationChanged)
        EventBus.newVeriBlockFoundEvent.register(this, ::onNewVeriBlockFound)
        bitcoinService.initialize()
        if (!configuration.getBoolean(Constants.BYPASS_ACKNOWLEDGEMENT_KEY)) {
            val data = keyValueRepository[Constants.WALLET_SEED_VIEWED_KEY]
            if (data == null || data.value != "1") {
                EventBus.walletSeedAgreementMissingEvent.trigger()
            }
        }
    }

    @Throws(InterruptedException::class)
    fun shutdown() {
        EventBus.popMiningOperationCompletedEvent.unregister(this)
        EventBus.coinsReceivedEvent.unregister(this)
        EventBus.insufficientFundsEvent.remove(this)
        EventBus.bitcoinServiceReadyEvent.remove(this)
        EventBus.bitcoinServiceNotReadyEvent.remove(this)
        EventBus.blockchainDownloadedEvent.remove(this)
        EventBus.nodeCoreHealthyEvent.remove(this)
        EventBus.nodeCoreUnhealthyEvent.remove(this)
        EventBus.nodeCoreSynchronizedEvent.remove(this)
        EventBus.nodeCoreDesynchronizedEvent.remove(this)
        EventBus.configurationChangedEvent.remove(this)
        EventBus.newVeriBlockFoundEvent.unregister(this)

        processManager.shutdown()
        bitcoinService.shutdown()
        nodeCoreService.shutdown()
    }

    fun isReady(): Boolean =
        PoPMinerDependencies.SATISFIED == readyConditions

    fun listOperations(): List<OperationSummary> {
        return operations.values.asSequence().map { operation ->
            val state = operation.state
            val miningInstruction = (state as? OperationState.Instruction)?.miningInstruction
            var blockNumber = -1
            if (miningInstruction != null) {
                blockNumber = BlockUtility.extractBlockHeightFromBlockHeader(miningInstruction.endorsedBlockHeader)
            }
            val status = operation.status.toString()
            OperationSummary(operation.id, blockNumber, status, state.type.name, state.toString())
        }.sortedBy {
            it.endorsedBlockNumber
        }.toList()
    }

    fun getOperationState(id: String): PopMiningOperationStateDto? {
        val operation = stateService.getOperation(id)
            ?: return null

        val state = operation.state

        // TODO: Implement
        val result = PopMiningOperationStateDto()
        result.operationId = operation.id
        result.status = operation.status
        result.currentAction = operation.state.type
        result.miningInstruction = (state as? OperationState.Instruction)?.miningInstruction
        result.transaction = (state as? OperationState.EndorsementTransaction)?.endorsementTransactionBytes
        result.submittedTransactionId = (state as? OperationState.EndorsementTransaction)?.endorsementTransaction?.txId?.toString()
        result.bitcoinBlockHeaderOfProof = (state as? OperationState.BlockOfProof)?.blockOfProof?.bitcoinSerialize()
        result.merklePath = (state as? OperationState.Proven)?.merklePath
        result.bitcoinContextBlocks = (state as? OperationState.Context)?.bitcoinContextBlocks?.map { it.bitcoinSerialize() }
        result.popTransactionId = (state as? OperationState.SubmittedPopData)?.proofOfProofId
        result.detail = state.toString()
        return result
    }

    fun mine(blockNumber: Int?): MineResult {
        val operationId = Utility.generateOperationId()
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
        // multiple UTXO and long mempool chains. If that was improved, this count algorithm wouldn't be sufficient.
        val count = operations.values.count { it.state.type == OperationStateType.ENDORSEMEMT_TRANSACTION }
        if (count >= Constants.MEMPOOL_CHAIN_LIMIT) {
            result.fail()
            result.addMessage(
                DefaultResultMessage(
                    "V412",
                    "Too Many Pending Transactions",
                    "Creating additional transactions at this time would result in rejection on the Bitcoin network",
                    true
                )
            )
            return result
        }
        val operation = MiningOperation(operationId, blockHeight = blockNumber)
        operations.putIfAbsent(operationId, operation)
        operation.begin()
        processManager.submit(operation)
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
            result.addMessage("V412", "Miner is not ready", java.lang.String.join("; ", reasons), true)
            return result
        }
        val operation = operations[id]
        if (operation == null) {
            result.fail()
            result.addMessage("V404", "Operation not found", String.format("Could not find operation with id '%s'", id), true)
            return result
        }
        processManager.submit(operation)
        result.addMessage("V200", "Success", String.format("To view details, run command: getoperation %s", operation.id), false)
        return result
    }

    fun getMinerAddress(): String? {
        return if (readyConditions.contains(PoPMinerDependencies.NODECORE_CONNECTED)) {
            nodeCoreService.getMinerAddress()
        } else {
            null
        }
    }

    fun getBitcoinBalance(): Coin =
        bitcoinService.getBalance()

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
        val data = KeyValueData()
        data.key = Constants.WALLET_SEED_VIEWED_KEY
        data.value = "1"
        keyValueRepository.insert(data)
    }

    fun importWallet(seedWords: List<String?>?, creationDate: Long?): Boolean {
        return bitcoinService.importWallet(StringUtils.join(seedWords, " "), creationDate)
    }

    suspend fun sendBitcoinToAddress(address: String, amount: BigDecimal): Result {
        val result = Result()
        val coinAmount = Utility.amountToCoin(amount)
        try {
            val tx = bitcoinService.sendCoins(address, coinAmount)!!
            result.addMessage("V201", "Created", String.format("Transaction: %s", tx.txId.toString()), false)
        } catch (e: SendTransactionException) {
            for (t in e.suppressed) {
                when (t) {
                    is UnableToAcquireTransactionLock -> {
                        result.addMessage(
                            "V409",
                            "Temporarily Unable to Create Tx",
                            "A previous transaction has not yet completed broadcasting to peers and new transactions would result in double spending. Wait a few seconds and try again.",
                            true
                        )
                    }
                    is InsufficientMoneyException -> {
                        result.addMessage("V400", "Insufficient Funds", "Wallet does not contain sufficient funds to create transaction", true)
                    }
                    is ExceededMaxTransactionFee -> {
                        result.addMessage(
                            "V400",
                            "Exceeded Max Fee",
                            "Transaction fee was calculated to be more than the configured maximum transaction fee",
                            true
                        )
                    }
                    is DuplicateTransactionException -> {
                        result.addMessage(
                            "V409",
                            "Duplicate Transaction",
                            "Transaction created is a duplicate of a previously broadcast transaction",
                            true
                        )
                    }
                    else -> {
                        result.addMessage("V500", "Send Failed", "Unable to send coins, view logs for details", true)
                    }
                }
            }
            result.fail()
        }
        return result
    }

    fun showRecentBitcoinFees(): Pair<Int, Long>? {
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
        if (!readyConditions.contains(PoPMinerDependencies.BLOCKCHAIN_DOWNLOADED) && bitcoinService.blockchainDownloaded()) {
            addReadyCondition(PoPMinerDependencies.BLOCKCHAIN_DOWNLOADED)
        }
    }

    private fun ensureBitcoinServiceReady() {
        if (!readyConditions.contains(PoPMinerDependencies.BITCOIN_SERVICE_READY) && bitcoinService.serviceReady()) {
            addReadyCondition(PoPMinerDependencies.BITCOIN_SERVICE_READY)
        }
    }

    private fun ensureSufficientFunds() {
        val maximumTransactionFee = Coin.valueOf(configuration.maxTransactionFee)
        if (bitcoinService.getBalance().isGreaterThan(maximumTransactionFee)) {
            if (!readyConditions.contains(PoPMinerDependencies.SUFFICIENT_FUNDS)) {
                logger.info("PoP wallet is sufficiently funded")
                EventBus.fundsAddedEvent.trigger()
            }
            addReadyCondition(PoPMinerDependencies.SUFFICIENT_FUNDS)
        } else {
            removeReadyCondition(PoPMinerDependencies.SUFFICIENT_FUNDS)
        }
    }

    private fun addReadyCondition(flag: PoPMinerDependencies) {
        val previousReady = isReady()
        readyConditions.add(flag)
        if (!previousReady && isReady()) {
            if (!stateRestored) {
                restoreOperations()
            }
            logger.info("PoP Miner: READY")
        }
    }

    private fun removeReadyCondition(flag: PoPMinerDependencies) {
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

    private fun getMessageForDependencyCondition(flag: PoPMinerDependencies): String {
        return when (flag) {
            PoPMinerDependencies.BLOCKCHAIN_DOWNLOADED -> "Bitcoin blockchain is not downloaded"
            PoPMinerDependencies.SUFFICIENT_FUNDS -> {
                val maximumTransactionFee = Coin.valueOf(configuration.maxTransactionFee)
                val balance = bitcoinService.getBalance()
                "PoP wallet does not contain sufficient funds" + System.lineSeparator() + "  Current balance: " +
                    Utility.formatBTCFriendlyString(balance) + System.lineSeparator() + String.format(
                    "  Minimum required: %1\$s, need %2\$s more",
                    Utility.formatBTCFriendlyString(maximumTransactionFee),
                    Utility.formatBTCFriendlyString(maximumTransactionFee.subtract(balance))
                ) + System.lineSeparator() + "  Send Bitcoin to: " +
                    bitcoinService.currentReceiveAddress()
            }
            PoPMinerDependencies.NODECORE_CONNECTED -> "Waiting for connection to NodeCore"
            PoPMinerDependencies.SYNCHRONIZED_NODECORE -> "Waiting for NodeCore to synchronize"
            PoPMinerDependencies.BITCOIN_SERVICE_READY -> "Bitcoin service is not ready"
        }
    }

    private fun restoreOperations() {
        val preservedOperations = stateService.getActiveOperations()
        logger.info("Found {} operations to restore", preservedOperations.size)
        for (miningOperation in preservedOperations) {
            try {
                operations[miningOperation.id] = miningOperation
                logger.info("Successfully restored operation {}", miningOperation.id)
            } catch (e: Exception) {
                logger.error("Unable to restore previous operation {}", miningOperation.id)
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
            logger.info(
                "Received pending tx '{}', pending balance: '{}'", event.tx.txId.toString(),
                Utility.formatBTCFriendlyString(event.newBalance)
            )
        } catch (e: Exception) {
            logger.error(e.message, e)
        }
    }

    private fun onInsufficientFunds() {
        try {
            removeReadyCondition(PoPMinerDependencies.SUFFICIENT_FUNDS)
        } catch (e: Exception) {
            logger.error(e.message, e)
        }
    }

    private fun onBitcoinServiceReady() {
        try {
            addReadyCondition(PoPMinerDependencies.BITCOIN_SERVICE_READY)
            if (!readyToMine()) {
                val failed = EnumSet.complementOf(readyConditions)
                for (flag in failed) {
                    logger.warn("PoP Miner: NOT READY ({})", getMessageForDependencyCondition(flag))
                    EventBus.popMinerNotReadyEvent.trigger(flag)
                }
            }
        } catch (e: Exception) {
            logger.error(e.message, e)
        }
    }

    private fun onBitcoinServiceNotReady() {
        try {
            removeReadyCondition(PoPMinerDependencies.BITCOIN_SERVICE_READY)
        } catch (e: Exception) {
            logger.error(e.message, e)
        }
    }

    private fun onBlockchainDownloaded() {
        try {
            addReadyCondition(PoPMinerDependencies.BLOCKCHAIN_DOWNLOADED)
            ensureSufficientFunds()
            logger.info(
                "Available Bitcoin balance: " + Utility.formatBTCFriendlyString(bitcoinService.getBalance())
            )
            logger.info("Send Bitcoin to: " + bitcoinService.currentReceiveAddress())
        } catch (e: Exception) {
            logger.error(e.message, e)
        }
    }

    fun onNodeCoreHealthy() {
        try {
            addReadyCondition(PoPMinerDependencies.NODECORE_CONNECTED)
        } catch (e: Exception) {
            logger.error(e.message, e)
        }
        return
    }

    private fun onNodeCoreUnhealthy() {
        try {
            removeReadyCondition(PoPMinerDependencies.NODECORE_CONNECTED)
        } catch (e: Exception) {
            logger.error(e.message, e)
        }
    }

    private fun onNodeCoreSynchronized() {
        try {
            addReadyCondition(PoPMinerDependencies.SYNCHRONIZED_NODECORE)
        } catch (e: Exception) {
            logger.error(e.message, e)
        }
    }

    private fun onNodeCoreDesynchronized() {
        try {
            removeReadyCondition(PoPMinerDependencies.SYNCHRONIZED_NODECORE)
        } catch (e: Exception) {
            logger.error(e.message, e)
        }
    }

    private fun onConfigurationChanged() {
        try {
            ensureSufficientFunds()
        } catch (e: Exception) {
            logger.error(e.message, e)
        }
    }

    private fun onNewVeriBlockFound(event: NewVeriBlockFoundEventDto) {
        for (key in HashSet(operations.keys)) {
            val operation = operations[key]
            val operationState = operation?.state
            val blockHeight = operation?.blockHeight ?: -1
            if (
                operationState != null && operationState !is OperationState.Confirmed &&
                blockHeight < event.block.getHeight() - Constants.POP_SETTLEMENT_INTERVAL
            ) {
                operation.fail("Endorsement of block $blockHeight is no longer relevant")
            }
        }
    }
}
