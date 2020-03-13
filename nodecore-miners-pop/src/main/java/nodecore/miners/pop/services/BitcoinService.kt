// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.miners.pop.services

import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import nodecore.miners.pop.Configuration
import nodecore.miners.pop.Constants
import nodecore.miners.pop.Threading
import nodecore.miners.pop.common.BitcoinNetwork
import nodecore.miners.pop.events.CoinsReceivedEventDto
import nodecore.miners.pop.events.EventBus
import nodecore.miners.pop.model.ApplicationExceptions.CorruptSPVChain
import nodecore.miners.pop.model.ApplicationExceptions.DuplicateTransactionException
import nodecore.miners.pop.model.ApplicationExceptions.ExceededMaxTransactionFee
import nodecore.miners.pop.model.ApplicationExceptions.SendTransactionException
import nodecore.miners.pop.model.ApplicationExceptions.UnableToAcquireTransactionLock
import org.apache.commons.lang3.tuple.Pair
import org.bitcoinj.core.Address
import org.bitcoinj.core.BitcoinSerializer
import org.bitcoinj.core.Block
import org.bitcoinj.core.BlockChain
import org.bitcoinj.core.Coin
import org.bitcoinj.core.Context
import org.bitcoinj.core.FilteredBlock
import org.bitcoinj.core.PartialMerkleTree
import org.bitcoinj.core.Peer
import org.bitcoinj.core.PeerGroup
import org.bitcoinj.core.Sha256Hash
import org.bitcoinj.core.StoredBlock
import org.bitcoinj.core.Transaction
import org.bitcoinj.core.listeners.BlocksDownloadedEventListener
import org.bitcoinj.core.listeners.DownloadProgressTracker
import org.bitcoinj.kits.WalletAppKit
import org.bitcoinj.script.Script
import org.bitcoinj.script.ScriptBuilder
import org.bitcoinj.script.ScriptOpCodes
import org.bitcoinj.store.BlockStore
import org.bitcoinj.store.BlockStoreException
import org.bitcoinj.wallet.DeterministicSeed
import org.bitcoinj.wallet.SendRequest
import org.bitcoinj.wallet.Wallet
import org.veriblock.core.utilities.createLogger
import java.io.File
import java.util.ArrayList
import java.util.Date
import java.util.HashMap
import java.util.LinkedHashMap
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutionException
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.function.Supplier

private val logger = createLogger {}

private val EMPTY_OBJECT = Any()

