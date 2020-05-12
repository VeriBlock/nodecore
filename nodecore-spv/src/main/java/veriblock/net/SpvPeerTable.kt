// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package veriblock.net

import com.google.common.util.concurrent.ListeningScheduledExecutorService
import com.google.common.util.concurrent.MoreExecutors
import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.google.protobuf.ByteString
import nodecore.api.grpc.VeriBlockMessages
import nodecore.api.grpc.VeriBlockMessages.Event.ResultsCase
import nodecore.api.grpc.VeriBlockMessages.LedgerProofReply.LedgerProofResult
import nodecore.api.grpc.VeriBlockMessages.LedgerProofRequest
import nodecore.api.grpc.VeriBlockMessages.TransactionAnnounce
import nodecore.api.grpc.utilities.ByteStringUtility
import org.veriblock.core.bitcoinj.Base58
import org.veriblock.core.crypto.BloomFilter
import org.veriblock.core.utilities.createLogger
import org.veriblock.core.crypto.Sha256Hash
import org.veriblock.sdk.models.VeriBlockBlock
import spark.utils.CollectionUtils
import veriblock.SpvContext
import veriblock.listeners.PendingTransactionDownloadedListener
import veriblock.model.DownloadStatus
import veriblock.model.DownloadStatusResponse
import veriblock.model.FutureEventReply
import veriblock.model.LedgerContext
import veriblock.model.ListenerRegistration
import veriblock.model.NetworkMessage
import veriblock.model.NodeMetadata
import veriblock.model.PeerAddress
import veriblock.model.StandardTransaction
import veriblock.model.Transaction
import veriblock.model.TransactionTypeIdentifier
import veriblock.model.mapper.LedgerProofReplyMapper
import veriblock.serialization.MessageSerializer
import veriblock.serialization.MessageSerializer.deserializeNormalTransaction
import veriblock.service.Blockchain
import veriblock.service.PendingTransactionContainer
import veriblock.util.MessageIdGenerator.next
import veriblock.util.Threading
import veriblock.util.Threading.shutdown
import veriblock.validator.LedgerProofReplyValidator
import java.io.IOException
import java.net.Socket
import java.sql.SQLException
import java.util.Collections
import java.util.concurrent.BlockingQueue
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.LinkedTransferQueue
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import java.util.function.Predicate
import java.util.stream.Collectors

private val logger = createLogger {}

const val DEFAULT_CONNECTIONS = 12
const val BLOOM_FILTER_TWEAK = 710699166
const val BLOOM_FILTER_FALSE_POSITIVE_RATE = 0.02
const val BLOCK_DIFFERENCE_TO_SWITCH_ON_ANOTHER_PEER = 200
const val AMOUNT_OF_BLOCKS_WHEN_WE_CAN_START_WORKING = 50

