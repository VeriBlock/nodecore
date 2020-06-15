// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package veriblock.wallet

import org.veriblock.core.contracts.Balance
import org.veriblock.sdk.models.Coin
import org.veriblock.core.crypto.Sha256Hash
import org.veriblock.core.utilities.extensions.withReadLock
import org.veriblock.core.utilities.extensions.withWriteLock
import java.util.LinkedHashMap
import java.util.Objects
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.withLock

class AddressLedger(
    val address: String,
    val startingBalance: Coin,
    val startingSignatureIndex: Long
) {
    private val lock: ReadWriteLock = ReentrantReadWriteLock(true)
    private val entries: LinkedHashMap<Sha256Hash, LedgerEntry> = LinkedHashMap()

    fun getCurrentBalance(): Balance = lock.withReadLock {
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
    }

    fun getCurrentSignatureIndex(): SignatureIndex = lock.withReadLock {
        SignatureIndex(
            value = entries.values.asSequence()
                .filter { it.status != LedgerEntry.Status.PENDING }
                .mapNotNull { it.signatureIndex }
                .max() ?: startingSignatureIndex,
            pending = entries.values.asSequence()
                .filter { it.status == LedgerEntry.Status.PENDING }
                .mapNotNull { it.signatureIndex }
                .max() ?: startingSignatureIndex
        )
    }

    fun getEntries(): Collection<LedgerEntry> {
        return entries.values
    }

    fun add(entry: LedgerEntry) = lock.withWriteLock {
        entries[entry.key] = entry
    }
}
