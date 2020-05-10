package veriblock.wallet;

import org.veriblock.sdk.models.Sha256Hash;
import veriblock.SpvContext;
import veriblock.listeners.PendingTransactionDownloadedListener;
import veriblock.model.StandardTransaction;
import veriblock.model.TransactionMeta;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class PendingTransactionDownloadedListenerImpl implements PendingTransactionDownloadedListener {

    private final SpvContext spvContext;

    private Map<Sha256Hash, StandardTransaction> transactions;
    private final Ledger ledger = new Ledger();

    private final KeyRing keyRing = new KeyRing();
    private final ReentrantLock lock = new ReentrantLock(true);

    public PendingTransactionDownloadedListenerImpl(SpvContext spvContext) {
        this.spvContext = spvContext;
    }

    @Override
    public void onPendingTransactionDownloaded(StandardTransaction transaction) {

    }

    void loadTransactions(List<StandardTransaction> toLoad) {
        for (StandardTransaction tx : toLoad) {
            transactions.put(tx.getTxId(), tx);
            spvContext.getTransactionPool().insert(tx.getTransactionMeta());
        }
    }

    public void commitTx(StandardTransaction tx) {
        lock.lock();
        try {
            if (transactions.containsKey(tx.getTxId())) {
                return;
            }

            tx.getTransactionMeta().setState(TransactionMeta.MetaState.PENDING);
            transactions.put(tx.getTxId(), tx);

            ledger.record(tx);
        } finally {
            lock.unlock();
        }
    }

    public StandardTransaction getStandardTransaction(Sha256Hash txId) {
        return transactions.get(txId);
    }

    public Collection<StandardTransaction> getStandardTransactions() {
        return Collections.unmodifiableCollection(transactions.values());
    }

    private boolean isTransactionRelevant(StandardTransaction tx) {
        if (this.transactions.containsKey(tx.getTxId())) {
            return true;
        }

        if (keyRing.contains(tx.getInputAddress().get())) {
            return true;
        }

        return tx.getOutputs().stream().anyMatch(output -> keyRing.contains(output.getAddress().get()));
    }

    private void addTransaction(StandardTransaction tx) {
        lock.lock();
        try {
            transactions.putIfAbsent(tx.getTxId(), tx);
            ledger.record(tx);
//            save();
        } finally {
            lock.unlock();
        }
    }
}
