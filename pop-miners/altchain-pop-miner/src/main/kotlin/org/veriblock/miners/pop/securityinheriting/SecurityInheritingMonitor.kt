// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.securityinheriting

import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.CONFLATED
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.veriblock.core.MineException
import org.veriblock.core.crypto.asVbkHash
import org.veriblock.core.utilities.Configuration
import org.veriblock.core.utilities.Utility
import org.veriblock.core.utilities.createLogger
import org.veriblock.core.utilities.debugError
import org.veriblock.core.utilities.debugWarn
import org.veriblock.core.utilities.extensions.toHex
import org.veriblock.miners.pop.core.ApmContext
import org.veriblock.miners.pop.util.Threading
import org.veriblock.miners.pop.EventBus
import org.veriblock.miners.pop.service.AltchainPopMinerService
import org.veriblock.miners.pop.util.VTBDebugUtility
import org.veriblock.sdk.alt.ApmInstruction
import org.veriblock.sdk.alt.SecurityInheritingChain
import org.veriblock.sdk.alt.model.Atv
import org.veriblock.sdk.alt.model.SecurityInheritingBlock
import org.veriblock.sdk.alt.model.SecurityInheritingTransaction
import org.veriblock.sdk.models.StateInfo
import org.veriblock.sdk.models.VeriBlockPublication
import org.veriblock.sdk.models.getSynchronizedMessage
import org.veriblock.sdk.util.checkSuccess
import org.veriblock.spv.util.SpvEventBus
import org.veriblock.spv.util.invokeOnFailure
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import org.veriblock.miners.pop.util.CheckResult
import org.veriblock.sdk.models.VeriBlockBlock
import java.util.*
import kotlin.concurrent.withLock

private val logger = createLogger {}

