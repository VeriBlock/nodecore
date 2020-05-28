package veriblock.service

import org.veriblock.core.crypto.Sha256Hash
import spark.utils.CollectionUtils
import veriblock.model.Transaction
import java.util.ArrayList
import java.util.HashSet
import java.util.concurrent.ConcurrentHashMap

class PendingTransactionContainer {
    private val pendingAddressTransaction: MutableMap<String, ArrayList<Transaction>> = ConcurrentHashMap()
    private val pendingTxIdTransaction: MutableMap<Sha256Hash, Transaction> = ConcurrentHashMap()
    private val confirmedTxIdTransactionReply: MutableMap<Sha256Hash, TransactionInfo> = ConcurrentHashMap()
    private val transactionsForMonitoring: MutableSet<Sha256Hash> = ConcurrentHashMap.newKeySet()

    fun getPendingTransactionsId(): Set<Sha256Hash> {
        val allPendingTx: MutableSet<Sha256Hash> = HashSet()
        allPendingTx.addAll(pendingTxIdTransaction.keys)
        allPendingTx.addAll(transactionsForMonitoring)
        return allPendingTx
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
        if (pendingTxIdTransaction.containsKey(transaction.txId)) {
            confirmedTxIdTransactionReply[transaction.txId] = transactionInfo
            if (transactionInfo.confirmations > 0) {
                pendingTxIdTransaction.remove(transaction.txId)
                transactionsForMonitoring.remove(transaction.txId)
            }
        }
    }

    fun addTransaction(transaction: Transaction): Boolean {
        val transactions = pendingAddressTransaction[transaction.inputAddress.toString()]
        if (CollectionUtils.isNotEmpty(transactions)) {
            transactions!!.add(transaction)
            pendingTxIdTransaction[transaction.txId] = transaction
        } else {
            val newList = ArrayList<Transaction>()
            newList.add(transaction)
            pendingTxIdTransaction[transaction.txId] = transaction
            pendingAddressTransaction[transaction.inputAddress.toString()] = newList
        }
        return true
    }

    fun getTransaction(txId: Sha256Hash): Transaction? {
        return pendingTxIdTransaction[txId]
    }

    fun getPendingSignatureIndexForAddress(address: String): Long? {
        val transactions = pendingAddressTransaction[address]
        return if (CollectionUtils.isNotEmpty(transactions)) {
            transactions!![transactions.size - 1].getSignatureIndex()
        } else null
    }
}
