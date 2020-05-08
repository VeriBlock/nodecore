package veriblock.service;

import nodecore.api.grpc.VeriBlockMessages;
import org.veriblock.sdk.models.Sha256Hash;
import spark.utils.CollectionUtils;
import veriblock.model.Transaction;
import veriblock.service.PendingTransactionContainer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class PendingTransactionContainer {
    private final Map<String, ArrayList<Transaction>> pendingAddressTransaction = new ConcurrentHashMap<>();
    private final Map<Sha256Hash, Transaction> pendingTxIdTransaction = new ConcurrentHashMap<>();
    private final Map<Sha256Hash, VeriBlockMessages.TransactionInfo> confirmedTxIdTransactionReply = new ConcurrentHashMap<>();
    private final Set<Sha256Hash> transactionsForMonitoring = ConcurrentHashMap.newKeySet();

    public Set<Sha256Hash> getPendingTransactionsId() {
        Set<Sha256Hash> allPendingTx = new HashSet<>();
        allPendingTx.addAll(pendingTxIdTransaction.keySet());
        allPendingTx.addAll(transactionsForMonitoring);

        return allPendingTx;
    }

    public VeriBlockMessages.TransactionInfo getTransactionInfo(Sha256Hash txId) {
        if (confirmedTxIdTransactionReply.containsKey(txId)) {
            return confirmedTxIdTransactionReply.get(txId);
        }

        if (!pendingTxIdTransaction.containsKey(txId)) {
            transactionsForMonitoring.add(txId);
        }

        return VeriBlockMessages.TransactionInfo.newBuilder()
            .setConfirmations(0)
            .build();
    }

    public void updateTransactionInfo(VeriBlockMessages.TransactionInfo transactionInfo) {
        Sha256Hash txId = Sha256Hash.wrap(transactionInfo.getTransaction().getTxId().toByteArray());

        if (pendingTxIdTransaction.containsKey(txId)) {
            confirmedTxIdTransactionReply.put(txId, transactionInfo);

            if (transactionInfo.getConfirmations() > 0) {
                pendingTxIdTransaction.remove(txId);
                transactionsForMonitoring.remove(txId);
            }
        }
    }

    public boolean addTransaction(Transaction transaction) {
        ArrayList<Transaction> transactions = pendingAddressTransaction.get(transaction.getInputAddress().toString());

        if (CollectionUtils.isNotEmpty(transactions)) {
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

    public Transaction getTransaction(Sha256Hash txId) {
        return pendingTxIdTransaction.get(txId);
    }

    public Long getPendingSignatureIndexForAddress(String address) {
        ArrayList<Transaction> transactions = pendingAddressTransaction.get(address);

        if(CollectionUtils.isNotEmpty(transactions)){
            return transactions.get(transactions.size() - 1).getSignatureIndex();
        }
        return null;
    }
}