class BitcoinService(
    private val configuration: Configuration,
    val context: Context,
    private val blockCache: BitcoinBlockCache
) : BlocksDownloadedEventListener {

    private var kit: WalletAppKit

    private lateinit var blockChain: BlockChain
    private lateinit var blockStore: BlockStore
    private lateinit var wallet: Wallet
    private lateinit var peerGroup: PeerGroup

    private val serializer: BitcoinSerializer
    private val txGate = Semaphore(1, true)
    private val txBroadcastAudit = object : LinkedHashMap<String, Any>() {
        override fun removeEldestEntry(eldest: Map.Entry<String, Any>): Boolean {
            return size > 50
        }
    }

    private val bitcoinNetwork = configuration.bitcoinNetwork

    private var isBlockchainDownloaded = false
    private var isServiceReady = false
    private var receiveAddress: String? = null
    private var changeAddress: Address? = null

    init {
        logger.info("Using Bitcoin {} network", bitcoinNetwork.toString())
        serializer = BitcoinSerializer(context.params, true)
        kit = createWalletAppKit(context, bitcoinNetwork.getFilePrefix(), null)
        logger.info("BitcoinService constructor finished")
    }

    fun setServiceReady(value: Boolean) {
        if (value == isServiceReady) {
            return
        }
        isServiceReady = value
        if (isServiceReady) {
            logger.info("Bitcoin service is ready")
            EventBus.bitcoinServiceReadyEvent.trigger()
        } else {
            logger.warn("Bitcoin service is not ready")
            EventBus.bitcoinServiceNotReadyEvent.trigger()
        }
    }

    private fun createWalletAppKit(
        context: Context,
        filePrefix: String,
        seed: DeterministicSeed?
    ): WalletAppKit {
        isBlockchainDownloaded = false
        var kit: WalletAppKit = object : WalletAppKit(
            context, Script.ScriptType.P2WPKH, null, File("."), filePrefix
        ) {
            override fun onSetupCompleted() {
                super.onSetupCompleted()
                blockStore = store()

                wallet = wallet().apply {
                    isAcceptRiskyTransactions = true
                    addCoinsReceivedEventListener { _, tx: Transaction, prevBalance: Coin, newBalance: Coin ->
                        EventBus.coinsReceivedEvent.trigger(CoinsReceivedEventDto(tx, prevBalance, newBalance))
                    }
                }

                blockChain = chain()

                peerGroup = peerGroup().apply {
                    useLocalhostPeerWhenPossible = configuration.isBitcoinUseLocalhostPeer
                    minRequiredProtocolVersion = configuration.bitcoinProtocolVersion.bitcoinProtocolVersion
                    maxConnections = configuration.bitcoinMaxPeerConnections
                    setPeerDiscoveryTimeoutMillis(configuration.bitcoinPeerDiscoveryTimeoutMillis.toLong())
                    setDownloadTxDependencies(configuration.bitcoinDownloadTxDependencies)
                    setRequiredServices(configuration.bitcoinRequiredServices)
                    minBroadcastConnections = configuration.bitcoinMinPeerBroadcastConnections
                    maxPeersToDiscoverCount = configuration.bitcoinMaxPeersToDiscoverCount
                    pingIntervalMsec = configuration.bitcoinPeerPingIntervalMsec.toLong()
                    addBlocksDownloadedEventListener(this@BitcoinService)
                }

                setServiceReady(true)
            }
        }
        kit.setBlockingStartup(false)
        kit.setDownloadListener(object : DownloadProgressTracker() {
            override fun doneDownload() {
                if (!isBlockchainDownloaded) {
                    isBlockchainDownloaded = true
                    logger.info("Bitcoin blockchain finished downloading")
                    EventBus.blockchainDownloadedEvent.trigger()
                }
            }

            override fun progress(pct: Double, blocksSoFar: Int, date: Date) {
                super.progress(pct, blocksSoFar, date)

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

    @Throws(CorruptSPVChain::class)
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
                    "A corrupt SPV chain has been detected but could not be " + "deleted. Please delete " + spvchain.absolutePath +
                        ", restart the PoP miner, and run 'resetwallet'!"
                )
            }
        }
    }

    fun serviceReady(): Boolean {
        return isBlockchainDownloaded && isServiceReady
    }

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

    fun getBalance(): Coin =
        wallet.getBalance(Wallet.BalanceType.AVAILABLE_SPENDABLE)

    fun resetWallet() {
        receiveAddress = null
        setServiceReady(false)
        wallet.reset()
        shutdown()
        kit = createWalletAppKit(context, bitcoinNetwork.getFilePrefix(), null)
        initialize()
    }

    fun blockchainDownloaded(): Boolean {
        return isBlockchainDownloaded
    }

    fun generatePoPScript(opReturnData: ByteArray): Script {
        return ScriptBuilder().op(ScriptOpCodes.OP_RETURN).data(opReturnData).build()
    }

    @Throws(SendTransactionException::class)
    fun createPoPTransaction(opReturnScript: Script): ListenableFuture<Transaction> {
        return sendTxRequest(Supplier {
            val tx = Transaction(kit.params())
            tx.addOutput(Coin.ZERO, opReturnScript)
            val request = SendRequest.forTx(tx)
            request.ensureMinRequiredFee = configuration.isMinimumRelayFeeEnforced
            request.changeAddress = wallet.currentChangeAddress()
            request.feePerKb = getTransactionFeePerKB()
            request.changeAddress = currentChangeAddress()
            request
        })
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

    fun getBestBlock(hashes: Collection<Sha256Hash>): Block? {
        val storedBlocks = HashMap<Sha256Hash, StoredBlock>()
        for (hash in hashes) {
            try {
                storedBlocks[hash] = blockStore[hash]
            } catch (e: BlockStoreException) {
                logger.error("Unable to get block from store", e)
            }
        }
        var cursor = blockChain.chainHead
        do {
            if (storedBlocks.containsKey(cursor.header.hash)) {
                return cursor.header
            }
            cursor = try {
                cursor.getPrev(blockStore)
            } catch (e: BlockStoreException) {
                logger.error("Unable to get block from store", e)
                break
            }
        } while (cursor != null)
        return null
    }

    private val downloadLock = Any()
    private val blockDownloader = ConcurrentHashMap<String, ListenableFuture<Block>>()

    fun getFilteredBlockFuture(hash: Sha256Hash): ListenableFuture<FilteredBlock> {
        return blockCache.getAsync(hash.toString())
    }

    fun getPartialMerkleTree(hash: Sha256Hash): PartialMerkleTree? {
        try {
            logger.info("Awaiting block {}...", hash.toString())
            val block = blockCache.getAsync(hash.toString())[configuration.actionTimeout.toLong(), TimeUnit.SECONDS]
            if (block != null) {
                return block.partialMerkleTree
            }
        } catch (e: TimeoutException) {
            logger.debug("Unable to download Bitcoin block", e)
        } catch (e: InterruptedException) {
            logger.debug("Unable to download Bitcoin block", e)
        } catch (e: ExecutionException) {
            logger.debug("Unable to download Bitcoin block", e)
        }
        return null
    }

    fun downloadBlock(hash: Sha256Hash): Block? {
        var attempts = 0
        var block: Block? = null
        while (block == null && attempts < 5) {
            logger.info("Attempting to download block with hash {}", hash.toString())
            attempts++
            // Lock for read to see if we've got a download already started
            val blockFuture: ListenableFuture<Block> = synchronized(downloadLock) {
                blockDownloader[hash.toString()]?.also {
                    logger.info("Found existing download of block {}", hash.toString())
                } ?: run {
                    logger.info("Starting download of block {} from peer group", hash.toString())
                    val blockFuture = peerGroup.downloadPeer.getBlock(hash)
                    blockDownloader.putIfAbsent(hash.toString(), blockFuture)
                    blockFuture
                }
            }
            try {
                logger.info("Waiting for block {} to finish downloading", hash.toString())
                block = blockFuture[configuration.actionTimeout.toLong(), TimeUnit.SECONDS]
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
            logger.info(
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
        val reconstitutedTx = wallet.getTransaction(rawTx.txId) ?: run {
            logger.debug("Could not find transaction {} in wallet", rawTx.txId.toString())
            try {
                peerGroup.downloadPeer.getPeerMempoolTransaction(rawTx.txId).get()
            } catch (e: Exception) {
                throw RuntimeException("Unable to download mempool transaction", e)
            }
        }
        return reconstitutedTx
    }

    @Throws(SendTransactionException::class)
    fun sendCoins(address: String, amount: Coin): Transaction {
        return try {
            sendTxRequest(Supplier {
                val sendRequest = SendRequest.to(
                    Address.fromString(kit.params(), address), amount
                )
                sendRequest.changeAddress = wallet.currentChangeAddress()
                sendRequest.feePerKb = getTransactionFeePerKB()
                sendRequest
            }).get()
        } catch (e: InterruptedException) {
            throw SendTransactionException(e)
        } catch (e: ExecutionException) {
            throw SendTransactionException(e)
        }
    }

    fun calculateFeesFromLatestBlock(): Pair<Int, Long>? {
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
                val result = ArrayList(seed.mnemonicCode)
                result.add(0, seed.creationTimeSeconds.toString())
                return result
            }
        }
        return emptyList()
    }

    fun importWallet(seedWords: String?, creationTime: Long?): Boolean {
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
            logger.debug("FilteredBlock {} downloaded", block.hashAsString)
            blockCache.put(block.hashAsString, filteredBlock)
        }
    }

    @Throws(SendTransactionException::class)
    private fun sendTxRequest(requestBuilder: Supplier<SendRequest>): ListenableFuture<Transaction> {
        try {
            acquireTxLock()
        } catch (e: UnableToAcquireTransactionLock) {
            throw SendTransactionException(e)
        }
        val request = requestBuilder.get()
        try {
            // WalletShim is a temporary solution until improvements present in bitcoinj's master branch
            // are packaged into a published release
            wallet.completeTx(request)
            if (request.tx.fee.isGreaterThan(getMaximumTransactionFee())) {
                throw ExceededMaxTransactionFee()
            }
            if (txBroadcastAudit.containsKey(request.tx.txId.toString())) {
                throw DuplicateTransactionException()
            }
            logger.info(
                "Created transaction spending " + request.tx.inputs.size + " inputs:"
            )
            for (i in request.tx.inputs.indices) {
                logger.info(
                    "\t" + request.tx.inputs[i].outpoint.hash.toString() + ":" +
                        request.tx.inputs[i].outpoint.index
                )
            }
        } catch (e: Exception) {
            releaseTxLock()
            throw SendTransactionException(e)
        }

        // Broadcast the transaction to the network peer group
        // BitcoinJ adds a listener that will commit the transaction to the wallet when a
        // sufficient number of peers have announced receipt
        logger.info("Broadcasting tx {} to peer group", request.tx.txId)
        val broadcast = kit.peerGroup().broadcastTransaction(request.tx)
        txBroadcastAudit[request.tx.txId.toString()] = EMPTY_OBJECT

        // Add a callback that releases the semaphore permit
        Futures.addCallback(
            broadcast.future(), object : FutureCallback<Transaction?> {
            override fun onSuccess(result: Transaction?) {
                releaseTxLock()
            }

            override fun onFailure(t: Throwable) {
                releaseTxLock()
            }
        }, Threading.TASK_POOL
        )
        logger.info(
            "Awaiting confirmation of broadcast of Tx {}", request.tx.txId.toString()
        )
        return broadcast.future()
    }

    @Throws(UnableToAcquireTransactionLock::class)
    private fun acquireTxLock() {
        logger.info("Waiting to acquire lock to create transaction")
        try {
            val permitted = txGate.tryAcquire(10, TimeUnit.SECONDS)
            if (!permitted) {
                throw UnableToAcquireTransactionLock()
            }
        } catch (e: InterruptedException) {
            throw UnableToAcquireTransactionLock()
        }
        logger.info("Acquired lock to create transaction")
    }

    private fun releaseTxLock() {
        logger.info("Releasing create transaction lock")
        txGate.release()
    }

    private fun getMaximumTransactionFee() =
        Coin.valueOf(configuration.maxTransactionFee)

    private fun getTransactionFeePerKB() =
        Coin.valueOf(configuration.transactionFeePerKB)
}

private fun BitcoinNetwork.getFilePrefix(): String {
    val filePrefix = "bitcoin-pop"
    return when (this) {
        BitcoinNetwork.MainNet -> filePrefix
        BitcoinNetwork.TestNet -> "$filePrefix-testnet"
        BitcoinNetwork.RegTest -> "$filePrefix-regtest"
    }
}
