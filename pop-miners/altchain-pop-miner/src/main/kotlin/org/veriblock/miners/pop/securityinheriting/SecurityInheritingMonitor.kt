// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.securityinheriting

import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.CONFLATED
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.veriblock.core.utilities.Configuration
import org.veriblock.core.utilities.createLogger
import org.veriblock.core.utilities.debugWarn
import org.veriblock.lite.core.Context
import org.veriblock.lite.util.Threading
import org.veriblock.miners.pop.EventBus
import org.veriblock.miners.pop.service.MinerService
import org.veriblock.miners.pop.util.isOnSameNetwork
import org.veriblock.sdk.alt.SecurityInheritingChain
import org.veriblock.sdk.alt.model.SecurityInheritingBlock
import org.veriblock.sdk.alt.model.SecurityInheritingTransaction
import org.veriblock.sdk.models.AltPublication
import org.veriblock.sdk.models.StateInfo
import org.veriblock.sdk.models.getSynchronizedMessage
import org.veriblock.sdk.util.checkSuccess
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

private val logger = createLogger {}

class SecurityInheritingMonitor(
    val context: Context,
    configuration: Configuration,
    private val chainId: String,
    private val chain: SecurityInheritingChain
) {
    private val pollingPeriodSeconds = configuration.getLong(
        "securityInheriting.$chainId.pollingPeriodSeconds"
    ) ?: 10L

    private val lock = ReentrantLock()

    private lateinit var miner: MinerService

    private var firstPoll: Boolean = true

    private val ready = AtomicBoolean(false)
    private val accessible = AtomicBoolean(false)
    private val synchronized = AtomicBoolean(false)
    private val sameNetwork = AtomicBoolean(false)
    private val connected = SettableFuture.create<Boolean>()

    private var bestBlockHeight: Int = -1

    private var pollSchedule: Job? = null

    private val newBlockHeightBroadcastChannel = BroadcastChannel<Int>(CONFLATED)

    private val blockHeightListeners = ConcurrentHashMap<Int, MutableList<Channel<SecurityInheritingBlock>>>()
    private val transactionListeners = ConcurrentHashMap<String, MutableList<Channel<SecurityInheritingTransaction>>>()

    private val atvBlocksById = ConcurrentHashMap<String, SecurityInheritingBlock>()

    fun isReady(): Boolean =
        ready.get()

    fun isAccessible(): Boolean =
        accessible.get()

    fun isSynchronized(): Boolean =
        synchronized.get()

    fun isOnSameNetwork(): Boolean =
        sameNetwork.get()

    var latestBlockChainInfo: StateInfo = StateInfo()

    /**
     * Starts monitoring the corresponding chain with a polling schedule
     */
    fun start(miner: MinerService) {
        this.miner = miner
        pollSchedule = CoroutineScope(Threading.SI_POLL_THREAD.asCoroutineDispatcher()).launch {
            delay(5_000L)
            while (true) {
                poll()
                delay(pollingPeriodSeconds * 1000)
            }
        }

        logger.info("Connecting to SI Chain ($chainId) at ${chain.config.host}...")
    }

    /**
     * Stops the polling schedule
     */
    fun stop() {
        pollSchedule?.cancel()
        pollSchedule = null
    }

    /**
     * Checks for the best chain's block height. If it changed, it handles all registered listeners.
     * Automining is also triggered here.
     */
    private suspend fun poll() {
        try {
            // Verify if we can make a connection with the Altchain
            val pinged = checkSuccess { chain.getBestBlockHeight() }
            if (pinged) {
                // At this point the APM<->Altchain connection is fine
                latestBlockChainInfo = chain.getBlockChainInfo()

                if (!isAccessible()) {
                    accessible.set(true)
                    EventBus.altChainAccessibleEvent.trigger(chainId)
                }

                // Verify the Altchain configured network
                if (latestBlockChainInfo.networkVersion.isOnSameNetwork(context.networkParameters.name)) {
                    if (!isOnSameNetwork()) {
                        sameNetwork.set(true)
                        EventBus.altChainSameNetworkEvent.trigger(chainId)
                    }
                } else {
                    if (isOnSameNetwork() || firstPoll) {
                        sameNetwork.set(false)
                        EventBus.altChainSameNetworkEvent.trigger(chainId)
                        logger.warn { "The connected ${chain.name} chain (${latestBlockChainInfo.networkVersion}) & APM (${context.networkParameters.name}) are not running on the same configured network" }
                    }
                }

                connected.set(true)

                // Verify the altchain synchronization status
                if (latestBlockChainInfo.isSynchronized) {
                    if (!isSynchronized()) {
                        synchronized.set(true)
                        EventBus.altChainSynchronizedEvent.trigger(chainId)
                        logger.info { "The connected ${chain.name} chain is synchronized: ${latestBlockChainInfo.getSynchronizedMessage()}" }
                    }
                } else {
                    if (isSynchronized() || firstPoll) {
                        synchronized.set(false)
                        EventBus.altChainNotSynchronizedEvent.trigger(chainId)
                        logger.info { "The connected ${chain.name} chain is not synchronized: ${latestBlockChainInfo.getSynchronizedMessage()}" }
                    }
                }
            } else {
                // At this point the APM<->Altchain connection can't be established
                latestBlockChainInfo = StateInfo()
                if (isAccessible()) {
                    accessible.set(true)
                    EventBus.altChainNotAccessibleEvent.trigger(chainId)
                }
                if (isSynchronized()) {
                    synchronized.set(false)
                    EventBus.altChainNotSynchronizedEvent.trigger(chainId)
                }
                if (isOnSameNetwork()) {
                    sameNetwork.set(false)
                    EventBus.altChainNotSameNetworkEvent.trigger(chainId)
                }
            }

            if (isAccessible() && isSynchronized() && isOnSameNetwork()) {
                if (!isReady()) {
                    ready.set(true)
                    EventBus.altChainReadyEvent.trigger(chainId)
                }

                // At this point the APM<->Altchain conection is fine and the Altchain is synchronized so
                // APM can continue with its work
                val bestBlockHeight: Int = try {
                    chain.getBestBlockHeight()
                } catch (e: Exception) {
                    logger.debugWarn(e) { "Error while retrieving ${chain.name} tip height" }
                    if (isAccessible()) {
                        accessible.set(false)
                        EventBus.altChainNotAccessibleEvent.trigger(chainId)
                    }
                    return
                }

                if (bestBlockHeight != this.bestBlockHeight) {
                    logger.debug { "New chain head detected!" }
                    if (this.bestBlockHeight != -1 && chain.shouldAutoMine(bestBlockHeight)) {
                        miner.mine(chainId, bestBlockHeight)
                    }

                    this.bestBlockHeight = bestBlockHeight
                    newBlockHeightBroadcastChannel.offer(bestBlockHeight)

                    val block = getBlockAtHeight(bestBlockHeight)
                        ?: error("Unable to find block at tip height $bestBlockHeight")

                    handleNewBlock(block)

                    handleBlockHeightListeners()
                    handleTransactionListeners()
                }
            } else {
                if (isReady()) {
                    ready.set(false)
                    EventBus.altChainNotReadyEvent.trigger(chainId)
                }
            }
        } catch (t: Throwable) {
            logger.debugWarn(t) { "Error when polling SI Chain ($chainId)" }
        }
        firstPoll = false
    }

    private suspend fun handleNewBlock(block: SecurityInheritingBlock) {
        logger.debug {
            val publicationsString = if (block.veriBlockPublicationIds.isEmpty()) {
                "with no VBK publications"
            } else {
                "with ${block.veriBlockPublicationIds.size} publications: ${block.veriBlockPublicationIds.joinToString()}"
            }
            "Found new ${chain.name} block: ${block.hash} @ ${block.height} $publicationsString"
        }

        for (atvId in block.veriBlockPublicationIds) {
            atvBlocksById[atvId] = block
        }
    }

    private suspend fun getBlockAtHeight(height: Int): SecurityInheritingBlock? {
        // Ignore if we didn't still reach the registered height yet
        if (height > bestBlockHeight) {
            return null
        }

        return try {
            // Retrieve block from SI chain
            chain.getBlock(height)
        } catch (e: Exception) {
            logger.debugWarn(e) { "Error when retrieving ${chain.name} block $height" }
            null
        }
        // The best block should never be null if the chain's integrity is not compromised
            ?: throw IllegalStateException("Unable to find block with height $height while the best chain height is $bestBlockHeight!")
    }

    private suspend fun getTransaction(txId: String): SecurityInheritingTransaction? = try {
        // Retrieve block from SI chain
        chain.getTransaction(txId)
    } catch (e: Exception) {
        logger.debugWarn(e) { "Error when retrieving ${chain.name} transaction $txId" }
        null
    }

    private suspend fun handleBlockHeightListeners() {
        for ((height, listeners) in blockHeightListeners) {
            val block = getBlockAtHeight(height)
            if (block != null) {
                for (listener in listeners) {
                    listener.offer(block)
                }
            }
        }
    }

    private suspend fun handleTransactionListeners() {
        for ((txId, listeners) in transactionListeners) {
            val transaction = getTransaction(txId)
            if (transaction != null) {
                for (listener in listeners) {
                    listener.offer(transaction)
                }
            }
        }
    }

    suspend fun getBlockAtHeight(height: Int, predicate: (SecurityInheritingBlock) -> Boolean = { true }): SecurityInheritingBlock {
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

    suspend fun getTransaction(txId: String, predicate: (SecurityInheritingTransaction) -> Boolean = { true }): SecurityInheritingTransaction {
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

    suspend fun confirmAtv(id: String): SecurityInheritingBlock {
        while (true) {
            val block = atvBlocksById[id]
            if (block == null) {
                delay(20_000L)
                continue
            }
            return block
        }
    }

    private fun <T, R> subscribe(container: MutableMap<T, MutableList<Channel<R>>>, key: T): Channel<R> {
        val channel = Channel<R>(CONFLATED)
        lock.withLock {
            container.getOrPut(key) {
                CopyOnWriteArrayList()
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
