// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package veriblock.model;

import org.veriblock.sdk.models.Sha256Hash;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import static com.google.common.base.Preconditions.checkNotNull;

public class TransactionPool {
    private static class WeakPoolReference extends WeakReference<TransactionMeta> {
        public Sha256Hash hash;
        public WeakPoolReference(TransactionMeta meta, ReferenceQueue<TransactionMeta> queue) {
            super(meta, queue);
            hash = meta.getTxId();
        }
    }

    private final ReentrantLock lock = new ReentrantLock(true);
    private final ReferenceQueue<TransactionMeta> referenceQueue = new ReferenceQueue<>();
    private final LinkedHashMap<Sha256Hash, WeakPoolReference> pool = new LinkedHashMap<Sha256Hash, WeakPoolReference>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Sha256Hash, WeakPoolReference> eldest) {
            return size() > 1000;
        }
    };

    public int record(Sha256Hash txId, String peerAddress) {
        lock.lock();
        try {
            purge();
            TransactionMeta tx = getOrCreate(txId);
            boolean wasNew = tx.recordBroadcast(peerAddress);
            if (wasNew) {
                // TODO: Add listener for a newly seen tx

                return tx.getBroadcastPeerCount();
            } else {
                return 0;
            }
        } finally {
            lock.unlock();
        }
    }

    public TransactionMeta getOrCreate(Sha256Hash txId) {
        checkNotNull(txId);

        lock.lock();
        try {
            WeakPoolReference reference = pool.get(txId);
            if (reference != null) {
                TransactionMeta transactionMeta = reference.get();
                if (transactionMeta != null) {
                    return transactionMeta;
                }
            }

            TransactionMeta tx = new TransactionMeta(txId);
            pool.put(txId, new WeakPoolReference(tx, referenceQueue));
            return tx;
        } finally {
            lock.unlock();
        }
    }

    public void insert(TransactionMeta meta) {
        checkNotNull(meta);

        lock.lock();
        try {
            pool.put(meta.getTxId(), new WeakPoolReference(meta, referenceQueue));
        } finally {
            lock.unlock();
        }
    }

    private void purge() {
        lock.lock();
        try {
            Reference<? extends TransactionMeta> reference;
            while ((reference = referenceQueue.poll()) != null) {
                WeakPoolReference txReference = (WeakPoolReference)reference;
                pool.remove(txReference.hash);
            }
        } finally {
            lock.unlock();
        }
    }
}
