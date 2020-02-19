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
    private val chainId: String,
    private val chain: SecurityInheritingChain
) {
    private val pollingPeriodSeconds = Configuration.getLong("securityInheriting.$chainId.pollingPeriodSeconds") ?: 20L
    private val neededConfirmations = Configuration.getInt("securityInheriting.$chainId.neededConfirmations") ?: 10

    private val lock = ReentrantLock()

    private lateinit var miner: Miner

    private val healthy = AtomicBoolean(false)
    private val connected = SettableFuture.create<Boolean>()

    private var bestBlockHeight: Int = -1

    private var pollSchedule: ScheduledFuture<*>? = null

    private val blockHeightListeners = HashMap<Int, AltchainBlockHeightListener>()
    private val blockListeners = HashMap<String, AltchainBlockListener>()
    private val transactionListeners = HashMap<String, AltchainTransactionListener>()

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

    fun stop() {
        pollSchedule?.cancel(false)
        pollSchedule = null
    }

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

                    handleBlockHeightListeners(bestBlockHeight)
                    handleBlockListeners()
                    handleTransactionListeners()

                    this.bestBlockHeight = bestBlockHeight
                }
            } else {
                val pinged = checkSuccess { chain.getBestBlockHeight() }
                if (pinged) {
                    healthy.set(true)
                    connected.set(true)
                }
            }
        } catch (e: Throwable) {
            logger.error(e) { "Error when polling SI Chain ($chainId)" }
        }
    }

    private fun handleBlockHeightListeners(bestBlockHeight: Int) {
        val triggeredBlocks = ArrayList<Int>()
        for ((height, listener) in blockHeightListeners) {
            if (height > bestBlockHeight) {
                continue
            }
            try {
                val block = chain.getBlock(listener.blockHeight)
                if (block == null) {
                    triggeredBlocks += height
                    listener.onError(IllegalStateException(
                        "Unable to find block with height $height while the best chain height is $bestBlockHeight!"
                    ))
                    continue
                }

                if (block.confirmations >= neededConfirmations) {
                    triggeredBlocks += height
                    listener.onComplete(block)
                }
            } catch (e: Exception) {
                logger.warn(e) { "Error when polling for block ${height}" }
            }
        }

        lock.withLock {
            triggeredBlocks.forEach {
                blockHeightListeners.remove(it)
            }
        }
    }

    private fun handleBlockListeners() {
        val triggeredBlocks = ArrayList<String>()
        for (listener in blockListeners.values) {
            val block = chain.getBlock(listener.blockHash)
                ?: continue

            try {
                if (block.confirmations >= neededConfirmations) {
                    triggeredBlocks += block.hash
                    listener.onComplete(block)
                } else if (block.confirmations < 0) {
                    triggeredBlocks += block.hash
                    listener.onError(AltchainBlockReorgException(block))
                }
            } catch (e: Exception) {
                logger.warn(e) { "Error when polling for block ${block.hash}" }
            }
        }

        lock.withLock {
            triggeredBlocks.forEach {
                blockListeners.remove(it)
            }
        }
    }

    private fun handleTransactionListeners() {
        val triggeredTransactions = ArrayList<String>()
        for (listener in transactionListeners.values) {
            val transaction = chain.getTransaction(listener.txId)
                ?: continue

            try {
                if (transaction.confirmations >= neededConfirmations) {
                    triggeredTransactions += transaction.txId
                    listener.onComplete(transaction)
                } else if (transaction.confirmations < 0) {
                    triggeredTransactions += transaction.txId
                    listener.onError(AltchainTransactionReorgException(transaction))
                }
            } catch (e: Exception) {
                logger.warn(e) { "Error when polling for transaction ${transaction.txId}" }
            }
        }

        lock.withLock {
            triggeredTransactions.forEach {
                transactionListeners.remove(it)
            }
        }
    }

    fun registerBlockHeightListener(blockListener: AltchainBlockHeightListener) = lock.withLock {
        blockHeightListeners[blockListener.blockHeight] = blockListener
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
