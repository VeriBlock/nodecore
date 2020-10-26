package org.veriblock.spv.service

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.veriblock.core.crypto.Sha256Hash
import org.veriblock.spv.model.Transaction
import java.util.ArrayList
import java.util.HashSet
import java.util.concurrent.ConcurrentHashMap

private const val MIN_TX_SEND_PERIOD_MS = 5_000

class PendingTransactionContainer {
    private val pendingAddressTransaction: MutableMap<String, MutableList<Transaction>> = ConcurrentHashMap()
    private val confirmedTxIdTransactionReply: MutableMap<Sha256Hash, TransactionInfo> = ConcurrentHashMap()
    private val pendingTxIdTransaction: MutableMap<Sha256Hash, Transaction> = ConcurrentHashMap()
    private val transactionsForMonitoring: MutableSet<Sha256Hash> = ConcurrentHashMap.newKeySet()

    fun getPendingTransactionIds(): Set<Sha256Hash> {
        val pendingTransactions = pendingTxIdTransaction.entries.asSequence()
            .sortedBy { it.value.getSignatureIndex() }
            .map { it.key }
            .toSet()
        return pendingTransactions + transactionsForMonitoring
    }

    fun getTransactionInfo(txId: Sha256Hash): TransactionInfo? {
        confirmedTxIdTransactionReply[txId]?.let {
            return it
        }
        if (!pendingTxIdTransaction.containsKey(txId)) {
            transactionsForMonitoring.add(txId)
        }
        return null
    }

    fun updateTransactionInfo(transactionInfo: TransactionInfo) {
        val transaction = transactionInfo.transaction
        if (pendingTxIdTransaction.containsKey(transaction.txId) || transactionsForMonitoring.contains(transaction.txId)) {
            confirmedTxIdTransactionReply[transaction.txId] = transactionInfo
            if (transactionInfo.confirmations > 0) {
                pendingTxIdTransaction.remove(transaction.txId)
                transactionsForMonitoring.remove(transaction.txId)
            }
        }
    }

    private val mutex = Mutex()
    private val lastTransactionTimes = ConcurrentHashMap<String, Long>()

    suspend fun addTransaction(transaction: Transaction) = mutex.withLock {
        val inputAddress = transaction.inputAddress.toString()

        // Artifically delay tx broadcast when 2 transactions are sent too close to each other
        val expectedNextTime = (lastTransactionTimes[inputAddress] ?: 0) + MIN_TX_SEND_PERIOD_MS
        val currentTime = System.currentTimeMillis()
        if (currentTime < expectedNextTime) {
            delay(expectedNextTime - currentTime)
            lastTransactionTimes[inputAddress] = expectedNextTime
        } else {
            lastTransactionTimes[inputAddress] = currentTime
        }

        // Add as pending transaction
        val transactions = pendingAddressTransaction[inputAddress]
        if (!transactions.isNullOrEmpty()) {
            transactions.add(transaction)
            pendingTxIdTransaction[transaction.txId] = transaction
        } else {
            val newList = mutableListOf(transaction)
            pendingTxIdTransaction[transaction.txId] = transaction
            pendingAddressTransaction[transaction.inputAddress.toString()] = newList
        }
    }

    fun getTransaction(txId: Sha256Hash): Transaction? {
        return pendingTxIdTransaction[txId]
    }

    fun getPendingSignatureIndexForAddress(address: String): Long? {
        val transactions = pendingAddressTransaction[address]
        return if (!transactions.isNullOrEmpty()) {
            transactions[transactions.size - 1].getSignatureIndex()
        } else {
            null
        }
    }
}
