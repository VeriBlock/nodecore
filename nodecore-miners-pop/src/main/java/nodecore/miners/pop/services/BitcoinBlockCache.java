// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.services;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.inject.Singleton;
import org.bitcoinj.core.FilteredBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

@Singleton
public class BitcoinBlockCache {
    private static final Logger logger = LoggerFactory.getLogger(BitcoinBlockCache.class);

    private final LinkedHashMap<String, SettableFuture<FilteredBlock>> cache = new LinkedHashMap<String, SettableFuture<FilteredBlock>>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, SettableFuture<FilteredBlock>> eldest) {
            return this.size() > 72;
        }
    };

    private final ReentrantLock lock = new ReentrantLock(true);

    public BitcoinBlockCache() {
    }

    public void put(String key, FilteredBlock block) {
        SettableFuture<FilteredBlock> task;

        try {
            lock.lock();
            task = cache.computeIfAbsent(key, k -> {
                logger.debug("Placing block {} in downloaded cache", block.getHash().toString());
                final SettableFuture<FilteredBlock> future = SettableFuture.create();
                future.set(block);
                return future;
            });
        } finally {
            lock.unlock();
        }

        if (!task.isDone()) {
            logger.debug("Placing block {} in downloaded cache", block.getHash().toString());
            task.set(block);
        }
    }

    public ListenableFuture<FilteredBlock> getAsync(String key) {
        ListenableFuture<FilteredBlock> task;

        try {
            lock.lock();
            logger.debug("Requesting block {} from download cache", key);
            task = cache.computeIfAbsent(key, k -> SettableFuture.create());
        } finally {
            lock.unlock();
        }

        return task;
    }
}
