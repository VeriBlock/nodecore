// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package veriblock.net

import com.google.protobuf.ByteString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withTimeout
import nodecore.api.grpc.VeriBlockMessages
import nodecore.api.grpc.VeriBlockMessages.Event.ResultsCase
import nodecore.api.grpc.VeriBlockMessages.LedgerProofReply.LedgerProofResult
import nodecore.api.grpc.VeriBlockMessages.LedgerProofRequest
import nodecore.api.grpc.VeriBlockMessages.TransactionAnnounce
import nodecore.api.grpc.utilities.ByteStringUtility
import nodecore.api.grpc.utilities.extensions.toHex
import org.veriblock.core.bitcoinj.Base58
import org.veriblock.core.crypto.BloomFilter
import org.veriblock.core.utilities.createLogger
import org.veriblock.core.crypto.Sha256Hash
import org.veriblock.core.wallet.AddressPubKey
import org.veriblock.sdk.models.VeriBlockBlock
import veriblock.SpvContext
import veriblock.model.DownloadStatus
import veriblock.model.DownloadStatusResponse
import veriblock.model.LedgerContext
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
import veriblock.service.OutputData
import veriblock.service.PendingTransactionContainer
import veriblock.service.TransactionData
import veriblock.service.TransactionInfo
import veriblock.service.TransactionType
import veriblock.util.EventBus
import veriblock.util.Threading
import veriblock.util.buildMessage
import veriblock.util.nextMessageId
import veriblock.util.launchWithFixedDelay
import veriblock.validator.LedgerProofReplyValidator
import java.io.IOException
import java.sql.SQLException
import java.util.Collections
import java.util.concurrent.BlockingQueue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedTransferQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

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
) {
    private val lock = ReentrantLock()
    private val running = AtomicBoolean(false)
    private val discovery: PeerDiscovery
    private val blockchain: Blockchain
    var maximumPeers = DEFAULT_CONNECTIONS
    var downloadPeer: Peer? = null
    val bloomFilter: BloomFilter
    private val addressesState: MutableMap<String, LedgerContext> = ConcurrentHashMap()
    private val pendingTransactionContainer: PendingTransactionContainer

    private val peers = ConcurrentHashMap<String, Peer>()
    private val pendingPeers = ConcurrentHashMap<String, Peer>()
    private val incomingQueue: BlockingQueue<NetworkMessage> = LinkedTransferQueue()

    init {
        bloomFilter = createBloomFilter()
        blockchain = spvContext.blockchain
        discovery = peerDiscovery
        this.pendingTransactionContainer = pendingTransactionContainer

        EventBus.pendingTransactionDownloadedEvent.register(
            spvContext.pendingTransactionDownloadedListener,
            spvContext.pendingTransactionDownloadedListener::onPendingTransactionDownloaded
        )
    }

    fun start() {
        running.set(true)

        EventBus.peerConnectedEvent.register(this, ::onPeerConnected)
        EventBus.peerDisconnectedEvent.register(this, ::onPeerDisconnected)
        EventBus.messageReceivedEvent.register(this) {
            onMessageReceived(it.message, it.peer)
        }

        val executorScope = CoroutineScope(Threading.PEER_TABLE_THREAD.asCoroutineDispatcher())
        executorScope.launchWithFixedDelay(5_000L, 20_000L) {
            requestAddressState()
        }
        discoverPeers()
        executorScope.launchWithFixedDelay(5_000L, 20_000L) {
            discoverPeers()
        }
        executorScope.launchWithFixedDelay(5_000L, 20_000L) {
            requestPendingTransactions()
        }

        // Scheduling with a fixed delay allows it to recover in the event of an unhandled exception
        val messageHandlerScope = CoroutineScope(Threading.MESSAGE_HANDLER_THREAD.asCoroutineDispatcher())
        messageHandlerScope.launch {
            processIncomingMessages()
        }
    }

    fun shutdown() {
        running.set(false)

        // Close peer connections
        incomingQueue.clear()
        pendingPeers.clear()
        peers.forEachValue(4) {
            it.closeConnection()
        }
    }

    fun connectTo(address: PeerAddress): Peer {
        peers[address.address]?.let {
            return it
        }
        lock.lock()
        return try {
            val peer = createPeer(address)
            pendingPeers[address.address] = peer
            peer
        } catch (e: IOException) {
            logger.error("Unable to open connection", e)
            throw e
        } finally {
            lock.unlock()
        }
    }

    fun createPeer(address: PeerAddress): Peer {
        return Peer(spvContext, blockchain, NodeMetadata, address.address, address.port)
    }

    fun startBlockchainDownload(peer: Peer) {
        logger.debug("Beginning blockchain download")
        try {
            downloadPeer = peer
            peer.startBlockchainDownload()
        } catch (ex: Exception) {
            downloadPeer = null
            //TODO SPV-70 add bun on some time.
            logger.error(ex.message, ex)
        }
    }

    fun acknowledgeAddress(address: AddressPubKey) {
        addressesState.putIfAbsent(address.hash, LedgerContext(
            null, null, null, null
        ))
    }

    private fun requestAddressState() {
        val addresses = spvContext.addressManager.getAll()
        if (addresses.isEmpty()) {
            return
        }
        val request = buildMessage {
            ledgerProofRequest = LedgerProofRequest.newBuilder().apply {
                for (address in addresses) {
                    acknowledgeAddress(address)
                    addAddresses(ByteString.copyFrom(Base58.decode(address.hash)))
                }
            }.build()
        }
        logger.debug("Request address state.")
        for (peer in peers.values) {
            peer.sendMessage(request)
        }
    }

    private fun requestPendingTransactions() {
        val pendingTransactionIds = pendingTransactionContainer.getPendingTransactionIds()
        for (sha256Hash in pendingTransactionIds) {
            val request = VeriBlockMessages.Event.newBuilder()
                .setId(nextMessageId())
                .setAcknowledge(false)
                .setTransactionRequest(
                    VeriBlockMessages.GetTransactionRequest.newBuilder()
                        .setId(ByteString.copyFrom(sha256Hash.bytes))
                        .build()
                )
                .build()
            for (peer in peers.values) {
                peer.sendMessage(request)
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
                logger.debug("Attempting connection to {}:{}", address.address, address.port)
                val peer = try {
                    connectTo(address)
                } catch (e: IOException) {
                    continue
                }
                logger.debug("Discovered peer connected {}:{}", peer.address, peer.port)
            }
        }
    }

    private fun processIncomingMessages() {
        try {
            while (running.get()) {
                try {
                    val (sender, message) = incomingQueue.take()
                    logger.debug { "Processing ${message.resultsCase.name} message from ${sender.address}" }
                    when (message.resultsCase) {
                        ResultsCase.HEARTBEAT -> {
                            val heartbeat = message.heartbeat
                            // Copy reference to local scope
                            val downloadPeer = downloadPeer
                            if (downloadPeer == null && heartbeat.block.number > 0) {
                                startBlockchainDownload(sender)
                            } else if (downloadPeer != null &&
                                heartbeat.block.number - downloadPeer.bestBlockHeight > BLOCK_DIFFERENCE_TO_SWITCH_ON_ANOTHER_PEER
                            ) {
                                startBlockchainDownload(sender)
                            }
                        }
                        ResultsCase.ADVERTISE_BLOCKS -> {
                            val advertiseBlocks = message.advertiseBlocks
                            logger.debug {
                                "Received advertisement of ${advertiseBlocks.headersList.size} blocks, height ${blockchain.getChainHead().height}"
                            }
                            val veriBlockBlocks: List<VeriBlockBlock> = advertiseBlocks.headersList.map {
                                MessageSerializer.deserialize(it)
                            }
                            if (downloadPeer == null && veriBlockBlocks.last().height > 0) {
                                startBlockchainDownload(sender)
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
                                message.transaction
                            )
                            notifyPendingTransactionDownloaded(standardTransaction)
                        }
                        ResultsCase.TX_REQUEST -> {
                            val txIds = message.txRequest.transactionsList.map {
                                Sha256Hash.wrap(
                                    ByteStringUtility.byteStringToHex(it.txId)
                                )
                            }
                            p2PService.onTransactionRequest(txIds, sender)
                        }
                        ResultsCase.LEDGER_PROOF_REPLY -> {
                            val proofReply = message.ledgerProofReply.proofsList
                            val ledgerContexts: List<LedgerContext> = proofReply.asSequence().filter { lpr: LedgerProofResult ->
                                addressesState.containsKey(Base58.encode(lpr.address.toByteArray()))
                            }.filter {
                                LedgerProofReplyValidator.validate(it)
                            }.map {
                                LedgerProofReplyMapper.map(it)
                            }.toList()
                            updateAddressState(ledgerContexts)
                        }
                        ResultsCase.TRANSACTION_REPLY -> if (message.transactionReply.success) {
                            pendingTransactionContainer.updateTransactionInfo(message.transactionReply.transaction.toModel())
                        }
                        else -> {
                            // Ignore the other message types as they are irrelevant for SPV
                        }
                    }
                } catch (e: InterruptedException) {
                    break
                }
            }
        } catch (t: Throwable) {
            logger.error("An unhandled exception occurred processing message queue", t)
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
            if (addressesState.getValue(ledgerContext.address!!.address) > ledgerContext) {
                addressesState.replace(ledgerContext.address.address, ledgerContext)
            }
        }
    }

    private fun notifyPendingTransactionDownloaded(tx: StandardTransaction) {
        EventBus.pendingTransactionDownloadedEvent.trigger(tx)
    }

    fun onPeerConnected(peer: Peer) = lock.withLock {
        logger.debug("Peer {} connected", peer.address)
        pendingPeers.remove(peer.address)
        peers[peer.address] = peer

        // TODO: Wallet related setup (bloom filter)

        // Attach listeners
        peer.setFilter(bloomFilter)

        peer.sendMessage(
            VeriBlockMessages.Event.newBuilder()
                .setStateInfoRequest(VeriBlockMessages.GetStateInfoRequest.getDefaultInstance())
                .build()
        )

        if (downloadPeer == null) {
            startBlockchainDownload(peer)
        }
    }

    fun onPeerDisconnected(peer: Peer) = lock.withLock {
        pendingPeers.remove(peer.address)
        peers.remove(peer.address)
        if (downloadPeer != null && downloadPeer!!.address.equals(peer.address, ignoreCase = true)) {
            downloadPeer = null
        }
    }

    fun onMessageReceived(message: VeriBlockMessages.Event, sender: Peer) {
        try {
            logger.debug("Message Received messageId: {}, from: {}:{}", message.id, sender.address, sender.port)
            incomingQueue.put(NetworkMessage(sender, message))
        } catch (e: InterruptedException) {
            logger.error("onMessageReceived interrupted", e)
        }
    }

    fun advertise(transaction: Transaction) {
        val advertise = VeriBlockMessages.Event.newBuilder()
            .setId(nextMessageId())
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
                peer.sendMessage(advertise)
            } catch (ex: Exception) {
                logger.error(ex.message, ex)
            }
        }
    }

    suspend fun requestMessage(
        event: VeriBlockMessages.Event,
        timeoutInMillis: Long = 5000L
    ): VeriBlockMessages.Event = withTimeout(timeoutInMillis) {
        // Create a flow that emits in execution order
        val executionOrderFlow = flow {
            // Open a select scope for being able to call onAwait concurrently for all peers
            select {
                // Perform the request for all the peers asynchronously
                // TODO: consider a less expensive approach such as asking a random peer. There can be peer behavior score weighting and/or retries.
                for (peer in peers.values) {
                    async {
                        peer.requestMessage(event, timeoutInMillis)
                    }.onAwait {
                        // Emit in the flow on completion, so the first one to complete will get the other jobs cancelled
                        emit(it)
                    }
                }
            }
        }
        // Choose the first one to complete
        executionOrderFlow.first()
    }

    fun getSignatureIndex(address: String): Long? {
        return addressesState[address]?.ledgerValue?.signatureIndex
    }

    fun getAvailablePeers(): Int = peers.size

    fun getBestBlockHeight(): Int = peers.values.maxOfOrNull {
        it.bestBlockHeight
    } ?: 0

    fun getDownloadStatus(): DownloadStatusResponse {
        val status: DownloadStatus
        val currentHeight = blockchain.getChainHead().height
        val bestBlockHeight = downloadPeer?.bestBlockHeight ?: 0
        status = when {
            downloadPeer == null || bestBlockHeight == 0 ->
                DownloadStatus.DISCOVERING
            bestBlockHeight - currentHeight < AMOUNT_OF_BLOCKS_WHEN_WE_CAN_START_WORKING ->
                DownloadStatus.READY
            else ->
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

private fun VeriBlockMessages.TransactionInfo.toModel() = TransactionInfo(
    confirmations = confirmations,
    transaction = transaction.toModel(),
    blockNumber = blockNumber,
    timestamp = timestamp,
    endorsedBlockHash = endorsedBlockHash.toHex(),
    bitcoinBlockHash = bitcoinBlockHash.toHex(),
    bitcoinTxId = bitcoinTxId.toHex(),
    bitcoinConfiormations = bitcoinConfirmations,
    blockHash = blockHash.toHex(),
    merklePath = merklePath
)

private fun VeriBlockMessages.Transaction.toModel() = TransactionData(
    type = TransactionType.valueOf(type.name),
    sourceAddress = sourceAddress.toHex(),
    sourceAmount = sourceAmount,
    outputs = outputsList.map { it.toModel() },
    transactionFee = transactionFee,
    data = data.toHex(),
    bitcoinTransaction = bitcoinTransaction.toHex(),
    endorsedBlockHeader = endorsedBlockHeader.toHex(),
    bitcoinBlockHeaderOfProof = "",
    merklePath = merklePath,
    contextBitcoinBlockHeaders = listOf(),
    timestamp = timestamp,
    size = size,
    txId = Sha256Hash.wrap(txId.toHex())
)

private fun VeriBlockMessages.Output.toModel() = OutputData(
    address = address.toHex(),
    amount = amount
)
