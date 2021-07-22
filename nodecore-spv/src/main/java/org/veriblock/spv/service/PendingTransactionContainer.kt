package org.veriblock.spv.service

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.veriblock.core.crypto.Sha256Hash
import org.veriblock.sdk.models.Address
import org.veriblock.spv.model.Transaction
import java.util.concurrent.ConcurrentHashMap

class PendingTransactionContainer {
    // TODO(warchant): use Address as a key, instead of String
    private val pendingTransactionsByAddress: MutableMap<String, MutableList<Transaction>> = ConcurrentHashMap()
    private val confirmedTransactionReplies: MutableMap<Sha256Hash, TransactionInfo> = ConcurrentHashMap()
    private val pendingTransactions: MutableMap<Sha256Hash, Transaction> = ConcurrentHashMap()
    private val transactionsToMonitor: MutableSet<Sha256Hash> = ConcurrentHashMap.newKeySet()

    fun getPendingTransactionIds(): Set<Sha256Hash> {
        val pendingTransactions = pendingTransactions.entries.asSequence()
            .sortedBy { it.value.getSignatureIndex() }
            .map { it.key }
            .toSet()
        return pendingTransactions + transactionsToMonitor
    }

    fun getTransactionInfo(txId: Sha256Hash): TransactionInfo? {
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
            }
        }
    }

    private val mutex = Mutex()

    suspend fun addTransaction(transaction: Transaction) = mutex.withLock {
        val inputAddress = transaction.inputAddress.toString()

        // Add as pending transaction
        val transactions = pendingTransactionsByAddress.getOrPut(inputAddress) { mutableListOf() }
        transactions.add(transaction)
        pendingTransactions[transaction.txId] = transaction
    }

    fun getTransaction(txId: Sha256Hash): Transaction? {
        return pendingTransactions[txId]
    }

    fun getPendingSignatureIndexForAddress(address: Address): Long? {
        val transactions = pendingTransactionsByAddress[address.address]
            ?: return null
        return transactions
            .maxOfOrNull { it.getSignatureIndex() }
    }
}
