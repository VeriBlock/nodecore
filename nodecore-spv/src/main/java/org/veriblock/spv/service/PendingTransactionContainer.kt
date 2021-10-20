package org.veriblock.spv.service

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.veriblock.sdk.models.Address
import org.veriblock.spv.model.Transaction
import java.util.concurrent.ConcurrentHashMap
import org.veriblock.core.crypto.VbkTxId
import org.veriblock.core.utilities.createLogger

private val logger = createLogger {}

class PendingTransactionContainer {
    // TODO(warchant): use Address as a key, instead of String
    private val pendingTransactionsByAddress: MutableMap<String, MutableList<Transaction>> = ConcurrentHashMap()
    private val confirmedTransactionReplies: MutableMap<VbkTxId, TransactionInfo> = ConcurrentHashMap()
    private val pendingTransactions: MutableMap<VbkTxId, Transaction> = ConcurrentHashMap()
    private val transactionsToMonitor: MutableSet<VbkTxId> = ConcurrentHashMap.newKeySet()

    private val mutex = Mutex()

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

    fun updateTransactionInfo(transactionInfo: TransactionInfo) {
        val transaction = transactionInfo.transaction
        if (pendingTransactions.containsKey(transaction.txId) || transactionsToMonitor.contains(transaction.txId)) {
            confirmedTransactionReplies[transaction.txId] = transactionInfo
            if (transactionInfo.confirmations > 0) {
                pendingTransactions.remove(transaction.txId)
                transactionsToMonitor.remove(transaction.txId)
                pendingTransactionsByAddress[transaction.sourceAddress]?.removeIf { it.txId == transaction.txId }
            }
        }
    }

    suspend fun addTransaction(transaction: Transaction) = mutex.withLock {
        val inputAddress = transaction.inputAddress.toString()

        // Add as pending transaction
        val transactions = pendingTransactionsByAddress.getOrPut(inputAddress) { mutableListOf() }
        transactions.add(transaction)
        pendingTransactions[transaction.txId] = transaction
    }

    fun getTransaction(txId: VbkTxId): Transaction? {
        return pendingTransactions[txId]
    }

    fun getPendingSignatureIndexForAddress(address: Address): Long? {
        val transactions = pendingTransactionsByAddress[address.address]
            ?: return null
        return transactions
            .maxOfOrNull { it.getSignatureIndex() }
    }
}
