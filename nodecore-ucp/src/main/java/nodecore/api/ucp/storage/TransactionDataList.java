// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.api.ucp.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.veriblock.core.types.Pair;
import org.veriblock.core.utilities.Utility;

import java.util.ArrayList;
import java.util.List;

public class TransactionDataList {
    private static final Logger _logger = LoggerFactory.getLogger(TransactionDataList.class);
    private static final char SEPARATOR = ';';

    private ArrayList<String> transactionList = new ArrayList<>();

    public ArrayList<String> getTransactionList() {
        return new ArrayList<String>(transactionList);
    }

    public TransactionDataList clone() {
        TransactionDataList toReturn = new TransactionDataList(serialize());
        return toReturn;
    }

    public boolean applyDeltaLists(DeltaList toRemove, DeltaList toAdd) {
        ArrayList<String> transactionListCopy = new ArrayList<String>(transactionList);
        if (toRemove != null) {
            for (int i = 0; i < toRemove.getSize(); i++) {
                Pair<Integer, String> removalPair = toRemove.getPairAtIndex(i);
                int removalIndex = removalPair.getFirst();
                if (removalIndex >= transactionListCopy.size()) {
                    _logger.error("We attempted to remove an element at index " + i + " but our working mepool is only " + transactionListCopy.size() + " elements long!");
                    return false;
                }
                String transactionForRemoval = transactionListCopy.get(removalIndex);
                if (!transactionForRemoval.equals(removalPair.getSecond())) {
                    _logger.error("DeltaList says index " + removalIndex + " should contain (and remove) " + removalPair.getSecond() + " but it holds " + transactionForRemoval + " there instead.");
                    return false;
                }

                transactionListCopy.remove(removalIndex);
            }
        }

        if (toAdd != null) {
            for (int i = 0; i < toAdd.getSize(); i++) {
                Pair<Integer, String> insertionPair = toAdd.getPairAtIndex(i);
                int insertionIndex = insertionPair.getFirst();
                if (insertionIndex > transactionListCopy.size()) {
                    _logger.error("We attempted to insert an element at index " + i + " but our working mempool is only " + transactionListCopy.size() + " elements long!");
                    return false;
                }
                String transactionForInsertion = insertionPair.getSecond();
                transactionListCopy.add(insertionIndex, transactionForInsertion);
            }
        }

        transactionList = transactionListCopy;
        return true;
    }

    public TransactionDataList(String txList) {
        if (txList == null) {
            throw new IllegalArgumentException("TransactionDataList constructor cannot be called with a null txList!");
        }

        if (!txList.equals("")) {
            String[] parts = txList.split(";");

            if (parts.length < 1) {
                throw new IllegalArgumentException("TransactionDataList constructor cannot be called with an empty txList!");
            }

            for (int i = 0; i < parts.length; i++) {
                addTransaction(parts[i]);
            }
        }
    }

    public TransactionDataList() {
        // Do nothing
    }

    public static TransactionDataList fromStringList(List<String> serializedTransactions) {

        if (serializedTransactions == null) {
            throw new IllegalArgumentException("TransactionDataList constructor cannot be called with a null txList!");
        }

        TransactionDataList toReturn = new TransactionDataList();

        for (int i = 0; i < serializedTransactions.size(); i++) {
            toReturn.addTransaction(serializedTransactions.get(i));
        }

        return toReturn;
    }

    public TransactionDataList(List<String> txList) {
        if (txList == null) {
            throw new IllegalArgumentException("TransactionDataList constructor cannot be called with a null txList!");
        }

        for (String tx : txList) {
            this.addTransaction(tx);
        }
    }

    public void addTransaction(String transactionData) {
        if (!Utility.isHex(transactionData)) {
            throw new IllegalArgumentException("Transactions in the transaction list must be encoded in hexadecimal!");
        }
        transactionList.add(transactionData);
    }

    public int getSize() {
        return transactionList.size();
    }

    public String getTransactionDataAtIndex(int index) {
        if (index >= getSize()) {
            throw new IllegalArgumentException("getTransactionDataAtIndex must contain a positive-or-zero index below it's size (" + getSize()+ ") but was passed " + index + "!");
        }

        return transactionList.get(index);
    }

    public String serialize() {
        StringBuilder serialized = new StringBuilder();
        for (int i = 0; i < transactionList.size(); i++) {
            serialized.append(transactionList.get(i));
            if (i != transactionList.size() - 1) {
                serialized.append(SEPARATOR);
            }
        }

        return serialized.toString();
    }

    public String toString() {
        return serialize();
    }

    public boolean equals(Object o) {
        if (!(o instanceof TransactionDataList)) {
            return false;
        }

        TransactionDataList toTest = (TransactionDataList)o;

        return toTest.serialize().equals(serialize());
    }

    @Override
    public int hashCode() {
        return serialize().hashCode();
    }
}
