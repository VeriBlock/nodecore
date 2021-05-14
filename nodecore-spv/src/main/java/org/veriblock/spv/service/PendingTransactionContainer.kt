package org.veriblock.spv.service

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.veriblock.core.crypto.Sha256Hash
import org.veriblock.sdk.models.Address
import org.veriblock.spv.model.Transaction
import java.util.concurrent.ConcurrentHashMap

class PendingTransactionContainer {
    // TODO(warchant): use Address as a key, instead of String
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

    suspend fun addTransaction(transaction: Transaction) = mutex.withLock {
        val inputAddress = transaction.inputAddress.toString()

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

    fun getPendingSignatureIndexForAddress(address: Address): Long? {
        val transactions = pendingAddressTransaction[address.address]
            ?: return null
        return transactions
            .map { it.getSignatureIndex() }
            .maxOrNull()
    }
}
