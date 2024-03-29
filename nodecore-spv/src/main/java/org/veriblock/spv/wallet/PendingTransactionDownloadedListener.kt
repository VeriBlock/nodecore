package org.veriblock.spv.wallet

import org.veriblock.core.crypto.Sha256Hash
import org.veriblock.spv.SpvContext
import org.veriblock.spv.model.StandardTransaction
import org.veriblock.spv.model.TransactionMeta
import java.util.Collections
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class PendingTransactionDownloadedListener(
    private val spvContext: SpvContext
) {
    private val transactions: MutableMap<Sha256Hash, StandardTransaction> = mutableMapOf()
    private val ledger = Ledger()
    private val keyRing = KeyRing()
    private val lock = ReentrantLock(true)

    fun loadTransactions(toLoad: List<StandardTransaction>) {
        for (tx in toLoad) {
            transactions[tx.txId] = tx
            spvContext.transactionPool.insert(tx.transactionMeta!!)
        }
    }

    fun commitTx(tx: StandardTransaction) = lock.withLock {
        if (transactions.containsKey(tx.txId)) {
            return
        }
        tx.transactionMeta!!.setState(TransactionMeta.MetaState.PENDING)
        transactions[tx.txId] = tx
        ledger.record(tx)
    }

    fun getStandardTransaction(txId: Sha256Hash?): StandardTransaction? {
        return transactions[txId]
    }

    fun getStandardTransactions(): Collection<StandardTransaction> =
        Collections.unmodifiableCollection(transactions.values)

    private fun isTransactionRelevant(tx: StandardTransaction): Boolean {
        if (transactions.containsKey(tx.txId)) {
            return true
        }
        return if (keyRing.contains(tx.inputAddress!!.get())) {
            true
        } else {
            tx.getOutputs().any {
                keyRing.contains(it.address.get())
            }
        }
    }

    private fun addTransaction(tx: StandardTransaction) = lock.withLock {
        transactions.putIfAbsent(tx.txId, tx)
        ledger.record(tx)
        //save();
    }
}
