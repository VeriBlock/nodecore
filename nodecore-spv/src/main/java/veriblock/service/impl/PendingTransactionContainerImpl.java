package veriblock.service.impl;

import com.google.inject.Singleton;
import org.veriblock.sdk.models.Sha256Hash;
import spark.utils.CollectionUtils;
import veriblock.model.Transaction;
import veriblock.service.PendingTransactionContainer;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class PendingTransactionContainerImpl implements PendingTransactionContainer {
    private Map<String, ArrayList<Transaction>> pendingAddressTransaction = new ConcurrentHashMap<>();
    private Map<Sha256Hash, Transaction> pendingTxIdTransaction = new ConcurrentHashMap<>();

    @Override
    public boolean addTransaction(Transaction transaction) {
        ArrayList<Transaction> transactions = pendingAddressTransaction.get(transaction.getInputAddress().toString());

        if(CollectionUtils.isNotEmpty(transactions)) {
            transactions.add(transaction);
            pendingTxIdTransaction.put(transaction.getTxId(), transaction);
        } else {
            ArrayList<Transaction> newList = new ArrayList<>();
            newList.add(transaction);

            pendingTxIdTransaction.put(transaction.getTxId(), transaction);
            pendingAddressTransaction.put(transaction.getInputAddress().toString(), newList);
        }
        return true;
    }

    @Override
    public Transaction getTransaction(Sha256Hash txId) {
        return pendingTxIdTransaction.get(txId);
    }

    @Override
    public Long getPendingSignatureIndexForAddress(String address) {
        ArrayList<Transaction> transactions = pendingAddressTransaction.get(address);

        if(CollectionUtils.isNotEmpty(transactions)){
            return transactions.get(transactions.size() - 1).getSignatureIndex();
        }
        return null;
    }
}
