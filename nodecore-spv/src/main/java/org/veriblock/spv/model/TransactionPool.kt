// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.spv.model

import io.ktor.util.network.*
import org.veriblock.core.crypto.Sha256Hash
import org.veriblock.core.crypto.VbkTxId
import java.lang.ref.Reference
import java.lang.ref.ReferenceQueue
import java.lang.ref.WeakReference
import java.util.LinkedHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class TransactionPool {
    private class WeakPoolReference(
        meta: TransactionMeta,
        queue: ReferenceQueue<TransactionMeta>?
    ) : WeakReference<TransactionMeta>(meta, queue) {
        var hash: Sha256Hash? = meta.txId
    }

    private val lock = ReentrantLock(true)
    private val referenceQueue = ReferenceQueue<TransactionMeta>()
    private val pool: LinkedHashMap<VbkTxId, WeakPoolReference> = object : LinkedHashMap<VbkTxId, WeakPoolReference>() {
        override fun removeEldestEntry(eldest: Map.Entry<VbkTxId, WeakPoolReference>): Boolean {
            return size > 1000
        }
    }

    fun record(txId: VbkTxId, peerAddress: NetworkAddress): Int = lock.withLock {
        purge()
        val tx = getOrCreate(txId)
        val wasNew = tx.recordBroadcast(peerAddress)
        if (wasNew) {
            // TODO: Add listener for a newly seen tx
            tx.broadcastPeerCount
        } else {
            0
        }
    }

    fun getOrCreate(txId: VbkTxId): TransactionMeta = lock.withLock {
        val reference = pool[txId]
        if (reference != null) {
            val transactionMeta = reference.get()
            if (transactionMeta != null) {
                return transactionMeta
            }
        }
        val tx = TransactionMeta(txId)
        pool[txId] = WeakPoolReference(tx, referenceQueue)
        tx
    }

    fun insert(meta: TransactionMeta) = lock.withLock {
        pool[meta.txId] = WeakPoolReference(meta, referenceQueue)
    }

    private fun purge() = lock.withLock {
        var reference: Reference<out TransactionMeta>? = referenceQueue.poll()
        while (reference != null) {
            val txReference = reference as WeakPoolReference
            pool.remove(txReference.hash)
            reference = referenceQueue.poll()
        }
    }
}