class SecurityInheritingMonitor(
    val context: ApmContext,
    configuration: Configuration,
    private val chainId: String,
    private val chain: SecurityInheritingChain
) {
    private val pollingPeriodSeconds = configuration.getLong(
        "securityInheriting.$chainId.pollingPeriodSeconds"
    ) ?: 10L

    private val lock = ReentrantLock()

    private lateinit var miner: AltchainPopMinerService

    private var firstPoll: Boolean = true

    private val popIsActive = AtomicBoolean(false)
    private val ready = AtomicBoolean(false)
    private val accessible = AtomicBoolean(false)
    private val synchronized = AtomicBoolean(false)
    private val sameNetwork = AtomicBoolean(false)
    private val connected = SettableFuture.create<Boolean>()

    private val bestBlockHeight = MutableStateFlow(-1)

    private var pollSchedule: Job? = null

    private val blockHeightListeners = ConcurrentHashMap<Int, MutableList<Channel<SecurityInheritingBlock>>>()
    private val transactionListeners = ConcurrentHashMap<String, MutableList<Channel<SecurityInheritingTransaction>>>()

    fun isPopActive(): Boolean =
        popIsActive.get()

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
    fun start(miner: AltchainPopMinerService) {
        this.miner = miner

        val coroutineScope = CoroutineScope(Threading.SI_MONITOR_POOL.asCoroutineDispatcher())
        coroutineScope.launch {
            // Wait for nodeCore to be ready
            while (!miner.network.isReady()) {
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

            while(!isPopActive()) {
                delay(10_000L)
            }

            // Start submitting context and VTBs
            launch {
                autoSubmitContext()
            }.invokeOnFailure {
                logger.error(it) { "${chain.name} context submission task has failed" }
            }
            launch {
                autoSubmitVtbs()
            }.invokeOnFailure {
                logger.error(it) { "${chain.name} VTB submission task has failed" }
            }
            launch {
                autoHandleContextGap()
            }.invokeOnFailure {
                logger.error(it) { "${chain.name} VTB context gap handling task has failed" }
            }
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
    private suspend fun poll() = coroutineScope {
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
                // TODO: check if APM & altchain are on same VBK network (beware of comparisons "test" vs "testnet")
                sameNetwork.set(true)
                // TODO: check if POP is active and exit if not.
                popIsActive.set(true)

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

                // At this point the APM<->Altchain connection is fine and the Altchain is synchronized so
                // APM can continue with its work
                val bestBlockHeight: Int = try {
                    chain.getBestBlockHeight()
                } catch (e: Exception) {
                    logger.debugWarn(e) { "Error while retrieving ${chain.name} tip height" }
                    if (isAccessible()) {
                        accessible.set(false)
                        EventBus.altChainNotAccessibleEvent.trigger(chainId)
                    }
                    return@coroutineScope
                }

                // Should not happen, some altchain blocks appear faster than we update the sync information
                if (latestBlockChainInfo.localBlockchainHeight < bestBlockHeight) {
                    latestBlockChainInfo = chain.getBlockChainInfo()
                    logger.debug { "${chain.name} blocks are appearing too fast, forcing the block chain sync info update..." }
                }

                if (bestBlockHeight != this@SecurityInheritingMonitor.bestBlockHeight.value) {
                    logger.debug { "New chain head detected @${bestBlockHeight}" }
                    if (this@SecurityInheritingMonitor.bestBlockHeight.value != -1) {
                        val isMinerReady = miner.checkReadyConditions()
                        ((this@SecurityInheritingMonitor.bestBlockHeight.value + 1)..bestBlockHeight).forEach { blockHeight ->
                            if (chain.shouldAutoMine(blockHeight)) {
                                if (isMinerReady is CheckResult.Success) {
                                    logger.debug { "Auto mining block @$blockHeight" }
                                    try {
                                        miner.mine(chainId, blockHeight)
                                    } catch (e: MineException) {
                                        logger.error { "Failed to auto mine the block $blockHeight: ${e.message}" }
                                    }
                                } else {
                                   logger.info { "Unable to auto mine the block @$blockHeight: ${(isMinerReady as CheckResult.Failure).error}" }
                                }
                            }
                        }
                    }

                    this@SecurityInheritingMonitor.bestBlockHeight.value = bestBlockHeight

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

    private suspend fun autoSubmitContext() = coroutineScope {
        logger.info("Starting continuous submission of VBK context for ${chain.name}")
        SpvEventBus.newBlockFlow.asSharedFlow().collect { newBlock ->
            try {
                submitContextBlock(newBlock)
            } catch (e: Exception) {
                logger.debugWarn(e) { "Error while submitting context to ${chain.name}! Will try again later..." }
                e.printStackTrace()
            }
        }
    }

    private suspend fun autoSubmitVtbs() = coroutineScope {
        logger.info("Starting continuous submission of VTBs for ${chain.name}")
        while (true) {
            try {
                submitVtbs()
            } catch (e: Exception) {
                logger.debugWarn(e) { "Error while submitting VTBs to ${chain.name}! Will try again later..." }
                delay(300_000L)
            } catch (t: Throwable) {
                logger.error(t) { "Error while submitting VTBs to ${chain.name}! Will try again later..." }
                delay(300_000L)
            }
        }
    }

    private suspend fun autoHandleContextGap() = coroutineScope {
        logger.info("Starting continuous submission of VTBs for ${chain.name}")
        while (true) {
            try {
                handleContextGap()
            } catch (e: Exception) {
                logger.debugWarn(e) { "Error handling context gap for ${chain.name}! Will try again later..." }
            } catch (t: Throwable) {
                logger.error(t) { "Error handling context gap for ${chain.name}! Will try again later..." }
            }
            delay(1200_000L)
        }
    }

    private suspend fun getLastCommonVbkBlock(): VeriBlockBlock? {
        val bestHash = chain.getBestKnownVbkBlockHash()
        var cursor = chain.getVbkBlock(bestHash)
            ?: return null
        while (!miner.gateway.isOnActiveChain(cursor.hash)) {
            logger.info { "${chain.name} block ${cursor.hash} is not on the active chain... Getting previous block." }
            cursor = chain.getVbkBlock(cursor.previousBlock.toString())
                ?: return null
        }

        return cursor
    }

    suspend fun submitContextBlock(newBlock: VeriBlockBlock) {
        if (!miner.gateway.isOnActiveChain(newBlock.hash)) {
            logger.debug { "New block $newBlock is not on the main chain, skipping..." }
            return
        }
        logger.debug { "New block $newBlock is on the main chain" }

        val bestKnownBlock = getLastCommonVbkBlock()
        if(bestKnownBlock == null) {
            logger.error { "${chain.name} can not find last common VBK block. Misconfiguration?" }
            return
        }
        logger.debug {"${chain.name} last common VBK block: $bestKnownBlock"}

        if (bestKnownBlock.hash == newBlock.hash) {
            // return early if altchain already knows newBlock
            return
        }

        val contextBlocks = generateSequence(newBlock) {
            miner.gateway.getBlock(it.previousBlock)
        }.takeWhile {
            it.height >= bestKnownBlock.height
        }.sortedBy {
            it.height
        }.toList()

        logger.debug {"Context blocks: ${contextBlocks.size}. First: ${contextBlocks.first()}. Last: ${contextBlocks.last()}"}

        val mempoolContext = chain.getPopMempool().vbkBlockHashes.map { it.lowercase() }
        val successfulSubmissions = contextBlocks.asFlow()
            .filter { it.hash.trimToPreviousBlockSize().toString().lowercase() !in mempoolContext }
            .map { contextBlock ->
                val result = chain.submitPopVbk(contextBlock)
                if (!result.accepted) {
                    logger.debug { "VBK context block $contextBlock was not accepted in ${chain.name}'s PoP mempool." }
                }
                result
            }
            .count { it.accepted }
        if (successfulSubmissions > 0) {
            logger.debug { "Successfully submitted $successfulSubmissions VBK context block(s) to ${chain.name}." }
        }
        return
    }

    suspend fun submitVtbs() {
        val instruction = chain.getMiningInstructionByHeight()
        val vbkContextBlockHash = instruction.context.first().asVbkHash()
        val vbkContextBlock = miner.gateway.getBlock(vbkContextBlockHash)
        while (vbkContextBlock == null) {
            // The altchain has knowledge of a block we don't even know. Let's try again after a while
            logger.info { "${chain.name}'s last known block $vbkContextBlockHash is not known by APM. Trying to submit VTBs later..." }
            delay(20_000L)
            return submitVtbs()
        }
        logger.info {
            "${chain.name}'s known VBK context block: ${vbkContextBlock.hash} @ ${vbkContextBlock.height}." +
                " Bitcoin context block: ${instruction.btcContext.first().toHex()}. Waiting for next VBK keystone..."
        }

        // Wait for the next keystone to be mined
        val keystone = SpvEventBus.newBlockFlow.filter {
            it.height % 20 == 0 && it.height > vbkContextBlock.height
        }.first()

        logger.info { "Got keystone for ${chain.name}'s VTBs: ${keystone.hash} @ ${keystone.height}. Retrieving publication data..." }
        // Fetch and wait for veriblock publications (VTBs)
        val vtbs = miner.network.getVeriBlockPublications(
            keystone.hash.toString(),
            instruction.context.first().toHex(),
            instruction.btcContext.first().toHex()
        )
        // Validate the retrieved data
        verifyPublications(instruction, vtbs)

        logger.info { "VeriBlock Publication data for ${chain.name} retrieved and verified! Submitting to ${chain.name}'s daemon..." }

        // Submit them to the blockchain
        vtbs.forEach {
            chain.submitPopVtb(it)
        }
        val successfulSubmissions = vtbs.asFlow()
            .map { vtb ->
                val result = chain.submitPopVtb(vtb)
                if (!result.accepted) {
                    logger.debug { "VTB with ${vtb.getFirstBitcoinBlock()} was not accepted in ${chain.name}'s PoP mempool." }
                }
                result
            }
            .count { it.accepted }
        if (successfulSubmissions > 0) {
            logger.info { "Successfully submitted $successfulSubmissions VTBs to ${chain.name}!" }
        } else {
            logger.info { "Failed to submit VTBs to ${chain.name}" }
        }
    }

    suspend fun handleContextGap() {
        val missingBtcBlockHashes = try {
            chain.getMissingBtcBlockHashes()
        } catch (e: Exception) {
            logger.debugWarn(e) { "Unable to retrieve ${chain.name}'s missing BTC block hashes" }
            return
        }

        if (missingBtcBlockHashes.isEmpty()) {
            return
        }

        logger.info { "The chain ${chain.name} has a context gap of ${missingBtcBlockHashes.size} BTC blocks! Retrieving corresponding publication data..." }
        // Fetch and wait for veriblock publications (VTBs)
        val vtbs = miner.gateway.getVtbsForBtcBlocks(missingBtcBlockHashes)

        logger.info { "${vtbs.size} VTBs found! Submitting to ${chain.name}'s daemon..." }

        // Submit them to the blockchain
        vtbs.forEach {
            chain.submitPopVtb(it)
        }
        val successfulSubmissions = vtbs.asFlow()
            .map { vtb ->
                val result = chain.submitPopVtb(vtb)
                if (!result.accepted) {
                    logger.debug { "VTB with ${vtb.getFirstBitcoinBlock()} was not accepted in ${chain.name}'s PoP mempool." }
                }
                result
            }
            .count { it.accepted }
        if (successfulSubmissions > 0) {
            logger.info { "Successfully submitted $successfulSubmissions VTBs to ${chain.name}!" }
        } else {
            logger.info { "Failed to submit VTBs to ${chain.name}" }
        }
    }

    private fun handleNewBlock(block: SecurityInheritingBlock) {
        logger.debug {
            val publicationsString = if (block.veriBlockPublicationIds.isEmpty()) {
                "with no VBK publications"
            } else {
                "with ${block.veriBlockPublicationIds.size} publications: ${block.veriBlockPublicationIds.joinToString()}"
            }
            "Found new ${chain.name} block: ${block.hash} @ ${block.height} $publicationsString"
        }
    }

    private suspend fun getBlockAtHeight(height: Int): SecurityInheritingBlock? {
        // Ignore if we didn't still reach the registered height yet
        if (height > bestBlockHeight.value) {
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
            ?: throw IllegalStateException("Unable to find block with height $height while the best chain height is ${bestBlockHeight.value}!")
    }

    private suspend fun getTransaction(txId: String): SecurityInheritingTransaction? = try {
        // Retrieve block from SI chain
        chain.getTransaction(txId, null)
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

    suspend fun getAtv(id: String, delay: Long = 10_000L, predicate: suspend (atv: Atv) -> Boolean): Atv? {
        var atv: Atv?
        do {
            atv = try {
                chain.getAtv(id)
            } catch (e: Exception) {
                logger.debug { "Can not get ATV=${id} from ${chain.name}: ${e.message}" }
                null
            }

            delay(delay)
        } while (atv == null || !predicate(atv))

        return atv
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
            logger.info {
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
                logger.info {
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
