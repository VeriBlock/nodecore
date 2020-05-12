package veriblock.wallet

import org.veriblock.core.crypto.Sha256Hash
import veriblock.SpvContext
import veriblock.listeners.PendingTransactionDownloadedListener
import veriblock.model.Output
import veriblock.model.StandardTransaction
import veriblock.model.TransactionMeta
import java.util.Collections
import java.util.concurrent.locks.ReentrantLock

class PendingTransactionDownloadedListenerImpl(
    private val spvContext: SpvContext
) : PendingTransactionDownloadedListener {
    private val transactions: MutableMap<Sha256Hash?, StandardTransaction>? = null
    private val ledger = Ledger()
    private val keyRing = KeyRing()
    private val lock = ReentrantLock(true)
    override fun onPendingTransactionDownloaded(transaction: StandardTransaction?) {}

    fun loadTransactions(toLoad: List<StandardTransaction>) {
        for (tx in toLoad) {
            transactions!![tx.txId] = tx
            spvContext.transactionPool.insert(tx.transactionMeta!!)
        }
    }

    fun commitTx(tx: StandardTransaction) {
        lock.lock()
        try {
            if (transactions!!.containsKey(tx.txId)) {
                return
            }
            tx.transactionMeta!!.setState(TransactionMeta.MetaState.PENDING)
            transactions[tx.txId] = tx
            ledger.record(tx)
        } finally {
            lock.unlock()
        }
    }

    fun getStandardTransaction(txId: Sha256Hash?): StandardTransaction? {
        return transactions!![txId]
    }

    fun getStandardTransactions(): Collection<StandardTransaction> =
        Collections.unmodifiableCollection(transactions!!.values)

    private fun isTransactionRelevant(tx: StandardTransaction): Boolean {
        if (transactions!!.containsKey(tx.txId)) {
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

    private fun addTransaction(tx: StandardTransaction) {
        lock.lock()
        try {
            transactions!!.putIfAbsent(tx.txId, tx)
            ledger.record(tx)
            //save();
        } finally {
            lock.unlock()
        }
    }

}
