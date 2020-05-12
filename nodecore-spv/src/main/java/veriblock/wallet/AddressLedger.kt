// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package veriblock.wallet

import org.veriblock.sdk.models.Coin
import org.veriblock.core.crypto.Sha256Hash
import veriblock.model.Balance
import java.util.LinkedHashMap
import java.util.Objects
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock

class AddressLedger(
    val address: String,
    val startingBalance: Coin,
    val startingSignatureIndex: Long
) {
    private val lock: ReadWriteLock = ReentrantReadWriteLock(true)
    private val entries: LinkedHashMap<Sha256Hash, LedgerEntry> = LinkedHashMap()

    fun getCurrentBalance(): Balance {
        lock.readLock().lock()
        return try {
            val current = Balance(startingBalance)
            for (entry in entries.values) {
                if (entry.status == LedgerEntry.Status.PENDING) {
                    current.addPendingSpend(entry.debitAmount)
                    current.addPendingReceipt(entry.creditAmount)
                } else {
                    current.addConfirmedSpend(entry.debitAmount)
                    current.addConfirmedReceipt(entry.creditAmount)
                }
            }
            current
        } finally {
            lock.readLock().unlock()
        }
    }

    fun getCurrentSignatureIndex(): SignatureIndex {
        lock.readLock().lock()
        return try {
            val current = SignatureIndex(startingSignatureIndex)
            val confirmed = entries.values.parallelStream()
                .filter { entry: LedgerEntry -> entry.status != LedgerEntry.Status.PENDING }
                .mapToLong { obj: LedgerEntry -> obj.signatureIndex }
                .filter { obj: Long -> Objects.nonNull(obj) }
                .max()
            if (confirmed.isPresent) {
                current.value = confirmed.asLong
            }
            val pending = entries.values.parallelStream()
                .filter { entry: LedgerEntry -> entry.status == LedgerEntry.Status.PENDING }
                .mapToLong { obj: LedgerEntry -> obj.signatureIndex }
                .filter { obj: Long -> Objects.nonNull(obj) }
                .max()
            if (pending.isPresent) {
                current.pending = pending.asLong
            }
            current
        } finally {
            lock.readLock().unlock()
        }
    }

    fun getEntries(): Collection<LedgerEntry> {
        return entries.values
    }

    fun add(entry: LedgerEntry) {
        lock.writeLock().lock()
        try {
            entries[entry.key] = entry
        } finally {
            lock.writeLock().unlock()
        }
    }
}
