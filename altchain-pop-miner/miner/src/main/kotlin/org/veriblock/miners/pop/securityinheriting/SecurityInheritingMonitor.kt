// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.securityinheriting

import com.google.common.util.concurrent.SettableFuture
import org.veriblock.core.utilities.Configuration
import org.veriblock.core.utilities.createLogger
import org.veriblock.lite.util.Threading
import org.veriblock.miners.pop.Miner
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
    private val neededConfirmations = configuration.getInt("securityInheriting.$chainId.neededConfirmations") ?: 10

    private val lock = ReentrantLock()

    private lateinit var miner: Miner

    private val healthy = AtomicBoolean(false)
    private val connected = SettableFuture.create<Boolean>()

    private var bestBlockHeight: Int = -1

    private var pollSchedule: ScheduledFuture<*>? = null

    private val blockHeightListeners = HashMap<Int, AltchainBlockHeightListener>()
    private val blockListeners = HashMap<String, AltchainBlockListener>()
    private val transactionListeners = HashMap<String, AltchainTransactionListener>()

    /**
     * Starts monitoring the corresponding chain with a polling schedule
     */
    fun start(miner: Miner) {
        this.miner = miner
        pollSchedule = Threading.SI_POLL_THREAD.scheduleWithFixedDelay({
            this.poll()
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
                    logger.error("$chainId Chain Error", e)
                    healthy.set(false)
                    return
                }

                if (bestBlockHeight != this.bestBlockHeight) {
                    logger.debug { "New chain head detected!" }
                    if (this.bestBlockHeight != -1 && chain.shouldAutoMine(bestBlockHeight)) {
                        miner.mine(chainId, bestBlockHeight)
                    }

                    this.bestBlockHeight = bestBlockHeight

                    handleBlockHeightListeners()
                    handleBlockListeners()
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
            logger.error(t) { "Error when polling SI Chain ($chainId)" }
        }
    }

    /**
     * Checks for the height listeners. If any of them is registered for a height not higher than the best,
     * it checks for the best block in the registered height and its confirmations.
     */
    private fun handleBlockHeightListeners() = lock.withLock {
        val triggeredBlocks = ArrayList<Int>()
        for (listener in blockHeightListeners.values) {
            val handled = handleBlockHeightListener(listener)
            if (handled) {
                triggeredBlocks += listener.blockHeight
            }
        }

        // Remove triggered listeners
        triggeredBlocks.forEach {
            blockHeightListeners.remove(it)
        }
    }

    private fun handleBlockHeightListener(listener: AltchainBlockHeightListener): Boolean {
        // Ignore if we didn't still reach the registered height yet
        if (listener.blockHeight > bestBlockHeight) {
            return false
        }
        // Retrieve block from SI chain
        val block = try {
            chain.getBlock(listener.blockHeight)
        } catch (e: Exception) {
            logger.warn(e) { "Error when polling for block ${listener.blockHeight}" }
            return false
        }
        if (block == null) {
            // The best block should never be null if the chain's integrity is not compromised
            try {
                listener.onError(
                    IllegalStateException("Unable to find block with height ${listener.blockHeight} while the best chain height is $bestBlockHeight!")
                )
            } catch (t: Throwable) {
                logger.warn(t) { t.message }
            }
            return true
        }

        // Check for the needed confirmations and trigger the listener if the block has enough of them
        if (block.confirmations >= neededConfirmations) {
            try {
                listener.onComplete(block)
            } catch (e: Exception) {
                logger.warn(e) { e.message }
                return false
            }
            return true
        }
        return false
    }

    private fun handleBlockListeners() = lock.withLock {
        val triggeredBlocks = ArrayList<String>()
        for (listener in blockListeners.values) {
            // Ignore if the block does not exist in the chain yet
            val block = chain.getBlock(listener.blockHash)
                ?: continue

            try {
                // Check for the needed confirmations and trigger the listener if the block has enough of them
                if (block.confirmations >= neededConfirmations) {
                    triggeredBlocks += block.hash
                    listener.onComplete(block)
                }
                // Also check if a reorg has taken place
                else if (block.confirmations < 0) {
                    triggeredBlocks += block.hash
                    listener.onError(AltchainBlockReorgException(block))
                }
            } catch (e: Exception) {
                logger.warn(e) { "Error when polling for block ${block.hash}" }
            }
        }

        // Remove triggered listeners
        triggeredBlocks.forEach {
            blockListeners.remove(it)
        }
    }

    private fun handleTransactionListeners() = lock.withLock {
        val triggeredTransactions = ArrayList<String>()
        for (listener in transactionListeners.values) {
            // Ignore if the transaction does not exist in the chain yet
            val transaction = chain.getTransaction(listener.txId)
                ?: continue

            try {
                // Check for the needed confirmations and trigger the listener if the transaction has enough of them
                if (transaction.confirmations >= neededConfirmations) {
                    triggeredTransactions += transaction.txId
                    listener.onComplete(transaction)
                }
                // Also check if a reorg has taken place
                else if (transaction.confirmations < 0) {
                    triggeredTransactions += transaction.txId
                    listener.onError(AltchainTransactionReorgException(transaction))
                }
            } catch (e: Exception) {
                logger.warn(e) { "Error when polling for transaction ${transaction.txId}" }
            }
        }

        // Remove triggered listeners
        triggeredTransactions.forEach {
            transactionListeners.remove(it)
        }
    }

    fun registerBlockHeightListener(blockListener: AltchainBlockHeightListener) {
        val handled = handleBlockHeightListener(blockListener)
        if (!handled) {
            lock.withLock {
                blockHeightListeners[blockListener.blockHeight] = blockListener
            }
        }
    }

    fun registerBlockListener(blockListener: AltchainBlockListener) = lock.withLock {
        blockListeners[blockListener.blockHash] = blockListener
    }

    fun registerTransactionListener(transactionListener: AltchainTransactionListener) = lock.withLock {
        transactionListeners[transactionListener.txId] = transactionListener
    }
}

class AltchainBlockHeightListener(
    val blockHeight: Int,
    val onComplete: (SecurityInheritingBlock) -> Unit,
    val onError: (Throwable) -> Unit
)

class AltchainBlockListener(
    val blockHash: String,
    val onComplete: (SecurityInheritingBlock) -> Unit,
    val onError: (Throwable) -> Unit
)

class AltchainTransactionListener(
    val txId: String,
    val onComplete: (SecurityInheritingTransaction) -> Unit,
    val onError: (Throwable) -> Unit
)

class AltchainBlockReorgException(
    val block: SecurityInheritingBlock
) : IllegalStateException("There was a reorg leaving block ${block.hash} out of the main chain!")

class AltchainTransactionReorgException(
    val transaction: SecurityInheritingTransaction
) : IllegalStateException("There was a reorg leaving transaction ${transaction.txId} out of the main chain!")
