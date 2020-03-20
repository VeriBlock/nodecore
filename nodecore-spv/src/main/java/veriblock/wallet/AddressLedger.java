// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package veriblock.wallet;

import org.veriblock.sdk.models.Coin;
import org.veriblock.sdk.models.Sha256Hash;
import veriblock.model.Balance;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class AddressLedger {
    private final ReadWriteLock lock = new ReentrantReadWriteLock(true);

    private final String address;
    private final LinkedHashMap<Sha256Hash, LedgerEntry> entries;

    private Coin startingBalance;
    private long startingSignatureIndex;

    public String getAddress() {
        return address;
    }

    public Coin getStartingBalance() {
        return startingBalance;
    }

    public long getStartingSignatureIndex() {
        return startingSignatureIndex;
    }

    public Balance getCurrentBalance() {
        lock.readLock().lock();
        try {
            Balance current = new Balance(startingBalance);
            for (LedgerEntry entry : entries.values()) {
                if (entry.getStatus() == LedgerEntry.Status.PENDING) {
                    current.addPendingSpend(entry.getDebitAmount());
                    current.addPendingReceipt(entry.getCreditAmount());
                } else {
                    current.addConfirmedSpend(entry.getDebitAmount());
                    current.addConfirmedReceipt(entry.getCreditAmount());
                }
            }

            return current;
        } finally {
            lock.readLock().unlock();
        }
    }

    public SignatureIndex getCurrentSignatureIndex() {
        lock.readLock().lock();
        try {
            SignatureIndex current = new SignatureIndex(startingSignatureIndex);
            OptionalLong confirmed = entries.values().parallelStream()
                    .filter(entry -> entry.getStatus() != LedgerEntry.Status.PENDING)
                    .mapToLong(LedgerEntry::getSignatureIndex)
                    .filter(Objects::nonNull)
                    .max();

            if (confirmed.isPresent()) {
                current.set(confirmed.getAsLong());
            }

            OptionalLong pending = entries.values().parallelStream()
                    .filter(entry -> entry.getStatus() == LedgerEntry.Status.PENDING)
                    .mapToLong(LedgerEntry::getSignatureIndex)
                    .filter(Objects::nonNull)
                    .max();

            if (pending.isPresent()) {
                current.setPending(pending.getAsLong());
            }

            return current;
        } finally {
            lock.readLock().unlock();
        }
    }

    public Collection<LedgerEntry> getEntries() {
        return entries.values();
    }

    public AddressLedger(String address, Coin startingBalance, long startingSignatureIndex) {
        this.address = address;
        this.entries = new LinkedHashMap<>();

        this.startingBalance = startingBalance;
        this.startingSignatureIndex = startingSignatureIndex;
    }

    public void add(LedgerEntry entry) {
        lock.writeLock().lock();
        try {
            entries.put(entry.getKey(), entry);
        } finally {
            lock.writeLock().unlock();
        }
    }
}
