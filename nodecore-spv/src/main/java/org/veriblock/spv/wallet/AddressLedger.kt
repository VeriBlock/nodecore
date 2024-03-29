// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.spv.wallet

import org.veriblock.core.contracts.Balance
import org.veriblock.sdk.models.Coin
import org.veriblock.core.crypto.Sha256Hash
import java.util.LinkedHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class AddressLedger(
    val address: String,
    val startingBalance: Coin,
    val startingSignatureIndex: Long
) {
    private val lock = ReentrantReadWriteLock(true)
    private val entries: LinkedHashMap<Sha256Hash, LedgerEntry> = LinkedHashMap()

    fun getCurrentBalance(): Balance = lock.read {
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

    fun getCurrentSignatureIndex(): SignatureIndex = lock.read {
        SignatureIndex(
            value = entries.values.asSequence()
                .filter { it.status != LedgerEntry.Status.PENDING }
                .maxOfOrNull { it.signatureIndex }
                ?: startingSignatureIndex,
            pending = entries.values.asSequence()
                .filter { it.status == LedgerEntry.Status.PENDING }
                .maxOfOrNull { it.signatureIndex }
                ?: startingSignatureIndex
        )
    }

    fun getEntries(): Collection<LedgerEntry> {
        return entries.values
    }

    fun add(entry: LedgerEntry) = lock.write {
        entries[entry.key] = entry
    }
}
