// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package veriblock.wallet;

import org.veriblock.sdk.models.Coin;
import veriblock.model.Output;
import veriblock.model.StandardTransaction;
import veriblock.model.TransactionMeta;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Ledger {
    private Map<String, AddressLedger> entries = new HashMap<>();

    public void add(String address) {
        entries.putIfAbsent(address,
                new AddressLedger(address, Coin.ZERO, 0));
    }

    public AddressLedger get(String address) {
        return entries.get(address);
    }

    public Collection<AddressLedger> list() {
        return entries.values();
    }

    public void load(List<AddressLedger> toLoad) {
        entries.clear();
        for (AddressLedger addressLedger : toLoad) {
            add(addressLedger);
        }
    }

    public void record(StandardTransaction tx) {
        List<LedgerEntry> ledgerEntries = createLedgerEntriesFrom(tx);
        ledgerEntries.forEach(entry -> get(entry.getAddress()).add(entry));
    }

    private void add(AddressLedger addressLedger) {
        entries.putIfAbsent(addressLedger.getAddress(), addressLedger);
    }

    private List<LedgerEntry> createLedgerEntriesFrom(StandardTransaction tx) {
        List<LedgerEntry> ledgerEntries = new ArrayList<>();

        LedgerEntry.Status status = (tx.getTransactionMeta().getState() == TransactionMeta.MetaState.CONFIRMED) ? LedgerEntry.Status.CONFIRMED : LedgerEntry.Status.PENDING;
        if (entries.containsKey(tx.getInputAddress().get())) {
            ledgerEntries.add(new LedgerEntry(tx.getInputAddress().get(),
                    tx.getTxId(),
                    tx.getInputAmount(),
                    Coin.ZERO,
                    tx.getSignatureIndex(),
                    0,
                    status));
        }

        for (int i = 0; i < tx.getOutputs().size(); i++) {
            Output o = tx.getOutputs().get(i);
            if (entries.containsKey(o.getAddress().get())) {
                ledgerEntries.add(new LedgerEntry(o.getAddress().get(),
                        tx.getTxId(),
                        Coin.ZERO,
                        o.getAmount(),
                        -1,
                        i,
                        status));
            }
        }

        return ledgerEntries;
    }
}