class SpvPeerTable(
    private val spvContext: SpvContext,
    private val p2PService: P2PService,
    peerDiscovery: PeerDiscovery,
    pendingTransactionContainer: PendingTransactionContainer
) : PeerConnectedEventListener, PeerDisconnectedEventListener, MessageReceivedEventListener {
    private val lock = ReentrantLock()
    private val running = AtomicBoolean(false)
    val discovery: PeerDiscovery
    private val blockchain: Blockchain
    private val executor: ListeningScheduledExecutorService
    private val messageExecutor: ScheduledExecutorService
    var maximumPeers = DEFAULT_CONNECTIONS
    var downloadPeer: Peer? = null
    val bloomFilter: BloomFilter
    private val addressesState: MutableMap<String, LedgerContext> = ConcurrentHashMap()
    private val pendingTransactionContainer: PendingTransactionContainer

    private val peers = ConcurrentHashMap<String, Peer>()
    private val pendingPeers = ConcurrentHashMap<String, Peer>()
    private val incomingQueue: BlockingQueue<NetworkMessage> = LinkedTransferQueue()
    private val pendingTransactionDownloadedEventListeners = CopyOnWriteArrayList<ListenerRegistration<PendingTransactionDownloadedListener>>()
    private val futureExecutor = Executors.newCachedThreadPool()
    private val futureEventReplyList: MutableMap<String, FutureEventReply> = ConcurrentHashMap()

    init {
        bloomFilter = createBloomFilter()
        blockchain = spvContext.blockchain
        executor = MoreExecutors
            .listeningDecorator(
                Executors.newSingleThreadScheduledExecutor(
                    ThreadFactoryBuilder().setNameFormat("PeerTable Thread").build()
                )
            )
        messageExecutor = Executors.newSingleThreadScheduledExecutor(
            ThreadFactoryBuilder().setNameFormat("Message Handler Thread").build()
        )
        discovery = peerDiscovery
        this.pendingTransactionContainer = pendingTransactionContainer
        addPendingTransactionDownloadedEventListeners(executor, spvContext.pendingTransactionDownloadedListener)
    }

    fun start() {
        running.set(true)
        discoverPeers()
        executor.scheduleAtFixedRate({ requestAddressState() }, 5, 60, TimeUnit.SECONDS)
        executor.scheduleAtFixedRate({ discoverPeers() }, 0, 60, TimeUnit.SECONDS)
        executor.scheduleAtFixedRate({ requestPendingTransactions() }, 5, 60, TimeUnit.SECONDS)

        // Scheduling with a fixed delay allows it to recover in the event of an unhandled exception
        messageExecutor.scheduleWithFixedDelay({ processIncomingMessages() }, 1, 1, TimeUnit.SECONDS)
    }

    fun shutdown() {
        running.set(false)

        // Shut down executors
        shutdown(messageExecutor)
        shutdown(futureExecutor)
        shutdown(executor)

        // Close peer connections
        incomingQueue.clear()
        pendingPeers.clear()
        peers.forEachValue(
            4
        ) { obj: Peer? -> obj!!.closeConnection() }
    }

    fun connectTo(address: PeerAddress): Peer? {
        if (peers.containsKey(address.address)) {
            return peers[address.address]
        }
        val peer = createPeer(address)
        peer.addConnectedEventListener(Threading.LISTENER_THREAD, this)
        peer.addDisconnectedEventListener(Threading.LISTENER_THREAD, this)
        lock.lock()
        return try {
            openConnection(peer)
            pendingPeers[address.address] = peer
            peer
        } catch (e: IOException) {
            logger.error("Unable to open connection", e)
            null
        } finally {
            lock.unlock()
        }
    }

    fun createPeer(address: PeerAddress): Peer {
        return Peer(spvContext, blockchain, NodeMetadata, address.address, address.port)
    }

    @Throws(IOException::class)
    fun openConnection(peer: Peer) {
        val socket = Socket(peer.address, peer.port)
        val handler = PeerSocketHandler(socket)
        peer.setConnection(handler)
    }

    fun startBlockchainDownload(peer: Peer) {
        logger.info("Beginning blockchain download")
        try {
            downloadPeer = peer
            peer.startBlockchainDownload()
        } catch (ex: Exception) {
            downloadPeer = null
            //TODO SPV-70 add bun on some time.
            logger.error(ex.message, ex)
        }
    }

    private fun requestAddressState() {
        val addresses = spvContext.addressManager.all
        if (CollectionUtils.isEmpty(addresses)) {
            return
        }
        val ledgerProof = LedgerProofRequest.newBuilder()
        for (address in addresses) {
            if (!addressesState.containsKey(address!!.hash)) {
                addressesState[address.hash] = LedgerContext()
            }
            ledgerProof.addAddresses(ByteString.copyFrom(Base58.decode(address.hash)))
        }
        val request = VeriBlockMessages.Event.newBuilder()
            .setId(next())
            .setAcknowledge(false)
            .setLedgerProofRequest(ledgerProof.build())
            .build()
        logger.info("Request address state.")
        for (peer in peers.values) {
            peer.sendMessage(request)
        }
    }

    private fun requestPendingTransactions() {
        val pendingTransactionsId = pendingTransactionContainer.getPendingTransactionsId()
        for (sha256Hash in pendingTransactionsId) {
            val request = VeriBlockMessages.Event.newBuilder()
                .setId(next())
                .setAcknowledge(false)
                .setTransactionRequest(
                    VeriBlockMessages.GetTransactionRequest.newBuilder()
                        .setId(ByteString.copyFrom(sha256Hash.bytes))
                        .build()
                )
                .build()
            for (peer in peers.values) {
                peer!!.sendMessage(request)
            }
        }
    }

    private fun discoverPeers() {
        val maxConnections = maximumPeers
        if (maxConnections > 0 && countConnectedPeers() >= maxConnections) {
            return
        }
        val needed = maxConnections - (countConnectedPeers() + countPendingPeers())
        if (needed > 0) {
            val candidates = discovery.getPeers(needed)
            for (address in candidates) {
                if (peers.containsKey(address.address) || pendingPeers.containsKey(address.address)) {
                    continue
                }
                logger.info("Attempting connection to {}:{}", address.address, address.port)
                val peer = connectTo(address)
                logger.info("Discovered peer connected {}:{}", peer!!.address, peer.port)
            }
        }
    }

    private fun processIncomingMessages() {
        try {
            while (running.get()) {
                try {
                    val message = incomingQueue.take()
                    logger.info("{} message from {}", message.message.resultsCase.name, message.sender.address)
                    when (message.message.resultsCase) {
                        ResultsCase.HEARTBEAT -> {
                            val heartbeat = message.message.heartbeat
                            if (downloadPeer == null && heartbeat.block.number > 0) {
                                startBlockchainDownload(message.sender)
                            } else if (downloadPeer != null &&
                                heartbeat.block.number - downloadPeer!!.bestBlockHeight > BLOCK_DIFFERENCE_TO_SWITCH_ON_ANOTHER_PEER
                            ) {
                                startBlockchainDownload(message.sender)
                            }
                        }
                        ResultsCase.ADVERTISE_BLOCKS -> {
                            val advertiseBlocks = message.message.advertiseBlocks
                            logger.info(
                                "PeerTable Received advertisement of {} blocks, height {}", advertiseBlocks.headersList.size,
                                blockchain.getChainHead()!!.height
                            )
                            val veriBlockBlocks: List<VeriBlockBlock> = advertiseBlocks.headersList.map {
                                MessageSerializer.deserialize(it)
                            }
                            try {
                                blockchain.addAll(veriBlockBlocks)
                            } catch (e: SQLException) {
                                logger.error("Unable to add block to blockchain", e)
                            }
                        }
                        ResultsCase.TRANSACTION -> {
                            // TODO: Different Transaction types
                            val standardTransaction = deserializeNormalTransaction(
                                message.message.transaction
                            )
                            notifyPendingTransactionDownloaded(standardTransaction)
                        }
                        ResultsCase.TX_REQUEST -> {
                            val txIds = message.message.txRequest.transactionsList.map {
                                Sha256Hash.wrap(
                                    ByteStringUtility.byteStringToHex(it.txId)
                                )
                            }
                            p2PService.onTransactionRequest(txIds, message.sender)
                        }
                        ResultsCase.LEDGER_PROOF_REPLY -> {
                            val proofReply = message.message.ledgerProofReply.proofsList
                            val ledgerContexts: List<LedgerContext> = proofReply.asSequence().filter { lpr: LedgerProofResult ->
                                addressesState.containsKey(Base58.encode(lpr.address.toByteArray()))
                            }.filter {
                                LedgerProofReplyValidator.validate(it)
                            }.map {
                                LedgerProofReplyMapper.map(it)
                            }.toList()
                            updateAddressState(ledgerContexts)
                        }
                        ResultsCase.TRANSACTION_REPLY -> if (message.message.transactionReply.success) {
                            pendingTransactionContainer.updateTransactionInfo(message.message.transactionReply.transaction)
                        }
                        ResultsCase.DEBUG_VTB_REPLY -> {
                        }
                        ResultsCase.VERIBLOCK_PUBLICATIONS_REPLY -> if (futureEventReplyList.containsKey(message.message.requestId)) {
                            futureEventReplyList[message.message.requestId]!!.response(message.message)
                        }
                    }
                } catch (e: InterruptedException) {
                    break
                }
            }
        } catch (e: Exception) {
            logger.error("An unhandled exception occurred processing message queue", e)
        }
    }

    private fun createBloomFilter(): BloomFilter {
        val addresses = spvContext.addressManager.all
        val filter = BloomFilter(
            spvContext.addressManager.numAddresses + 10, BLOOM_FILTER_FALSE_POSITIVE_RATE,
            BLOOM_FILTER_TWEAK
        )
        for (address in addresses) {
            filter.insert(address.hash)
        }
        return filter
    }

    private fun updateAddressState(ledgerContexts: List<LedgerContext>) {
        for (ledgerContext in ledgerContexts) {
            if (addressesState[ledgerContext.address!!.address]!!.compareTo(ledgerContext) > 0) {
                addressesState.replace(ledgerContext.address!!.address, ledgerContext)
            }
        }
    }

    fun addPendingTransactionDownloadedEventListeners(
        executor: Executor?,
        listener: PendingTransactionDownloadedListener
    ) {
        pendingTransactionDownloadedEventListeners.add(
            ListenerRegistration(listener, executor!!)
        )
    }

    private fun notifyPendingTransactionDownloaded(tx: StandardTransaction) {
        for (registration in pendingTransactionDownloadedEventListeners) {
            registration.executor.execute { registration.listener.onPendingTransactionDownloaded(tx) }
        }
    }

    override fun onPeerConnected(peer: Peer?) {
        lock.lock()
        try {
            logger.info("Peer {} connected", peer!!.address)
            pendingPeers.remove(peer.address)
            peers[peer.address] = peer

            // TODO: Wallet related setup (bloom filter)

            // Attach listeners
            peer.addMessageReceivedEventListeners(executor, this)
            peer.setFilter(bloomFilter)
        } finally {
            lock.unlock()
        }
    }

    override fun onPeerDisconnected(peer: Peer) {
        lock.lock()
        try {
            pendingPeers.remove(peer.address)
            peers.remove(peer.address)
            if (downloadPeer != null && downloadPeer!!.address.equals(peer.address, ignoreCase = true)) {
                downloadPeer = null
            }
        } finally {
            lock.unlock()
        }
    }

    override fun onMessageReceived(message: VeriBlockMessages.Event, sender: Peer) {
        try {
            logger.info("Message Received messageId: {}, from: {}:{}", message.id, sender.address, sender.port)
            incomingQueue.put(NetworkMessage(sender, message))
        } catch (e: InterruptedException) {
            logger.error("onMessageReceived interrupted", e)
        }
    }

    fun advertise(transaction: Transaction) {
        val advertise = VeriBlockMessages.Event.newBuilder()
            .setId(next())
            .setAcknowledge(false)
            .setAdvertiseTx(
                VeriBlockMessages.AdvertiseTransaction.newBuilder()
                    .addTransactions(
                        TransactionAnnounce.newBuilder()
                            .setType(
                                if (transaction.transactionTypeIdentifier === TransactionTypeIdentifier.PROOF_OF_PROOF) TransactionAnnounce.Type.PROOF_OF_PROOF else TransactionAnnounce.Type.NORMAL
                            )
                            .setTxId(ByteString.copyFrom(transaction.txId.bytes))
                            .build()
                    )
                    .build()
            )
            .build()
        for (peer in peers.values) {
            try {
                peer!!.sendMessage(advertise)
            } catch (ex: Exception) {
                logger.error(ex.message, ex)
            }
        }
    }

    fun advertiseWithReply(event: VeriBlockMessages.Event): Future<VeriBlockMessages.Event> {
        val futureEventReply = FutureEventReply()
        futureEventReplyList[event.id] = futureEventReply
        for (peer in peers.values) {
            try {
                peer.sendMessage(event)
            } catch (ex: Exception) {
                logger.error(ex.message, ex)
            }
        }
        return futureExecutor.submit(Callable {
            while (!futureEventReply.isDone()) {
                try {
                    Thread.sleep(2000)
                } catch (e: InterruptedException) {
                    logger.error(e.message, e)
                }
            }
            futureEventReplyList.remove(event.id)
            futureEventReply.response
        })
    }

    fun getSignatureIndex(address: String): Long? {
        return if (addressesState[address]!!.ledgerValue != null) addressesState[address]!!.ledgerValue!!.signatureIndex else null
    }

    fun getAvailablePeers(): Int = peers.size

    fun getBestBlockHeight(): Int = peers.values.map { it.bestBlockHeight }.max() ?: 0

    fun getDownloadStatus(): DownloadStatusResponse {
        val status: DownloadStatus
        val currentHeight = blockchain.getChainHead()!!.height
        val bestBlockHeight = if (downloadPeer == null) 0 else downloadPeer!!.bestBlockHeight
        status = if (downloadPeer == null) {
            DownloadStatus.DISCOVERING
        } else if (bestBlockHeight - currentHeight < AMOUNT_OF_BLOCKS_WHEN_WE_CAN_START_WORKING) {
            DownloadStatus.READY
        } else {
            DownloadStatus.DOWNLOADING
        }
        return DownloadStatusResponse(status, currentHeight, bestBlockHeight)
    }

    fun getAddressesState(): Map<String, LedgerContext> {
        return addressesState
    }

    fun getAddressState(address: String): LedgerContext? {
        return addressesState[address]
    }

    fun getConnectedPeers(): Collection<Peer> = Collections.unmodifiableCollection(peers.values)

    fun countConnectedPeers(): Int {
        return peers.size
    }

    fun countPendingPeers(): Int {
        return pendingPeers.size
    }
}
