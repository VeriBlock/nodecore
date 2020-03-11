// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.miners.pop.services

import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import org.bitcoinj.core.FilteredBlock
import org.veriblock.core.utilities.createLogger
import java.util.LinkedHashMap
import java.util.concurrent.locks.ReentrantLock

private val logger = createLogger {}

private const val CACHE_SIZE = 72

class BitcoinBlockCache {

    private val cache = object : LinkedHashMap<String, SettableFuture<FilteredBlock>>() {
        override fun removeEldestEntry(eldest: Map.Entry<String, SettableFuture<FilteredBlock>>): Boolean {
            return this.size > CACHE_SIZE
        }
    }

    private val lock = ReentrantLock(true)
    fun put(key: String, block: FilteredBlock) {
        val task: SettableFuture<FilteredBlock>
        task = try {
            lock.lock()
            cache.computeIfAbsent(key) {
                logger.debug("Placing block {} in downloaded cache", block.hash.toString())
                val future = SettableFuture.create<FilteredBlock>()
                future.set(block)
                future
            }
        } finally {
            lock.unlock()
        }
        if (!task.isDone) {
            logger.debug("Placing block {} in downloaded cache", block.hash.toString())
            task.set(block)
        }
    }

    fun getAsync(key: String): ListenableFuture<FilteredBlock> {
        val task: ListenableFuture<FilteredBlock>
        task = try {
            lock.lock()
            logger.debug("Requesting block {} from download cache", key)
            cache.computeIfAbsent(key) { SettableFuture.create() }
        } finally {
            lock.unlock()
        }
        return task
    }
}
