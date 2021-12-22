package org.veriblock.spv.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nodecore.api.grpc.RpcOutput
import nodecore.api.grpc.RpcTransaction
import nodecore.api.grpc.RpcTransactionUnion
import nodecore.api.grpc.utilities.ByteStringUtility
import nodecore.api.grpc.utilities.extensions.toHex
import nodecore.api.grpc.utilities.extensions.toProperAddressType
import org.veriblock.core.crypto.VbkTxId
import org.veriblock.core.crypto.asVbkTxId
import org.veriblock.core.utilities.createLogger
import org.veriblock.sdk.models.Address
import org.veriblock.sdk.models.VeriBlockBlock
import org.veriblock.spv.SpvContext
import org.veriblock.spv.model.FullBlock
import org.veriblock.spv.model.Transaction
import org.veriblock.spv.serialization.MessageSerializer
import org.veriblock.spv.util.SpvEventBus
import org.veriblock.spv.util.Threading
import java.lang.IllegalArgumentException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock


private val logger = createLogger {}

class PendingTransactionContainer(
    private val context: SpvContext
) {
    // TODO(warchant): use Address as a key, instead of String
    private val pendingTransactionsByAddress: MutableMap<String, MutableList<Transaction>> = ConcurrentHashMap()
    private val pendingTransactions: MutableMap<VbkTxId, Transaction> = ConcurrentHashMap()
    // FIXME: add converting between TransactionData <-> Transaction and delete this
    private val pendingTransactionsInfo: MutableMap<VbkTxId, TransactionInfo> = ConcurrentHashMap()

    private val addedToBlockchainTransactions: MutableMap<VbkTxId, Transaction> = ConcurrentHashMap()
    // FIXME: add converting between TransactionData <-> Transaction and delete this
    private val addedToBlockchainTransactionsInfo: MutableMap<VbkTxId, TransactionInfo> = ConcurrentHashMap()

    private val lock = ReentrantLock()

    private var lastConfirmedSignatureIndex = -1L
    private var maxConfirmedSigIndex = -1L

    init {
        SpvEventBus.removedBestBlockEvent.register(this, ::handleRemovedBestBlock)
    }

    fun getPendingTransactionIds(): Set<VbkTxId> {
        val pendingTransactions = pendingTransactions.entries.asSequence()
            .sortedBy { it.value.getSignatureIndex() }
            .map { it.key }
            .toSet()
        return pendingTransactions
    }

    fun getTransaction(txId: VbkTxId): Transaction? {
        pendingTransactions[txId]?.let {
            return it
        }
        addedToBlockchainTransactions[txId]?.let {
            return it
        }
        return null
    }

    fun getTransactionInfo(txId: VbkTxId): TransactionInfo? {
        pendingTransactionsInfo[txId]?.let {
            return it
        }
        addedToBlockchainTransactionsInfo[txId]?.let {
            return it
        }
        return null
    }

    fun getMaxConfirmedSigIndex(): Long {
        return maxConfirmedSigIndex
    }

    fun getSize(): Int {
        return pendingTransactions.size
    }

    fun updateTransactionInfo(transactionInfo: TransactionInfo) = lock.withLock {
        val transaction = transactionInfo.transaction
        if (pendingTransactions.containsKey(transaction.txId)) {
            pendingTransactionsInfo[transaction.txId] = transactionInfo
            val pendingTx = pendingTransactions[transaction.txId]
            if (pendingTx != null) {
                if (pendingTx.getSignatureIndex() > lastConfirmedSignatureIndex) {
                    lastConfirmedSignatureIndex = pendingTx.getSignatureIndex()
                }
            }
            if (transactionInfo.confirmations > 0) {
                logger.info { "Transaction ${transaction.txId} has been confirmed. (${pendingTransactions.size} unconfirmed transactions left)" }
                val currentSignatureIndex : Long? = pendingTransactions[transaction.txId]?.getSignatureIndex()
                if (currentSignatureIndex != null) {
                    if (currentSignatureIndex > maxConfirmedSigIndex) {
                        maxConfirmedSigIndex = currentSignatureIndex
                    }
                }
                addedToBlockchainTransactions[transaction.txId] = pendingTx!!
                addedToBlockchainTransactionsInfo[transaction.txId] = pendingTransactionsInfo[transaction.txId]!!

                pendingTransactions.remove(transaction.txId)
                pendingTransactionsInfo.remove(transaction.txId)
                pendingTransactionsByAddress[transaction.sourceAddress]?.removeIf { it.txId == transaction.txId }
            }
        }

        // Prune confirmed transactions
        if (addedToBlockchainTransactionsInfo.size > 10_000) {
            val topConfirmedBlockHeight = addedToBlockchainTransactionsInfo.values.maxOf { it.blockNumber }
            val txToRemove = addedToBlockchainTransactionsInfo.values.asSequence().filter {
                it.blockNumber < topConfirmedBlockHeight - 1_000
            }.map {
                it.transaction.txId
            }
            for (txId in txToRemove) {
                addedToBlockchainTransactions.remove(txId)
                addedToBlockchainTransactionsInfo.remove(txId)
            }
        }
    }

    fun addTransaction(transaction: Transaction) = lock.withLock {
        val inputAddress = transaction.inputAddress.toString()

        // Add as pending transaction
        val transactions = pendingTransactionsByAddress.getOrPut(inputAddress) { mutableListOf() }
        transactions.add(transaction)
        pendingTransactions[transaction.txId] = transaction
    }

    fun addNetworkTransaction(message: RpcTransactionUnion): AddTransactionResult? {
        logger.info { "START!!!" }
        var txId: String? = null
        txId = when (message.transactionCase) {
            RpcTransactionUnion.TransactionCase.UNSIGNED -> {
                logger.warn("Rejected network transaction because it was unsigned")
                return AddTransactionResult.INVALID
            }
            RpcTransactionUnion.TransactionCase.SIGNED -> ByteStringUtility.byteStringToHex(message.signed.transaction.txId)
            RpcTransactionUnion.TransactionCase.SIGNED_MULTISIG -> ByteStringUtility.byteStringToHex(message.signedMultisig.transaction.txId)
            RpcTransactionUnion.TransactionCase.TRANSACTION_NOT_SET -> {
                logger.warn("Rejected a network transaction because the type was not set")
                return AddTransactionResult.INVALID
            }
            else -> {
                logger.warn("Rejected transaction with unknown type")
                return  AddTransactionResult.INVALID
            }
        }
        return try {
            logger.error { "deserializeNormalTransaction" }
            val transaction: Transaction = MessageSerializer.deserializeNormalTransaction(message)
            // TODO: DenylistTransactionCache
//            if (DenylistTransactionCache.get().contains(transaction.getTxId())) {
//                return PendingTransactionContainer.AddTransactionResult.INVALID
//            }
            logger.error { "addTransaction" }
            addTransaction(transaction)
            logger.debug("Add transaction to mempool {}", txId)
            AddTransactionResult.SUCCESS
        } catch (ex: Exception) {
            logger.warn("Could not construct transaction for reason: {}", ex.message)
            AddTransactionResult.INVALID
        }
    }

    fun getPendingSignatureIndexForAddress(address: Address, ledgerSignatureIndex: Long?): Long? = lock.withLock {
        val transactions = pendingTransactionsByAddress[address.address]
        if (transactions.isNullOrEmpty()) {
            return ledgerSignatureIndex?.coerceAtLeast(lastConfirmedSignatureIndex)
        }
        // FIXME The code inside this check is a hack. The proper way to do that is by fully supporting a filtered blockchain in SPV.
        if (ledgerSignatureIndex != null) {
            // Check ledger vs pending transactions. The lowest signature index should be at most the ledger's plus one
            val minSignatureIndex = transactions.minOf { it.getSignatureIndex() }
            if (minSignatureIndex > ledgerSignatureIndex + 1) {
                CoroutineScope(Threading.EVENT_EXECUTOR.asCoroutineDispatcher()).launch {
                    // Wait just in case there is a synchronization problem
                    delay(300_000)
                    lock.withLock {
                        // Recompute min sigindex
                        val newTransactions = pendingTransactionsByAddress[address.address]
                        if (newTransactions.isNullOrEmpty()) {
                            return@launch
                        }
                        // Recheck the signature index
                        val newLedgerSignatureIndex = context.getSignatureIndex(address)
                        if (newLedgerSignatureIndex == null || newLedgerSignatureIndex != ledgerSignatureIndex) {
                            // If it changed, that means transactions have been processed during this time so we're not stuck
                            return@launch
                        }
                        val newMinSignatureIndex = newTransactions.minOf {
                            it.getSignatureIndex()
                        }.coerceAtMost(minSignatureIndex)
                        if (newMinSignatureIndex > newLedgerSignatureIndex + 1) {
                            logger.warn { "The SPV mempool for address $address has become out of sync with the network!" }
                            logger.info { "All the transactions for that address will be pruned in order to prevent further transactions from being rejected." }
                            for (tx in newTransactions) {
                                pendingTransactions.remove(tx.txId)
                            }
                            newTransactions.clear()
                        }
                    }
                }
            }
        }
        return transactions.maxOf { it.getSignatureIndex() }
    }

    private fun handleRemovedBestBlock(removedBlock: VeriBlockBlock) = lock.withLock {
        val reorganizedTransactions = addedToBlockchainTransactionsInfo.values.filter {
            it.blockNumber == removedBlock.height
        }.mapNotNull {
            addedToBlockchainTransactions[it.transaction.txId]
        }
        for (transaction in reorganizedTransactions) {
            addedToBlockchainTransactions.remove(transaction.txId)
            addTransaction(transaction)
        }
    }

    fun updateTransactionsByBlock(block: FullBlock) = lock.withLock {
        /*
         * We are removing only transactions that match the exact String from the block. If the block validation
         * fails, NO transactions are removed from the transaction pool.
         */
        val normalTransactions = block.normalTransactions
            ?: throw IllegalArgumentException(
                "removeTransactionsInBlock cannot be called with a block with a " +
                    "null transaction set!"
            )
        for (transaction in normalTransactions) {
            // FIXME: convert StandardTransaction to TransactionData
            val builder = transaction.getSignedMessageBuilder(context.config.networkParameters)
            val rpcTx = builder.build()
            val txData = rpcTx.transaction.toModel()

            val txInfo = TransactionInfo(
                blockNumber = block.height,
                timestamp = block.timestamp,
                transaction = txData,
                confirmations = context.blockchain.getChainHeadIndex().height + 1 - block.height,
                blockHash = block.hash.toString(),
                merklePath = "", // FIXME: is it possible to calculate merklePath in SPV?
                endorsedBlockHash = "", // FIXME: for POP
                bitcoinBlockHash = "", // FIXME: for POP
                bitcoinTxId = "", // FIXME: for POP
                bitcoinConfirmations = 0, // FIXME: for POP
            )
            updateTransactionInfo(txInfo)
        }
    }

    enum class AddTransactionResult {
        SUCCESS,
        INVALID,
        DUPLICATE
    }

    private fun RpcTransaction.toModel() = TransactionData(
        type = TransactionType.valueOf(type.name),
        sourceAddress = sourceAddress.toProperAddressType(),
        sourceAmount = sourceAmount,
        outputs = outputsList.map { it.toModel() },
        transactionFee = transactionFee,
        data = data.toHex(),
        bitcoinTransaction = bitcoinTransaction.toHex(),
        endorsedBlockHeader = endorsedBlockHeader.toHex(),
        bitcoinBlockHeaderOfProof = "",
        merklePath = merklePath,
        contextBitcoinBlockHeaders = listOf(),
        timestamp = timestamp,
        size = size,
        txId = txId.toByteArray().asVbkTxId()
    )

    private fun RpcOutput.toModel() = OutputData(
        address = address.toHex(),
        amount = amount
    )
}
