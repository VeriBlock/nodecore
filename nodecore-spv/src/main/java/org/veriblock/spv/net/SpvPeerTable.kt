// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.spv.net

import com.google.protobuf.ByteString
import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.aSocket
import io.ktor.util.network.NetworkAddress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import nodecore.api.grpc.RpcAdvertiseTransaction
import nodecore.api.grpc.RpcEvent
import nodecore.api.grpc.RpcEvent.ResultsCase
import nodecore.api.grpc.RpcGetStateInfoRequest
import nodecore.api.grpc.RpcNetworkInfoRequest
import nodecore.api.grpc.RpcTransactionAnnounce
import nodecore.api.grpc.utilities.ByteStringUtility
import org.veriblock.core.crypto.BloomFilter
import org.veriblock.core.crypto.asVbkTxId
import org.veriblock.core.utilities.createLogger
import org.veriblock.core.utilities.debugError
import org.veriblock.sdk.models.VeriBlockBlock
import org.veriblock.spv.SpvContext
import org.veriblock.spv.model.DownloadStatus
import org.veriblock.spv.model.DownloadStatusResponse
import org.veriblock.spv.model.NetworkMessage
import org.veriblock.spv.model.NodeMetadata
import org.veriblock.spv.model.StandardTransaction
import org.veriblock.spv.model.Transaction
import org.veriblock.spv.model.TransactionTypeIdentifier
import org.veriblock.spv.serialization.MessageSerializer
import org.veriblock.spv.serialization.MessageSerializer.deserializeNormalTransaction
import org.veriblock.spv.service.Blockchain
import org.veriblock.spv.service.PendingTransactionContainer
import org.veriblock.spv.util.SpvEventBus
import org.veriblock.spv.util.Threading
import org.veriblock.spv.util.Threading.PEER_TABLE_DISPATCHER
import org.veriblock.spv.util.Threading.PEER_TABLE_SCOPE
import org.veriblock.spv.util.buildMessage
import org.veriblock.spv.util.invokeOnFailure
import org.veriblock.spv.util.launchWithFixedDelay
import java.io.IOException
import java.nio.channels.ClosedChannelException
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

private val logger = createLogger {}

const val DEFAULT_CONNECTIONS = 12
const val BLOOM_FILTER_TWEAK = 710699166
const val BLOOM_FILTER_FALSE_POSITIVE_RATE = 0.02
const val BLOCK_DIFFERENCE_TO_SWITCH_ON_ANOTHER_PEER = 200
const val AMOUNT_OF_BLOCKS_WHEN_WE_CAN_START_WORKING = 4//50

