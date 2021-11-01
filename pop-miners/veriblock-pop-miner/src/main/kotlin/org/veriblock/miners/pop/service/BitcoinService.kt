// VeriBlock PoP Miner
// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.miners.pop.service

import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.ThreadFactoryBuilder
import kotlinx.coroutines.*
import kotlinx.coroutines.guava.asDeferred
import kotlinx.coroutines.sync.Mutex
import org.apache.commons.lang3.tuple.Pair
import org.bitcoinj.core.*
import org.bitcoinj.core.listeners.BlocksDownloadedEventListener
import org.bitcoinj.core.listeners.DownloadProgressTracker
import org.bitcoinj.kits.WalletAppKit
import org.bitcoinj.script.Script
import org.bitcoinj.script.ScriptBuilder
import org.bitcoinj.script.ScriptOpCodes
import org.bitcoinj.store.BlockStore
import org.bitcoinj.store.BlockStoreException
import org.bitcoinj.store.SPVBlockStore
import org.bitcoinj.wallet.DeterministicSeed
import org.bitcoinj.wallet.SendRequest
import org.bitcoinj.wallet.Wallet
import org.veriblock.core.launchWithFixedDelay
import org.veriblock.core.utilities.createLogger
import org.veriblock.miners.pop.Constants
import org.veriblock.miners.pop.EventBus
import org.veriblock.miners.pop.VpmConfig
import org.veriblock.miners.pop.common.BitcoinNetwork
import org.veriblock.miners.pop.common.formatBtcFriendlyString
import org.veriblock.miners.pop.model.ApplicationExceptions.CorruptSPVChain
import org.veriblock.miners.pop.model.ApplicationExceptions.DuplicateTransactionException
import org.veriblock.miners.pop.model.ApplicationExceptions.ExceededMaxTransactionFee
import java.io.File
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import org.veriblock.core.createSingleThreadExecutor
import kotlin.math.roundToLong

val SEGWIT_TX_FEE_RATE = 0.713043478

private val EMPTY_OBJECT = Any()

private val logger = createLogger {}

