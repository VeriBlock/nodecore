package veriblock.service;

import org.veriblock.sdk.models.Sha256Hash;
import veriblock.model.Transaction;

/**
 * Container for pending transactions.
 */
public interface PendingTransactionContainer {

    boolean addTransaction(Transaction transaction);

    Transaction getTransaction(Sha256Hash txId);

    Long getPendingSignatureIndexForAddress(String address);

}
