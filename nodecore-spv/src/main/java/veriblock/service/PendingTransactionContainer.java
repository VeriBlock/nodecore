package veriblock.service;

import nodecore.api.grpc.VeriBlockMessages;
import org.veriblock.sdk.models.Sha256Hash;
import veriblock.model.Transaction;

import java.util.Set;

/**
 * Container for pending transactions.
 */
public interface PendingTransactionContainer {

    Set<Sha256Hash> getPendingTransactionsId();

    VeriBlockMessages.TransactionInfo getTransactionInfo(Sha256Hash txId);

    void updateTransactionInfo(VeriBlockMessages.TransactionInfo transactionInfo);

    boolean addTransaction(Transaction transaction);

    Transaction getTransaction(Sha256Hash txId);

    Long getPendingSignatureIndexForAddress(String address);
}
