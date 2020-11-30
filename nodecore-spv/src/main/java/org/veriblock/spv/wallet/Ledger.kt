// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.spv.wallet

import org.veriblock.sdk.models.Coin
import org.veriblock.spv.model.StandardTransaction
import org.veriblock.spv.model.TransactionMeta
import java.util.ArrayList
import java.util.HashMap
import java.util.function.Consumer

class Ledger {
    private val entries: MutableMap<String, AddressLedger> = HashMap()

    fun add(address: String) {
        entries.putIfAbsent(
            address,
            AddressLedger(address, Coin.ZERO, 0)
        )
    }

    operator fun get(address: String): AddressLedger {
        return entries.getValue(address)
    }

    fun list(): Collection<AddressLedger> {
        return entries.values
    }

    fun load(toLoad: List<AddressLedger>) {
        entries.clear()
        for (addressLedger in toLoad) {
            add(addressLedger)
        }
    }

    fun record(tx: StandardTransaction) {
        val ledgerEntries = createLedgerEntriesFrom(tx)
        ledgerEntries.forEach {
            get(it.address).add(it)
        }
    }

    private fun add(addressLedger: AddressLedger) {
        entries.putIfAbsent(addressLedger.address, addressLedger)
    }

    private fun createLedgerEntriesFrom(tx: StandardTransaction): List<LedgerEntry> {
        val ledgerEntries = ArrayList<LedgerEntry>()
        val status = if (tx.transactionMeta!!.state === TransactionMeta.MetaState.CONFIRMED) {
            LedgerEntry.Status.CONFIRMED
        } else {
            LedgerEntry.Status.PENDING
        }
        if (entries.containsKey(tx.inputAddress.get())) {
            ledgerEntries.add(
                LedgerEntry(tx.inputAddress.get(), tx.txId, tx.inputAmount!!, Coin.ZERO, tx.getSignatureIndex(), 0, status)
            )
        }
        for (i in tx.getOutputs().indices) {
            val o = tx.getOutputs()[i]
            if (entries.containsKey(o.address.get())) {
                ledgerEntries.add(
                    LedgerEntry(o.address.get(), tx.txId, Coin.ZERO, o.amount, -1, i, status)
                )
            }
        }
        return ledgerEntries
    }
}
