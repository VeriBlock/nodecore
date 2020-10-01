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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.veriblock.core.crypto.VBlakeHash
import org.veriblock.core.utilities.Configuration
import org.veriblock.core.utilities.Utility
import org.veriblock.core.utilities.createLogger
import org.veriblock.core.utilities.debugError
import org.veriblock.core.utilities.debugWarn
import org.veriblock.core.utilities.extensions.toHex
import org.veriblock.lite.NodeCoreLiteKit
import org.veriblock.lite.core.Context
import org.veriblock.lite.util.Threading
import org.veriblock.miners.pop.EventBus
import org.veriblock.miners.pop.service.MinerService
import org.veriblock.miners.pop.util.VTBDebugUtility
import org.veriblock.miners.pop.util.isOnSameNetwork
import org.veriblock.sdk.alt.ApmInstruction
import org.veriblock.sdk.alt.SecurityInheritingChain
import org.veriblock.sdk.alt.model.SecurityInheritingBlock
import org.veriblock.sdk.alt.model.SecurityInheritingTransaction
import org.veriblock.sdk.models.StateInfo
import org.veriblock.sdk.models.VeriBlockPublication
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
    private val chain: SecurityInheritingChain,
    private val nodeCoreLiteKit: NodeCoreLiteKit
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

        val coroutineScope = CoroutineScope(Threading.SI_POLL_THREAD.asCoroutineDispatcher())
        coroutineScope.launch {
            // Wait for nodeCore to be ready
            while (!nodeCoreLiteKit.network.isReady()) {
                delay(5_000L)
            }

            logger.info("Connecting to ${chain.name} daemon at ${chain.config.host}...")
            pollSchedule = launch {
                delay(5_000L)
                while (true) {
                    poll()
                    delay(pollingPeriodSeconds * 1000)
                }
            }

            // Wait for altchain to be ready
            while (!isReady()) {
                delay(1_000L)
            }
            // Start submitting context and VTBs
            launch { submitContext() }
            launch { submitVtbs() }
        }
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

    private suspend fun submitContext() = coroutineScope {
        logger.info("Starting continuous submission of VBK Context for ${chain.name}")
        val subscription = EventBus.newBestBlockChannel.openSubscription()
        for (newBlock in subscription) {
            try {
                val bestKnownBlockHash = VBlakeHash.wrap(chain.getBestKnownVbkBlockHash())
                val bestKnownBlock = nodeCoreLiteKit.blockChain.get(bestKnownBlockHash)
                if (bestKnownBlock == null) {
                    val networkBestKnownBlock = nodeCoreLiteKit.network.getBlock(bestKnownBlockHash)
                        ?: continue

                    val gap = newBlock.height - networkBestKnownBlock.height
                    logger.warn {
                        "Unable to find ${chain.name}'s best known VeriBlock block $bestKnownBlockHash in the local blockchain store." +
                            " There's a context gap of $gap blocks. Skipping VBK block context submission..."
                    }
                    subscription.cancel()
                    continue
                }

                val bestBlock = nodeCoreLiteKit.blockChain.getChainHead()
                    ?: continue

                if (bestKnownBlock.height == bestBlock.height) {
                    continue
                }

                val contextBlocks = generateSequence(bestBlock) {
                    nodeCoreLiteKit.blockChain.get(it.previousBlock)
                }.takeWhile {
                    it.hash != bestKnownBlockHash
                }.sortedBy {
                    it.height
                }.toList()

                val mempoolContext = chain.getPopMempool().vbkBlockHashes.map { it.toLowerCase() }
                val contextBlocksToSubmit = contextBlocks.filter {
                    it.hash.trimToPreviousBlockSize().toString().toLowerCase() !in mempoolContext
                }

                if (contextBlocksToSubmit.isEmpty()) {
                    continue
                }

                chain.submitContext(contextBlocksToSubmit)
                logger.info { "Submitted ${contextBlocksToSubmit.size} VBK context block(s) to ${chain.name}." }
            } catch (e: Exception) {
                logger.warn(e) { "Error while submitting context to ${chain.name}! Will try again later..." }
            }
        }
    }

    private suspend fun submitVtbs() = coroutineScope {
        logger.info("Starting continuous submission of VTBs for ${chain.name}")
        while (true) {
            try {
                val instruction = chain.getMiningInstruction()
                val vbkContextBlockHash = VBlakeHash.wrap(instruction.context.first())
                val vbkContextBlock = nodeCoreLiteKit.blockChain.get(vbkContextBlockHash)
                if (vbkContextBlock == null) {
                    val networkBestKnownBlock = nodeCoreLiteKit.network.getBlock(vbkContextBlockHash)
                    if (networkBestKnownBlock == null) {
                        // The altchain has knowledge of a block we don't even know, there's no need to send it further context.
                        // Let's try again after a while
                        delay(300_000L)
                        continue
                    }

                    val latestBlock = nodeCoreLiteKit.blockStore.getChainHead()
                    if (latestBlock == null) {
                        delay(20_000L)
                        continue
                    }

                    val gap = latestBlock.height - networkBestKnownBlock.height
                    logger.warn {
                        "Unable to find ${chain.name}'s best known VeriBlock block $vbkContextBlock in the local blockchain store." +
                            " There's a context gap of $gap blocks. Skipping VTB submission..."
                    }
                    return@coroutineScope
                }
                logger.info {
                    "${chain.name}'s known VBK context block: ${vbkContextBlock.hash} @ ${vbkContextBlock.height}." +
                    "Bitcoin context block: ${instruction.btcContext.first()}. Waiting for next VBK keystone..."
                }
                val newKeystone = EventBus.newBestBlockChannel.asFlow().filter {
                    it.height % 20 == 0 && it.height > vbkContextBlock.height
                }.first()

                logger.info { "Got keystone for ${chain.name}'s VTBs: ${newKeystone.hash} @ ${newKeystone.height}. Retrieving publication data..." }
                // Fetch and wait for veriblock publications (VTBs)
                val vtbs = nodeCoreLiteKit.network.getVeriBlockPublications(
                    newKeystone.hash.toString(),
                    instruction.context.first().toHex(),
                    instruction.btcContext.first().toHex()
                )
                // Validate the retrieved data
                verifyPublications(instruction, vtbs)

                logger.info { "VeriBlock Publication data for ${chain.name} retrieved and verified! Submitting to ${chain.name}'s daemon..." }

                // Submit them to the blockchain
                chain.submitVtbs(vtbs)
                logger.info { "Submitted ${vtbs.size} VTBs to ${chain.name}!" }
            } catch (e: Exception) {
                logger.warn(e) { "Error while submitting VTBs to ${chain.name}! Will try again later..." }
                delay(300_000L)
            }
        }
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
            atvBlocksById[atvId.toLowerCase()] = block
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
            val block = atvBlocksById[id.toLowerCase()]
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

private fun verifyPublications(
    miningInstruction: ApmInstruction,
    publications: List<VeriBlockPublication>
) {
    try {
        val btcContext = miningInstruction.btcContext
        // List<byte[]> vbkContext = context.getContext();

        // Check that the first VTB connects somewhere in the BTC context
        val firstPublication = publications[0]

        val serializedAltchainBTCContext = btcContext.joinToString("\n") { Utility.bytesToHex(it) }

        val serializedBTCHashesInPoPTransaction = VTBDebugUtility.serializeBitcoinBlockHashList(
            VTBDebugUtility.extractOrderedBtcBlocksFromPopTransaction(
                firstPublication.transaction
            )
        )

        if (!VTBDebugUtility.vtbConnectsToBtcContext(btcContext, firstPublication)) {
            logger.error {
                """Error: the first VeriBlock Publication with PoP TxID ${firstPublication.transaction.id} does not connect to the altchain context!
                               Altchain Bitcoin Context:
                               $serializedAltchainBTCContext
                               PoP Transaction Bitcoin blocks: $serializedBTCHashesInPoPTransaction""".trimIndent()
            }
        } else {
            logger.debug {
                """Success: the first VeriBlock Publication with PoP TxID ${firstPublication.transaction.id} connects to the altchain context!
                               Altchain Bitcoin Context:
                               $serializedAltchainBTCContext
                               PoP Transaction Bitcoin blocks: $serializedBTCHashesInPoPTransaction""".trimIndent()
            }
        }

        // Check that every VTB connects to the previous one
        for (i in 1 until publications.size) {
            val anchor = publications[i - 1]
            val toConnect = publications[i]

            val anchorBTCBlocks = VTBDebugUtility.extractOrderedBtcBlocksFromPopTransaction(anchor.transaction)
            val toConnectBTCBlocks = VTBDebugUtility.extractOrderedBtcBlocksFromPopTransaction(toConnect.transaction)

            val serializedAnchorBTCBlocks = VTBDebugUtility.serializeBitcoinBlockHashList(anchorBTCBlocks)
            val serializedToConnectBTCBlocks = VTBDebugUtility.serializeBitcoinBlockHashList(toConnectBTCBlocks)

            if (!VTBDebugUtility.doVtbsConnect(anchor, toConnect, (if (i > 1) publications.subList(0, i - 1) else emptyList()))) {
                logger.warn {
                    """Error: VTB at index $i does not connect to the previous VTB!
                                   VTB #${i - 1} BTC blocks:
                                   $serializedAnchorBTCBlocks
                                   VTB #$i BTC blocks:
                                   $serializedToConnectBTCBlocks""".trimIndent()
                }
            } else {
                logger.debug { "Success, VTB at index $i connects to VTB at index ${i - 1}!" }
            }
        }
    } catch (e: Exception) {
        logger.debugError(e) { "An error occurred checking VTB connection and continuity!" }
    }
}
