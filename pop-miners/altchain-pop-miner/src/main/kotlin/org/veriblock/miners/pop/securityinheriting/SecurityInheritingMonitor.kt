// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.securityinheriting

import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.CONFLATED
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.first
import org.veriblock.core.utilities.Configuration
import org.veriblock.core.utilities.createLogger
import org.veriblock.core.utilities.debugWarn
import org.veriblock.lite.util.Threading
import org.veriblock.miners.pop.service.MinerService
import org.veriblock.sdk.alt.SecurityInheritingChain
import org.veriblock.sdk.alt.model.SecurityInheritingBlock
import org.veriblock.sdk.alt.model.SecurityInheritingTransaction
import org.veriblock.sdk.util.checkSuccess
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

private val logger = createLogger {}

class SecurityInheritingMonitor(
    configuration: Configuration,
    private val chainId: String,
    private val chain: SecurityInheritingChain
) {
    private val pollingPeriodSeconds = configuration.getLong("securityInheriting.$chainId.pollingPeriodSeconds") ?: 20L

    private val lock = ReentrantLock()

    private lateinit var miner: MinerService

    private val healthy = AtomicBoolean(false)
    private val connected = SettableFuture.create<Boolean>()

    private var bestBlockHeight: Int = -1

    private var pollSchedule: ScheduledFuture<*>? = null

    val newBlockHeightBroadcastChannel = BroadcastChannel<Int>(CONFLATED)

    private val blockHeightListeners = HashMap<Int, MutableList<Channel<SecurityInheritingBlock>>>()
    private val transactionListeners = HashMap<String, MutableList<Channel<SecurityInheritingTransaction>>>()

    /**
     * Starts monitoring the corresponding chain with a polling schedule
     */
    fun start(miner: MinerService) {
        this.miner = miner
        pollSchedule = Threading.SI_POLL_THREAD.scheduleWithFixedDelay({
            poll()
        }, 5L, pollingPeriodSeconds, TimeUnit.SECONDS)

        logger.info("Connecting to SI Chain ($chainId)...")
        connected.addListener(Runnable {
            logger.info("Connected to SI Chain ($chainId)!")
        }, Threading.SI_POLL_THREAD)
    }

    /**
     * Stops the polling schedule
     */
    fun stop() {
        pollSchedule?.cancel(false)
        pollSchedule = null
    }

    /**
     * Checks for the best chain's block height. If it changed, it handles all registered listeners.
     * Automining is also triggered here.
     */
    private fun poll() {
        try {
            if (healthy.get()) {
                val bestBlockHeight: Int = try {
                    chain.getBestBlockHeight()
                } catch (e: Exception) {
                    logger.debugWarn(e) { "Error while retrieving ${chain.name} tip height: $e" }
                    healthy.set(false)
                    return
                }

                if (bestBlockHeight != this.bestBlockHeight) {
                    logger.debug { "New chain head detected!" }
                    if (this.bestBlockHeight != -1 && chain.shouldAutoMine(bestBlockHeight)) {
                        miner.mine(chainId, bestBlockHeight)
                    }

                    this.bestBlockHeight = bestBlockHeight
                    newBlockHeightBroadcastChannel.offer(bestBlockHeight)

                    handleBlockHeightListeners()
                    handleTransactionListeners()
                }
            } else {
                val pinged = checkSuccess { chain.getBestBlockHeight() }
                if (pinged) {
                    healthy.set(true)
                    connected.set(true)
                }
            }
        } catch (t: Throwable) {
            logger.debugWarn(t) { "Error when polling SI Chain ($chainId)" }
        }
    }

    private fun getBlockAtHeight(height: Int): SecurityInheritingBlock? {
        // Ignore if we didn't still reach the registered height yet
        if (height > bestBlockHeight) {
            return null
        }

        return try {
            // Retrieve block from SI chain
            chain.getBlock(height)
        } catch (e: Exception) {
            logger.warn(e) { "Error when polling for block $height" }
            null
        }
        // The best block should never be null if the chain's integrity is not compromised
            ?: throw IllegalStateException("Unable to find block with height $height while the best chain height is $bestBlockHeight!")
    }

    private fun getTransaction(txId: String): SecurityInheritingTransaction? = try {
        // Retrieve block from SI chain
        chain.getTransaction(txId)
    } catch (e: Exception) {
        logger.warn(e) { "Error when polling for transaction $txId" }
        null
    }

    private fun handleBlockHeightListeners() = lock.withLock {
        for ((height, listeners) in blockHeightListeners) {
            val block = getBlockAtHeight(height)
            if (block != null) {
                for (listener in listeners) {
                    listener.offer(block)
                }
            }
        }
    }

    private fun handleTransactionListeners() = lock.withLock {
        for ((txId, listeners) in transactionListeners) {
            val transaction = getTransaction(txId)
            if (transaction != null) {
                for (listener in listeners) {
                    listener.offer(transaction)
                }
            }
        }
    }

    suspend fun getBlockAtHeight(height: Int, predicate: (SecurityInheritingBlock) -> Boolean): SecurityInheritingBlock {
        // Check if we can skip the subscription
        val block = getBlockAtHeight(height)
        if (block != null && predicate(block)) {
            return block
        }

        val channel = subscribe(blockHeightListeners, height)
        return channel.consumeAsFlow().first {
            predicate(it)
        }
    }

    suspend fun getTransaction(txId: String, predicate: (SecurityInheritingTransaction) -> Boolean): SecurityInheritingTransaction {
        // Check if we can skip the subscription
        val transaction = getTransaction(txId)
        if (transaction != null && predicate(transaction)) {
            return transaction
        }

        val channel = subscribe(transactionListeners, txId)
        return channel.consumeAsFlow().first {
            predicate(it)
        }
    }

    private fun <T, R> subscribe(container: MutableMap<T, MutableList<Channel<R>>>, key: T): Channel<R> {
        val channel = Channel<R>(CONFLATED)
        lock.withLock {
            container.getOrPut(key) {
                arrayListOf()
            }.add(channel)
        }
        channel.invokeOnClose {
            lock.withLock {
                val channels = container[key]
                    ?: return@withLock

                channels.remove(channel)
                if (channels.isEmpty()) {
                    container.remove(key)
                }
            }
        }
        return channel
    }
}
