package org.veriblock.spv.service

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.veriblock.core.crypto.VbkTxId
import org.veriblock.core.utilities.createLogger
import org.veriblock.sdk.models.Address
import org.veriblock.sdk.models.VeriBlockBlock
import org.veriblock.spv.SpvContext
import org.veriblock.spv.util.SpvEventBus
import org.veriblock.spv.util.Threading


private val logger = createLogger {}

class PendingTransactionContainer(
    private val context: SpvContext
) {
    // TODO(warchant): use Address as a key, instead of String
    private val pendingTransactionsByAddress: MutableMap<String, MutableList<Transaction>> = ConcurrentHashMap()
    private val confirmedTransactionReplies: MutableMap<VbkTxId, TransactionInfo> = ConcurrentHashMap()
    private val confirmedTransactions: MutableMap<VbkTxId, Transaction> = ConcurrentHashMap()
    private val pendingTransactions: MutableMap<VbkTxId, Transaction> = ConcurrentHashMap()
    private val transactionsToMonitor: MutableSet<VbkTxId> = ConcurrentHashMap.newKeySet()

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
        return pendingTransactions + transactionsToMonitor
    }

    fun getTransactionInfo(txId: VbkTxId): TransactionInfo? {
        confirmedTransactionReplies[txId]?.let {
            return it
        }
        if (!pendingTransactions.containsKey(txId)) {
            transactionsToMonitor.add(txId)
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
        if (pendingTransactions.containsKey(transaction.txId) || transactionsToMonitor.contains(transaction.txId)) {
            confirmedTransactionReplies[transaction.txId] = transactionInfo
            val pendingTx = pendingTransactions[transaction.txId]
            if (pendingTx != null) {
                confirmedTransactions[transaction.txId] = pendingTx
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
                pendingTransactions.remove(transaction.txId)
                transactionsToMonitor.remove(transaction.txId)
                pendingTransactionsByAddress[transaction.sourceAddress]?.removeIf { it.txId == transaction.txId }
            }
        }

        // Prune confirmed transactions
        if (confirmedTransactionReplies.size > 10_000) {
            val topConfirmedBlockHeight = confirmedTransactionReplies.values.maxOf { it.blockNumber }
            val txToRemove = confirmedTransactionReplies.values.asSequence().filter {
                it.blockNumber < topConfirmedBlockHeight - 1_000
            }.map {
                it.transaction.txId
            }
            for (txId in txToRemove) {
                confirmedTransactionReplies.remove(txId)
                confirmedTransactions.remove(txId)
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

    fun getTransaction(txId: VbkTxId): Transaction? {
        return pendingTransactions[txId]
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
                                transactionsToMonitor.remove(tx.txId)
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
        val reorganizedTransactions = confirmedTransactionReplies.values.filter {
            it.blockNumber == removedBlock.height
        }.mapNotNull {
            confirmedTransactions[it.transaction.txId]
        }
        for (transaction in reorganizedTransactions) {
            confirmedTransactionReplies.remove(transaction.txId)
            confirmedTransactions.remove(transaction.txId)
            addTransaction(transaction)
        }
    }
}