class SpvPeerTable(
    private val spvContext: SpvContext,
    private val p2pService: P2PService,
    peerDiscovery: PeerDiscovery,
    pendingTransactionContainer: PendingTransactionContainer
) {
    private val lock = ReentrantLock()
    private val running = AtomicBoolean(false)
    private val discovery: PeerDiscovery
    private val blockchain: Blockchain

    var maximumPeers = DEFAULT_CONNECTIONS
    var downloadPeer: SpvPeer? = null
    val bloomFilter: BloomFilter
    private val pendingTransactionContainer: PendingTransactionContainer

    private val peers = ConcurrentHashMap<NetworkAddress, SpvPeer>()
    private val pendingPeers = ConcurrentHashMap<NetworkAddress, SpvPeer>()
    private val peersFromPeers = HashSet<NetworkAddress>()

    // incoming queue must be bounded, otherwise it's easy to DOS SPV
    private val incomingQueue: Channel<NetworkMessage> = Channel(10000)

    private val hashDispatcher = Threading.HASH_EXECUTOR.asCoroutineDispatcher();

    private val selectorManager = ActorSelectorManager(PEER_TABLE_DISPATCHER)

    init {
        bloomFilter = createBloomFilter()
        blockchain = spvContext.blockchain
        discovery = peerDiscovery
        this.pendingTransactionContainer = pendingTransactionContainer

        SpvEventBus.pendingTransactionDownloadedEvent.register(
            spvContext.pendingTransactionDownloadedListener,
            spvContext.pendingTransactionDownloadedListener::onPendingTransactionDownloaded
        )
    }

    fun start() {
        running.set(true)

        SpvEventBus.peerConnectedEvent.register(this, ::onPeerConnected)
        SpvEventBus.peerDisconnectedEvent.register(this, ::onPeerDisconnected)
        SpvEventBus.messageReceivedEvent.register(this) {
            onMessageReceived(it.message, it.peer)
        }

        PEER_TABLE_SCOPE.launchWithFixedDelay(200L, 60_000L) {
            discoverPeers()
        }.invokeOnFailure { t ->
            logger.debugError(t) { "The peer discovery task has failed" }
        }

        spvContext.startPendingTransactionsUpdateTask()
        spvContext.startAddressStateUpdateTask()

        // Scheduling with a fixed delay allows it to recover in the event of an unhandled exception
        val messageHandlerScope = CoroutineScope(Threading.MESSAGE_HANDLER_THREAD.asCoroutineDispatcher())
        messageHandlerScope.launch {
            processIncomingMessages()
        }.invokeOnFailure { t ->
            logger.error(t) { "There was an error while processing peer messages. Exiting process..." }
        }
    }

    private fun onNetworkInfoReply(reply: RpcNetworkInfoReply) {
        reply.availableNodesList.asSequence()
            .filter { !peers.any { peer ->
                peer.key.hostname == it.address }
            }
            .filter { !pendingPeers.any { peer ->
                peer.key.hostname == it.address }
            }
            .forEach {
                peersFromPeers.add(NetworkAddress(it.address, it.port))
            }
    }

    fun shutdown() {
        running.set(false)

        // Close peer connections
        incomingQueue.close()
        pendingPeers.clear()
        peers.forEachValue(4) {
            it.closeConnection()
        }
    }

    suspend fun connectTo(address: NetworkAddress): SpvPeer {
        peers[address]?.let {
            return it
        }
        val socket = try {
            aSocket(selectorManager)
                .tcp()
                .connect(address)
        } catch (e: IOException) {
            logger.debug("Unable to open connection to $address", e)
            throw e
        }
        val peer = createPeer(socket)
        lock.withLock {
            pendingPeers[address] = peer
        }
        return peer
    }

    fun createPeer(socket: Socket): SpvPeer {
        return SpvPeer(spvContext, blockchain, NodeMetadata, socket)
    }

    fun startBlockchainDownload(peer: SpvPeer) {
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

    private suspend fun discoverPeers() {
        if (maximumPeers > 0 && getConnectedPeerCount() >= maximumPeers) {
            return
        }

        // Connect to bootstrap peers
        connectToPeers(discovery.getPeers())

        // Check if we still need peers
        val remainingNeededPeers = getNeededPeers()
        if (peers.isNotEmpty() && remainingNeededPeers > 0) {
            // If so, request to the known peers their known peers
            logger.debug("Requesting the peer list to all peers")
            val message = buildMessage {
                networkInfoRequest = RpcNetworkInfoRequest.newBuilder().build()
            }
            requestAllMessages(message).collect { msg ->
                val peersFromPeer = msg.networkInfoReply.availableNodesList.map {
                    NetworkAddress(it.address, it.port)
                }
                connectToPeers(peersFromPeer)
            }
        }
    }

    private suspend fun connectToPeers(addresses: Collection<NetworkAddress>, neededPeers: Int = getNeededPeers()) {
        logger.debug { "We need $neededPeers peers more" }
        val newPeers = addresses.asSequence()
            // first, filter out known peers
            .filter { !peers.containsKey(it) && !pendingPeers.containsKey(it) }
            // shuffle ALL new peers
            .shuffled()
            // then take needed amount
            .take(neededPeers)
        for (address in newPeers) {
            connectToPeer(address)
        }
    }

    private fun getNeededPeers() =
        maximumPeers - (getConnectedPeerCount() + getPendingPeerCount())

    private suspend fun connectToPeer(address: NetworkAddress) = try {
        logger.debug("Attempting connection to $address")
        val peer = connectTo(address)
        logger.debug("Discovered peer connected $address, its best height=${peer.bestBlockHeight}")
    } catch (e: IOException) {
        // Ignored
        logger.debug(e) { "Unable to connect to the peer $address" }
    }

    suspend fun processIncomingMessages() {
        try {
            for ((sender, message) in incomingQueue) {
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
                            val lastBlock = MessageSerializer.deserialize(advertiseBlocks.headersList.last())
                            "Received advertisement of ${advertiseBlocks.headersList.size} blocks," +
                                " height: ${lastBlock.height}"
                        }
                        val trustHashes = spvContext.trustPeerHashes && advertiseBlocks.headersList.size > 10
                        val veriBlockBlocks: List<VeriBlockBlock> = coroutineScope {
                            advertiseBlocks.headersList.map {
                                async(hashDispatcher) {
                                    val block = MessageSerializer.deserialize(it, trustHashes)
                                    // pre-calculate hash in parallel
                                    block.hash
                                    block
                                }
                            }.awaitAll()
                        }
                        if (downloadPeer == null && veriBlockBlocks.last().height > 0) {
                            startBlockchainDownload(sender)
                        }

                        val allBlocksAccepted = veriBlockBlocks
                            .sortedBy { it.height }
                            .all { blockchain.acceptBlock(it) }

                        // TODO(warchant): if allBlocksAccepted == false here, block can not be connected or invalid
                        // maybe ban peer? for now, do nothing
                    }
                    ResultsCase.TRANSACTION -> {
                        // TODO: Different Transaction types
                        val standardTransaction = deserializeNormalTransaction(message.transaction)
                        notifyPendingTransactionDownloaded(standardTransaction)
                    }
                    ResultsCase.TX_REQUEST -> {
                        val txIds = message.txRequest.transactionsList.map {
                            ByteStringUtility.byteStringToHex(it.txId).asVbkTxId()
                        }
                        p2pService.onTransactionRequest(txIds, sender)
                    }
                    else -> {
                        // Ignore the other message types as they are irrelevant for SPV
                    }
                }
            }
        } catch (ignored: ClosedReceiveChannelException) {
            logger.info("Stopping peer table message handling task...")
            return
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

    private fun notifyPendingTransactionDownloaded(tx: StandardTransaction) {
        SpvEventBus.pendingTransactionDownloadedEvent.trigger(tx)
    }

    fun onPeerConnected(peer: SpvPeer) = lock.withLock {
        logger.debug("Peer {} connected", peer.address)
        pendingPeers.remove(peer.address)
        peers[peer.address] = peer

        // TODO: Wallet related setup (bloom filter)

        // Attach listeners
        peer.setFilter(bloomFilter)

        peer.sendMessage {
            stateInfoRequest = RpcGetStateInfoRequest.getDefaultInstance()
        }

        if (downloadPeer == null) {
            startBlockchainDownload(peer)
        }
    }

    fun onPeerDisconnected(peer: SpvPeer) = lock.withLock {
        pendingPeers.remove(peer.address)
        peers.remove(peer.address)
        if (downloadPeer?.address?.equals(peer.address) == true) {
            downloadPeer = null
        }
    }

    private fun onMessageReceived(message: RpcEvent, sender: SpvPeer) {
        try {
            logger.debug("Message Received messageId: ${message.id}, from: ${sender.address}")
            incomingQueue.offer(NetworkMessage(sender, message))
        } catch (e: InterruptedException) {
            logger.error("onMessageReceived interrupted", e)
        } catch (e: ClosedChannelException) {
            logger.error("onMessageReceived interrupted", e)
        }
    }

    fun advertise(transaction: Transaction) {
        val advertise = buildMessage {
            advertiseTx = RpcAdvertiseTransaction.newBuilder()
                .addTransactions(
                    RpcTransactionAnnounce.newBuilder()
                        .setType(
                            if (transaction.transactionTypeIdentifier === TransactionTypeIdentifier.PROOF_OF_PROOF) {
                                RpcTransactionAnnounce.Type.PROOF_OF_PROOF
                            } else {
                                RpcTransactionAnnounce.Type.NORMAL
                            }
                        )
                        .setTxId(ByteString.copyFrom(transaction.txId.bytes))
                        .build()
                )
                .build()
        }
        for (peer in peers.values) {
            try {
                peer.sendMessage(advertise)
            } catch (ex: Exception) {
                logger.error(ex.message, ex)
            }
        }
    }

    fun requestAllMessages(
        event: RpcEvent,
        timeoutInMillis: Long = 5000L
    ): Flow<RpcEvent> = channelFlow {
        // Perform the request for all the peers asynchronously
        // TODO: consider a less expensive approach such as asking a random peer. There can be peer behavior score weighting and/or retries.
        peers.values.map { peer ->
            // Launch each request in a child coroutine
            launch {
                val response = peer.requestMessage(event, timeoutInMillis)
                // "emit" the response to the channel flow
                logger.debug { "Received response from peer=${peer.address} response=${response.resultsCase.name}" }
                send(response)
            }
        }.joinAll() // Wait for requests to be done or cancelled before closing the flow
    }

    suspend fun requestMessage(
        event: RpcEvent,
        timeoutInMillis: Long = 5000L
    ): RpcEvent = withTimeout(timeoutInMillis) {
        // Create a flow that emits in execution order
        val allMessagesFlow = requestAllMessages(event, timeoutInMillis)
        // Choose the first one to complete
        allMessagesFlow.first()
    }

    suspend fun requestMessage(
        event: RpcEvent,
        timeoutInMillis: Long = 5000L,
        quantifier: ((RpcEvent) -> Int)
    ): RpcEvent = coroutineScope {
        // Perform the request for all the peers asynchronously
        peers.values.map {
            async {
                try {
                    it.requestMessage(event, timeoutInMillis)
                } catch (e: TimeoutCancellationException) {
                    null
                }
            }
        }.awaitAll()
            .filterNotNull()
            .maxByOrNull { quantifier(it) }
            ?: error("All requests timed out ($timeoutInMillis ms)")
    }

    fun getAvailablePeers(): Int = peers.size

    fun getBestBlockHeight(): Int = peers.values.maxOfOrNull {
        it.bestBlockHeight
    } ?: 0

    fun getDownloadStatus(): DownloadStatusResponse {
        val status: DownloadStatus
        val currentHeight = blockchain.activeChain.tip.height
        val bestBlockHeight = downloadPeer?.bestBlockHeight ?: 0
        status = when {
            downloadPeer == null || (currentHeight == 0 && bestBlockHeight == 0) ->
                DownloadStatus.DISCOVERING
            bestBlockHeight > 0 && bestBlockHeight - currentHeight < AMOUNT_OF_BLOCKS_WHEN_WE_CAN_START_WORKING ->
                DownloadStatus.READY
            else ->
                DownloadStatus.DOWNLOADING
        }
        return DownloadStatusResponse(status, currentHeight, bestBlockHeight)
    }

    fun getConnectedPeers(): Collection<SpvPeer> = Collections.unmodifiableCollection(peers.values)

    fun getConnectedPeerCount(): Int {
        return peers.size
    }

    fun getPendingPeerCount(): Int {
        return pendingPeers.size
    }
}