@Suppress("UnstableApiUsage")
class BitcoinService(
    config: VpmConfig
) : BlocksDownloadedEventListener {

    private val executor = createSingleThreadExecutor("bitcoin-service-thread")
    private val coroutineScope = CoroutineScope(executor.asCoroutineDispatcher())

    private val configuration = config.bitcoin
    val context = configuration.context

    var maxFee = configuration.maxFee
    var feePerKb = configuration.feePerKB

    private val blockCache = AwaitableCache<String, FilteredBlock>(maxSize = 150)

    private var kit: WalletAppKit

    private lateinit var blockChain: BlockChain
    private lateinit var blockStore: BlockStore
    private lateinit var wallet: Wallet
    private lateinit var peerGroup: PeerGroup

    private val serializer: BitcoinSerializer
    private val txLock = Mutex()
    private val txBroadcastAudit = object : LinkedHashMap<String, Any>() {
        override fun removeEldestEntry(eldest: Map.Entry<String, Any>): Boolean {
            return size > 50
        }
    }

    private val bitcoinNetwork = configuration.network

    var blockchainDownloadBlocksToGo = 0
    var blockchainDownloadPercent = 0

    private val blockChainDownloaded = AtomicBoolean(false)
    private val serviceReady = AtomicBoolean(false)
    private val funded = AtomicBoolean(false)

    private var receiveAddress: String? = null
    private var changeAddress: Address? = null

    private val scheduledExecutorService: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor(
        ThreadFactoryBuilder().setNameFormat("bitcoin-context").build()
    )

    val contextCoroutineScope = CoroutineScope(scheduledExecutorService.asCoroutineDispatcher()).apply {
        launch { Context.propagate(config.bitcoin.context) }
    }

    init {
        logger.info("Using Bitcoin {} network", bitcoinNetwork.toString())
        serializer = BitcoinSerializer(context.params, true)
        kit = createWalletAppKit(context, bitcoinNetwork.getFilePrefix(), null)
        EventBus.popMiningOperationFinishedEvent.register(this) {
            contextCoroutineScope.launch {
                it.endorsementTransaction?.confidence?.removeEventListener(it.transactionListener)
            }
        }
        logger.info("BitcoinService constructor finished")
    }

    private fun setServiceReady(value: Boolean) {
        if (value == isServiceReady()) {
            return
        }
        serviceReady.set(value)
        if (isServiceReady()) {
            EventBus.bitcoinServiceReadyEvent.trigger()
        } else {
            EventBus.bitcoinServiceNotReadyEvent.trigger()
        }
    }

    private fun setBlockChainDownloaded(value: Boolean) {
        if (value == isBlockchainDownloaded()) {
            return
        }
        blockChainDownloaded.set(value)
        if (isBlockchainDownloaded()) {
            EventBus.blockchainDownloadedEvent.trigger()
        } else {
            EventBus.blockchainNotDownloadedEvent.trigger()
        }
    }

    // Used to track changes on the wallet balance
    private var latestBalance: Coin = Coin.ZERO

    private fun createWalletAppKit(
        context: Context,
        filePrefix: String,
        seed: DeterministicSeed?
    ): WalletAppKit {
        setBlockChainDownloaded(false)
        var kit: WalletAppKit = object : WalletAppKit(
            context, Script.ScriptType.P2WPKH, null, File("."), filePrefix
        ) {
            override fun provideBlockStore(file: File): BlockStore {
                return SPVBlockStore(context.params, file, configuration.blockStoreCapacity, true)
            }

            override fun onSetupCompleted() {
                super.onSetupCompleted()

                blockStore = store()

                wallet = wallet().apply {
                    isAcceptRiskyTransactions = true
                    addCoinsReceivedEventListener { _, _: Transaction, prevBalance: Coin, newBalance: Coin ->
                        logger.info {
                            val delta = newBalance.minus(prevBalance)
                            val action = if (delta.isNegative) {
                                "Spent ${delta.negate().formatBtcFriendlyString()}"
                            } else {
                                "Received ${delta.formatBtcFriendlyString()}"
                            }
                            "New pending BTC transaction: $action. New pending balance: ${newBalance.formatBtcFriendlyString()}"
                        }
                    }

                    // Verify the balance at any wallet-change
                    addChangeEventListener {
                        verifyBalance()
                    }
                }

                blockChain = chain()

                peerGroup = peerGroup().apply {
                    useLocalhostPeerWhenPossible = configuration.useLocalhostPeer
                    minRequiredProtocolVersion = configuration.minimalPeerProtocolVersion.bitcoinProtocolVersion
                    maxConnections = configuration.maxPeerConnections
                    setPeerDiscoveryTimeoutMillis(configuration.peerDiscoveryTimeoutMillis.toLong())
                    setDownloadTxDependencies(configuration.peerDownloadTxDependencyDepth)
                    setRequiredServices(configuration.requiredPeerServices)
                    minBroadcastConnections = configuration.minPeerBroadcastConnections
                    maxPeersToDiscoverCount = configuration.maxPeersToDiscoverCount
                    pingIntervalMsec = configuration.peerPingIntervalMillis
                    setStallThreshold(configuration.downloadBlockchainPeriodSeconds, configuration.downloadBlockchainBytesPerSecond)
                    addBlocksDownloadedEventListener(this@BitcoinService)
                }

                val earliestKeyCreationTime = wallet.earliestKeyCreationTime
                if (earliestKeyCreationTime > 0) {
                    val createdDaysAgo = (Instant.now().epochSecond - earliestKeyCreationTime) / 86400
                    if (createdDaysAgo >= 30) {
                        logger.info { "This wallet was created $createdDaysAgo day(s) ago, we recommend you to create a new wallet and move your funds there, this will drastically decrease the needed time to synchronize the wallet" }
                    }
                }

                coroutineScope.launchWithFixedDelay(10_000, 10_000) {
                    verifyBalance(true)
                }

                setServiceReady(true)
            }
        }
        kit.setBlockingStartup(false)
        kit.setDownloadListener(object : DownloadProgressTracker() {
            override fun doneDownload() {
                if (!isBlockchainDownloaded()) {
                    setBlockChainDownloaded(true)

                    blockchainDownloadPercent = 100
                    blockchainDownloadBlocksToGo = 0

                    verifyBalance(true)
                }
            }

            override fun progress(pct: Double, blocksSoFar: Int, date: Date) {
                super.progress(pct, blocksSoFar, date)

                blockchainDownloadPercent = pct.toInt()
                blockchainDownloadBlocksToGo = blocksSoFar

                // Don't report progress at the end, doneDownload() will handle that
                if (blocksSoFar < 10) {
                    return
                }
                if (pct.toInt() % 5 == 0) {
                    logger.info("Blockchain downloading: {}%", pct.toInt())
                }
                if (pct > 95.0 && blocksSoFar % 10 == 0) {
                    logger.info("Blockchain downloading: {} blocks to go", blocksSoFar)
                }
            }
        })
        if (seed != null) {
            kit = kit.restoreWalletFromSeed(seed)
        }
        return kit
    }

    fun verifyBalance(ignoreCondition: Boolean = false) {
        try {
            val balance = wallet.getBalance(Wallet.BalanceType.AVAILABLE_SPENDABLE)
            if (balance != latestBalance) {
                if (balance.isLessThan(getMaximumTransactionFee())) {
                    if (ignoreCondition || isSufficientlyFunded()) {
                        funded.set(false)
                        EventBus.insufficientFundsEvent.trigger(balance)
                    }
                } else {
                    if (ignoreCondition || !isSufficientlyFunded()) {
                        funded.set(true)
                        EventBus.sufficientFundsEvent.trigger(balance)
                    }
                }
                EventBus.balanceChangedEvent.trigger(balance)
                latestBalance = balance
            }
        } catch (e: Exception) {
            logger.error(e) { "Unable to verify the balance" }
        }
    }

    fun initialize() {
        if (bitcoinNetwork === BitcoinNetwork.RegTest) {
            kit.connectToLocalHost()
        }
        kit.startAsync()
        try {
            kit.awaitRunning()
        } catch (e: IllegalStateException) {
            val spvchain = File(
                bitcoinNetwork.getFilePrefix() + ".spvchain"
            )
            val successfulDelete = spvchain.delete()
            logger.error(
                "An exception has occurred while waiting for the wallet kit to begin running!", e
            )
            if (successfulDelete) {
                logger.info("Deleted corrupt SPV chain...")
                throw CorruptSPVChain("A corrupt SPV chain has been detected and deleted. Please restart the PoP miner, and run 'resetwallet'!")
            } else {
                logger.info(
                    "Unable to delete corrupt SPV chain, please delete " + spvchain.absolutePath + "!"
                )
                throw CorruptSPVChain(
                    "A corrupt SPV chain has been detected but could not be deleted. Please delete ${spvchain.absolutePath}," +
                        " restart the PoP miner, and run 'resetwallet'!"
                )
            }
        }
    }

    fun isServiceReady() =
        serviceReady.get()

    fun isBlockchainDownloaded() =
        blockChainDownloaded.get()

    fun isSufficientlyFunded() =
        funded.get()

    fun currentReceiveAddress(): String {
        return receiveAddress ?: run {
            val receiveAddress = wallet.currentReceiveAddress().toString()
            this.receiveAddress = receiveAddress
            receiveAddress
        }
    }

    private fun currentChangeAddress(): Address {
        return changeAddress ?: run {
            val changeAddress = wallet.currentChangeAddress()
            this.changeAddress = changeAddress
            changeAddress
        }
    }

    suspend fun getBalance(): Coin = withContext(contextCoroutineScope.coroutineContext) {
        wallet.getBalance(Wallet.BalanceType.AVAILABLE_SPENDABLE)
    }

    suspend fun getPendingBalance(): Coin = withContext(contextCoroutineScope.coroutineContext) {
        wallet.getBalance(Wallet.BalanceType.ESTIMATED) - wallet.balance
    }

    fun resetWallet() {
        receiveAddress = null
        setServiceReady(false)
        setBlockChainDownloaded(false)
        wallet.reset()
        shutdown()
        kit = createWalletAppKit(context, bitcoinNetwork.getFilePrefix(), null)
        initialize()
    }

    fun generatePopScript(opReturnData: ByteArray): Script {
        return ScriptBuilder().op(ScriptOpCodes.OP_RETURN).data(opReturnData).build()
    }

    suspend fun createPopTransaction(opReturnScript: Script): Transaction? {
        return sendTxRequest {
            val tx = Transaction(kit.params()).apply {
                addOutput(Coin.ZERO, opReturnScript)
            }
            SendRequest.forTx(tx).apply {
                ensureMinRequiredFee = configuration.enableMinRelayFee
                changeAddress = wallet.currentChangeAddress()
                feePerKb = getTransactionFeePerKb()
                changeAddress = currentChangeAddress()
            }
        }
    }

    fun getBlock(hash: Sha256Hash): Block? {
        return try {
            val block = blockStore[hash]
            if (block == null) {
                logger.warn("Unable to retrieve block {}", hash.toString())
                return null
            }
            block.header
        } catch (e: BlockStoreException) {
            logger.error("Unable to get block {} from store", hash.toString(), e)
            null
        }
    }

    val lastBlock: StoredBlock
        get() = blockChain.chainHead

    fun getBestBlock(hashes: Set<Sha256Hash>): Block? {
        // Check all given hashes exist
        hashes.forEach { hash ->
            if (blockStore.get(hash) == null) {
                error("Unable to find bitcoin block $hash")
            }
        }
        var cursor = blockChain.chainHead
        while (cursor.header.hash !in hashes) {
            cursor = cursor.getPrev(blockStore)
        }
        return cursor.header
    }

    private val downloadLock = Any()
    private val blockDownloader = ConcurrentHashMap<String, ListenableFuture<Block>>()

    suspend fun getFilteredBlock(hash: Sha256Hash): FilteredBlock {
        return blockCache.get(hash.toString())
    }

    suspend fun getPartialMerkleTree(hash: Sha256Hash): PartialMerkleTree? {
        try {
            logger.trace("Awaiting block {}...", hash.toString())
            val block = blockCache.get(hash.toString())
            return block.partialMerkleTree
        } catch (e: TimeoutException) {
            logger.debug("Unable to download Bitcoin block", e)
        } catch (e: InterruptedException) {
            logger.debug("Unable to download Bitcoin block", e)
        } catch (e: ExecutionException) {
            logger.debug("Unable to download Bitcoin block", e)
        }
        return null
    }

    suspend fun downloadBlock(hash: Sha256Hash): Block? {
        var attempts = 0
        var block: Block? = null
        while (block == null && attempts < 5) {
            logger.trace("Attempting to download block with hash {}", hash.toString())
            attempts++
            // Lock for read to see if we've got a download already started
            val blockFuture: ListenableFuture<Block> = synchronized(downloadLock) {
                blockDownloader[hash.toString()]?.also {
                    logger.trace("Found existing download of block {}", hash.toString())
                } ?: run {
                    logger.trace("Starting download of block {} from peer group", hash.toString())
                    val blockFuture = peerGroup.downloadPeer.getBlock(hash)
                    blockDownloader.putIfAbsent(hash.toString(), blockFuture)
                    blockFuture
                }
            }
            try {
                logger.trace("Waiting for block {} to finish downloading", hash.toString())
                block = blockFuture.asDeferred().await()
            } catch (e: TimeoutException) {
                logger.error("Unable to download Bitcoin block at the #{} attempt: {}", attempts, e.message)
                blockDownloader.remove(hash.toString())
            } catch (e: InterruptedException) {
                logger.error("Unable to download Bitcoin block at the #{} attempt: {}", attempts, e.message)
                blockDownloader.remove(hash.toString())
            } catch (e: ExecutionException) {
                logger.error("Unable to download Bitcoin block at the #{} attempt: {}", attempts, e.message)
                blockDownloader.remove(hash.toString())
            }
        }
        if (block != null) {
            logger.trace(
                "Finished downloading block with hash {} at the #{} attempt", hash.toString(), attempts
            )
        }
        return block
    }

    fun makeBlock(raw: ByteArray): Block {
        return serializer.makeBlock(raw)
    }

    fun makeBlocks(raw: Collection<ByteArray>): Collection<Block> {
        return raw.asSequence().map {
            serializer.makeBlock(it)
        }.toSet()
    }

    fun makeTransaction(raw: ByteArray): Transaction {
        val rawTx = serializer.makeTransaction(raw)

        // Try to get the transaction from the wallet first
        val reconstitutedTx = wallet.getTransaction(rawTx.txId)
        if (reconstitutedTx != null) {
            return reconstitutedTx
        }

        logger.debug("Could not find transaction {} in wallet", rawTx.txId.toString())
        return try {
            peerGroup.downloadPeer.getPeerMempoolTransaction(rawTx.txId).get()
        } catch (e: Exception) {
            throw RuntimeException("Unable to download mempool transaction", e)
        }
    }

    suspend fun sendCoins(address: String, amount: Coin): Transaction? {
        return sendTxRequest {
            SendRequest.to(
                Address.fromString(kit.params(), address), amount
            ).apply {
                changeAddress = wallet.currentChangeAddress()
                feePerKb = getTransactionFeePerKb()
            }
        }
    }

    suspend fun calculateFeesFromLatestBlock(): Pair<Int, Long>? {
        try {
            val chainHead = blockChain.chainHead
            val block = downloadBlock(chainHead.header.hash)
            if (block != null) {
                val transactions = block.transactions
                if (!transactions.isNullOrEmpty()) {
                    val fees = transactions[0].outputSum.minus(block.getBlockInflation(chainHead.height))
                    val averageFees = fees.longValue() / block.optimalEncodingMessageSize
                    return Pair.of(chainHead.height, averageFees)
                }
            }
        } catch (e: Exception) {
            logger.error("Unable to calculate fees from latest block", e)
        }
        return null
    }

    fun getMnemonicSeed(): List<String> {
        val seed = wallet.keyChainSeed
        if (seed != null) {
            val mnemonicCode = seed.mnemonicCode
            if (mnemonicCode != null) {
                val result = ArrayList(mnemonicCode)
                result.add(0, seed.creationTimeSeconds.toString())
                return result
            }
        }
        return emptyList()
    }

    fun importWallet(seedWords: String, creationTime: Long?): Boolean {
        val finalCreationTime = creationTime
            ?: Constants.DEFAULT_WALLET_CREATION_DATE
        return try {
            shutdown()
            val seed = DeterministicSeed(seedWords, null, "", finalCreationTime)
            kit = createWalletAppKit(context, bitcoinNetwork.getFilePrefix(), seed)
            initialize()
            true
        } catch (e: Exception) {
            logger.error("Could not import wallet", e)
            false
        }
    }

    fun exportPrivateKeys(): List<String> {
        val keys = wallet.activeKeyChain.leafKeys
        return keys.map {
            it.getPrivateKeyAsWiF(wallet.networkParameters)
        }
    }

    fun shutdown() {
        setServiceReady(false)
        setBlockChainDownloaded(false)
        receiveAddress = null
        kit.stopAsync()
        kit.awaitTerminated()
    }

    override fun onBlocksDownloaded(
        peer: Peer,
        block: Block,
        filteredBlock: FilteredBlock?,
        blocksLeft: Int
    ) {
        if (filteredBlock != null) {
            logger.trace("FilteredBlock {} downloaded", block.hashAsString)
            blockCache.put(block.hashAsString, filteredBlock)
        }
    }

    private suspend fun sendTxRequest(requestBuilder: () -> SendRequest): Transaction? {
        logger.trace("Waiting to acquire lock to create transaction")
        txLock.lock()
        logger.trace("Acquired lock to create transaction")
        try {
            val request = requestBuilder()
            try {
                wallet.completeTx(request)
                if (request.tx.fee.isGreaterThan(getMaximumTransactionFee())) {
                    throw ExceededMaxTransactionFee()
                }
                if (txBroadcastAudit.containsKey(request.tx.txId.toString())) {
                    throw DuplicateTransactionException()
                }
                logger.debug("Created transaction spending ${request.tx.inputs.size} inputs:")
                for (i in request.tx.inputs.indices) {
                    logger.debug(
                        "\t" + request.tx.inputs[i].outpoint.hash.toString() + ":" +
                            request.tx.inputs[i].outpoint.index
                    )
                }
            } catch (e: InsufficientMoneyException) {
                funded.set(false)
                EventBus.insufficientFundsEvent.trigger(wallet.balance)
                error("PoP wallet does not contain sufficient funds to create PoP transaction")
            } catch (e: Wallet.CompletionException) {
                error("Unable to complete transaction: ${e.javaClass.simpleName}")
            }

            // Broadcast the transaction to the network peer group
            // BitcoinJ adds a listener that will commit the transaction to the wallet when a
            // sufficient number of peers have announced receipt
            logger.debug("Broadcasting tx {} to peer group", request.tx.txId)
            val broadcast = kit.peerGroup().broadcastTransaction(request.tx)
            txBroadcastAudit[request.tx.txId.toString()] = EMPTY_OBJECT

            val deferredTransaction = broadcast.future().asDeferred()

            logger.debug { "Awaiting confirmation of broadcast of Tx ${request.tx.txId}" }
            return deferredTransaction.await()
        } finally {
            // Release the lock
            logger.trace("Releasing create transaction lock")
            txLock.unlock()
        }
    }

    fun getMaximumTransactionFee(): Coin =
        Coin.valueOf(maxFee)

    private fun getTransactionFeePerKb(): Coin =
        Coin.valueOf((feePerKb * SEGWIT_TX_FEE_RATE).roundToLong())
}

private fun BitcoinNetwork.getFilePrefix(): String {
    val filePrefix = "bitcoin-pop"
    return when (this) {
        BitcoinNetwork.MainNet -> filePrefix
        BitcoinNetwork.TestNet -> "$filePrefix-testnet"
        BitcoinNetwork.RegTest -> "$filePrefix-regtest"
    }
}
