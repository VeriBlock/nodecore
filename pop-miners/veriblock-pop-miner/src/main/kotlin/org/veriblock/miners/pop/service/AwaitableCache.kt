// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.miners.pop.service

import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import org.veriblock.core.utilities.createLogger
import java.util.LinkedHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

private val logger = createLogger {}

/**
 * Cache for objects that we're expecting to be put in a future.
 * The [get] method suspends execution until [put] is called for its key.
 *
 * NOTE: If a [get] is called with no timeout it's exposed to be suspended forever!
 * NOTE 2: In the case an existing object gets old, removed and never put again,
 * [get] will suspend the execution forever as well!
 */
class AwaitableCache<K, V>(
    private val maxSize: Int
) {
    private val valueCache = object : LinkedHashMap<K, V>() {
        override fun removeEldestEntry(eldest: Map.Entry<K, V>): Boolean {
            return this.size > maxSize
        }
    }
    private val deferredCache = HashMap<K, ConflatedBroadcastChannel<V>>()

    private val lock = ReentrantLock(true)

    fun put(key: K, value: V) {
        lock.withLock {
            // Check for any subscriptors to this value
            deferredCache[key]?.let {
                // Offer the value to all subscriptors of the broadcast channel
                it.offer(value)
                // Remove the broadcast channel
                deferredCache.remove(key)
            }

            // Put the value in the value cache for future direct access
            valueCache.put(key, value)
        }
    }

    suspend fun get(key: K): V {
        val deferred = lock.withLock {
            // Return a value directly if it exists in the value cache
            valueCache[key]?.let {
                return it
            }
            // Retrieve (or create) a broadcast channel for this key
            deferredCache.getOrPut(key) {
                ConflatedBroadcastChannel()
            }
        }

        // Open a subscription in the retrieved broadcast channel and suspend execution
        return deferred.openSubscription().receive()
    }
}
