// VeriBlock NodeCore
// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.p2p

import com.google.common.net.InetAddresses
import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.aSocket
import io.ktor.util.network.NetworkAddress
import io.ktor.util.network.port
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import nodecore.api.grpc.RpcAnnounce
import nodecore.api.grpc.RpcEvent
import nodecore.api.grpc.RpcNetworkInfoRequest
import org.veriblock.core.launchWithFixedDelay
import org.veriblock.core.params.NetworkParameters
import org.veriblock.core.utilities.Utility
import org.veriblock.core.utilities.createLogger
import java.io.IOException
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.toList

private val logger = createLogger {}

class PeerTable(
    configuration: P2pConfiguration,
    private val warden: PeerWarden,
    private val bootstrapper: PeerTableBootstrapper
) {
    val socketDispatcher = Threading.PEER_SERVER_THREAD.asCoroutineDispatcher()
    val selectorManager = ActorSelectorManager(socketDispatcher)

    private val coroutineDispatcher = Threading.PEER_TABLE_POOL.asCoroutineDispatcher()
    private val coroutineScope = CoroutineScope(coroutineDispatcher)

    private val networkParameters: NetworkParameters = configuration.networkParameters
    private val bootstrapPeerLimit: Int = configuration.bootstrapLimit
    private val minimumPeerCount: Int = configuration.peerMinCount
    private val maximumPeerCount: Int = configuration.peerMaxCount
    private val bootstrapEnabled: Boolean = configuration.peerBootstrapEnabled

    val self: NodeMetadata = NodeMetadata(
        address = configuration.peerPublishAddress,
        port = configuration.peerBindPort,
        application = configuration.fullProgramNameVersion,
        protocolVersion = networkParameters.protocolVersion,
        platform = if (configuration.peerSharePlatform) P2pConstants.PLATFORM else "",
        startTimestamp = Utility.getCurrentTimeSeconds(),
        canShareAddress = configuration.peerShareMyAddress,
        capabilities = configuration.capabilities,
        id = UUID.randomUUID().toString()
    )

    private val capabilitiesMapper: PeerCapabilities.(Peer) -> PeerCapabilities = configuration.capabilitiesMapper
    private val neededCapabilities: PeerCapabilities = configuration.neededCapabilities

    private var externalPeers: MutableList<NetworkAddress> = (configuration.externalPeerEndpoints + bootstrapper.getNext(
        bootstrapPeerLimit
    )).toMutableList()
    private val connectOnlyToExternal = configuration.connectOnlyToExternal

    private val peers: MutableMap<String, Peer> = ConcurrentHashMap()
    private val peerCandidates: MutableMap<String, NodeMetadata> = ConcurrentHashMap()
    private val denylist: MutableMap<String, Ban> = ConcurrentHashMap()
    private val doNotConnect: MutableMap<String, Int> = ConcurrentHashMap()

    private var onConnected: Runnable? = null
    private var onDisconnected: Runnable? = null

    init {
        // Reserve space for incoming connections
        if (maximumPeerCount <= minimumPeerCount) {
            logger.warn("The configured peer counts make it impossible to accept incoming connections")
        }

        P2pEventBus.externalPeerAdded.register(this, ::onExternalPeerAdded)
        P2pEventBus.externalPeerRemoved.register(this, ::onExternalPeerRemoved)
        P2pEventBus.peerBanned.register(this, ::onPeerBanned)
        P2pEventBus.peerDisconnected.register(this, ::onPeerDisconnected)
    }

    fun initialize(onConnected: Runnable?, onDisconnected: Runnable?) {
        logger.info("Initializing peer table")
        this.onConnected = onConnected
        this.onDisconnected = onDisconnected
        // Introduce a bit of an initial delay when starting so that any nodes with a connect attempt to this node queued
        // have an opportunity to succeed first
        coroutineScope.launch {
            delay(20_000L)
            establishConnectionWithConfiguredPeers(externalPeers)
        }
        if (!connectOnlyToExternal) {
            coroutineScope.launch {
                delay(35_000L)
                val candidates = requestPeerTables()
                addPeerCandidates(candidates)
            }
        }
        coroutineScope.launchWithFixedDelay(60_000L, 60_000L) {
            upkeep()
        }
    }

    fun shutdown() {
        Threading.PEER_TABLE_POOL.safeShutdown()
        warden.shutdown()

        for (peer in peers.values) {
            peer.disconnect()
        }
    }

    private suspend fun establishConnectionWithConfiguredPeers(peers: List<NetworkAddress>, bootstrap: Boolean = false) {
        // Filter out ones that have already been added
        val filtered = peers.filter {
            !this.peers.containsKey(it.addressKey)
        }

        if (bootstrap) {
            logger.info { "Establishing connection with ${filtered.size} / ${peers.size} bootstrap peers." }
        }

        // Attempt to connect with the peer and add if successful
        for (address in filtered) {
            attemptOutboundPeerConnect(address.address, address.port)
        }
    }

    suspend fun attemptOutboundPeerConnect(address: String, port: Int): Peer? {
        val peer = createOutboundPeer(address, port)
        // TODO: Retry connections?
            ?: return null

        addPeer(peer)

        return peer
    }

    private suspend fun createOutboundPeer(address: String, port: Int): Peer? {
        try {
            logger.debug { "Attempting to establish client connection to peer $address:$port" }
            val socketAddress = NetworkAddress(address, port)
            val socket = aSocket(selectorManager)
                .tcp()
                .connect(socketAddress)
            return Peer(selectorManager, socket, true)
        } catch (e: IOException) {
            logger.debug(e) { "Unable to open connection to $address:$port!" }
        }
        return null
    }

    private suspend fun requestPeerTables(): List<NodeMetadata> {
        val event = buildMessage {
            networkInfoRequest = RpcNetworkInfoRequest.newBuilder().build()
        }

        return requestAllMessages(event)
            .flatMapConcat { reply ->
                reply.networkInfoReply.availableNodesList
                    .mapNotNull { it.toModel().asAcceptableCandidate() }
                    .asFlow()
            }
            .distinctUntilChangedBy { it.addressKey }
            .toList()
    }

    fun registerIncomingConnection(socket: Socket) {
        val hostAddress = socket.remoteAddress.address
        if (peers.size >= maximumPeerCount) {
            logger.info("Maximum amount of peers reached, rejecting connection")
            socket.close()
            return
        }
        if (denylist.containsKey(hostAddress)) {
            logger.info { "Incoming connection is from a denylisted peer ($hostAddress), closing connection" }
            socket.close()
            return
        }

        // FIXME this key may not be enough (multiple peers may connect from the same IP and port)
        val addressKey = socket.remoteAddress.addressKey
        val original = peers[addressKey]
        if (original != null) {
            if (original.status != Peer.Status.Closed && original.status != Peer.Status.Errored) {
                logger.info { "Incoming connection is from an already connected peer ($addressKey), closing connection" }
                socket.close()
                return
            }
        }

        var peer: Peer? = null
        try {
            peer = Peer(selectorManager, socket, false)
            addPeer(peer)

            announce(peer, true)
            logger.info { "New peer ${peer.address} connected" }
        } catch (e: Exception) {
            logger.error("Error handling incoming peer", e)
            peer?.disconnect()
        }
    }

    fun announce(target: Peer, requestReply: Boolean = false) {
        target.send(buildMessage {
            announce = RpcAnnounce.newBuilder().apply {
                reply = requestReply
                val capabilitiesToSend = self.capabilities.capabilitiesMapper(target)
                nodeInfo = self.toRpcNodeInfo(c = capabilitiesToSend)
            }.build()
        })
    }

    fun getConnectedPeers(): List<Peer> = peers.values.filter {
        it.status == Peer.Status.Connected
    }

    fun getDisconnectedPeers(): List<Peer> = peers.values.filter {
        it.status != Peer.Status.Connected
    }

    /**
     * Returns an acceptable candidate or null if it's not acceptable
     */
    private fun NodeMetadata.asAcceptableCandidate(): NodeMetadata? {
        // TODO: Remove once this is no longer needed
        val candidate = if (port !in 0..50000) {
            copy(port = networkParameters.p2pPort)
        } else {
            this
        }

        if (isNodeSelf(candidate.id, candidate.address) || isAddressPrivate(candidate.address)) {
            return null
        }

        if (peers.containsKey(candidate.addressKey)) {
            return null
        }

        return candidate
    }

    private fun addPeerCandidates(candidates: List<NodeMetadata>) {
        for (candidate in candidates) {
            peerCandidates[candidate.addressKey] = candidate
            logger.debug { "Added ${candidate.addressKey} as a peer candidate" }
        }
    }

    fun getPeerCandidates(): List<NodeMetadata> {
        return ArrayList(peerCandidates.values)
    }

    fun clearBans() {
        denylist.clear()
    }

    fun updatePeer(peer: Peer) {
        peers.replace(peer.addressKey, peer)
    }

    private fun banAddress(address: String) {
        // Remove all peers connected from that address
        peers.entries.asSequence().filter {
            it.value.address == address
        }.forEach {
            removePeer(it.key)
        }
        // Add that address to the denylist
        denylist[address] = createTemporaryBan(address)
    }

    private fun addPeer(peer: Peer) {
        if (isNodeSelf(peer.id, peer.address)) {
            logger.debug { "${peer.address} cannot be added as a peer because it is this node" }
            return
        }
        if (denylist.containsKey(peer.address)) {
            logger.info { "${peer.address} is currently denylisted and cannot be added" }
        }

        if (peers.isEmpty()) {
            onConnected?.run()
        }

        peers[peer.addressKey] = peer

        logger.debug { "Added peer ${peer.addressKey}" }
    }

    private fun removePeer(addressKey: String) {
        val peer = peers.remove(addressKey)
        peer?.disconnect()

        peerCandidates.remove(addressKey)
        logger.debug { "Removed $addressKey from peer list" }

        if (getConnectedPeers().isEmpty()) {
            onDisconnected?.run()
        }
    }

    private suspend fun upkeep() {
        try {
            groomPeers()
            releaseDoNotConnect()
            logger.debug("Ensure min peers...")
            ensureMinimumConnectedPeers()
            if (getConnectedPeers().isEmpty()) {
                logger.info("After attempting to ensure minimum peers we still have 0, removing all do-not-connect constraints...")
                // TODO: revisit clearing DNC
                doNotConnect.clear()
                ensureMinimumConnectedPeers()
                logger.info { "After removing do-not-connect constraints, we have ${getConnectedPeers().size} available peers." }
            }
            logger.debug("Releasing bans...")
            releaseExpiredBans()
        } catch (e: Exception) {
            logger.error(e.message, e)
        }
    }

    private fun groomPeers() {
        val peerList = ArrayList(peers.values)
        for (p in peerList) {
            if (p.state.lastMessageReceivedAt < Utility.getCurrentTimeSeconds() - P2pConstants.PEER_TIMEOUT) {
                logger.info { "Removing peer ${p.address} because it has sent no messages in the last ${P2pConstants.PEER_TIMEOUT} seconds" }
                removePeer(p.addressKey)
            }
        }
    }

    suspend fun ensureMinimumConnectedPeers() {
        val available = getConnectedPeers()
        // Take into account only the peers that have our needed capabilities
        val peerCount = available.count { it.capabilities.hasCapabilities(neededCapabilities) }
        if (peerCount >= minimumPeerCount) {
            logger.debug("Minimum peer threshold met!")
            return
        }

        if (peerCandidates.isEmpty()) {
            logger.debug("No peer candidates...")
            if (peerCount > 0 && !connectOnlyToExternal) {
                logger.info("Requesting peer tables to $peerCount peers.")
                val candidates = requestPeerTables()
                if (candidates.isNotEmpty()) {
                    addPeerCandidates(candidates)
                    return
                }
            }
            if (externalPeers.isNotEmpty()) {
                establishConnectionWithConfiguredPeers(externalPeers)
            } else if (bootstrapEnabled) {
                val morePeers = bootstrapper.getNext(bootstrapPeerLimit)
                establishConnectionWithConfiguredPeers(morePeers, true)
                return
            } else {
                logger.info("bootstrap not enabled and no peers!")
            }
        }

        logger.debug { "Found ${peerCandidates.size} peer candidates" }

        val candidates: List<NodeMetadata> = peerCandidates.values.shuffled()

        var peerAttempts = 6
        for (candidate in candidates) {
            if (getConnectedPeers().size < minimumPeerCount && peerAttempts > 0) {
                peerCandidates.remove(candidate.addressKey)

                // Skip if it's already an established peer, waiting to connect or in the do not connect list
                if (peers.containsKey(candidate.addressKey) || doNotConnect.containsKey(candidate.address)) {
                    continue
                }

                logger.debug { "Attempting to connect with ${candidate.address}:${candidate.port}" }
                val peer = attemptOutboundPeerConnect(candidate.address, candidate.port)
                if (peer != null) {
                    peer.metadata = candidate
                }

                peerAttempts--
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
    ): RpcEvent? = withTimeout(timeoutInMillis) {
        // Create a flow that emits in execution order
        val allMessagesFlow = requestAllMessages(event, timeoutInMillis)
        // Choose the first one to complete
        allMessagesFlow.firstOrNull()
    }

    suspend fun requestMessage(
        event: RpcEvent,
        timeoutInMillis: Long = 5000L,
        neededCapability: PeerCapabilities.Capability? = null,
        quantifier: ((RpcEvent) -> Int)
    ): RpcEvent = coroutineScope {
        // Perform the request for all the peers asynchronously
        val peerToRequest = if (neededCapability != null) {
            peers.values.filter { it.capabilities.hasCapability(neededCapability) }
        } else {
            peers.values
        }
        peerToRequest.map {
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
            ?: throw NoPeersException("No peers were able to fulfill the request")
    }

    private fun releaseDoNotConnect() {
        doNotConnect.values.removeIf { Utility.getCurrentTimeSeconds() >= it }
    }

    private fun releaseExpiredBans() {
        denylist.values.removeIf { it.isExpired }
    }

    private fun isNodeSelf(id: String, address: String): Boolean {
        return self.id == id || self.address == address
    }

    private fun isAddressPrivate(address: String): Boolean {
        if (InetAddresses.isInetAddress(address)) {
            val ipAddress = InetAddresses.forString(address)
            return ipAddress.isSiteLocalAddress || ipAddress.isLoopbackAddress
        }
        return false
    }

    private fun onExternalPeerAdded(addedPeer: NetworkAddress) {
        try {
            externalPeers.add(addedPeer)
            coroutineScope.launch {
                establishConnectionWithConfiguredPeers(listOf(addedPeer))
            }
        } catch (e: Exception) {
            logger.error("Exception occurred adding external peer", e)
        }
    }

    private fun onExternalPeerRemoved(removedPeer: NetworkAddress) {
        try {
            externalPeers.removeIf { it.address == removedPeer.address }
            removePeer(removedPeer.addressKey)
        } catch (e: Exception) {
            logger.error("Exception occurred removing external peer", e)
        }
    }

    private fun onPeerBanned(peer: Peer) {
        banAddress(peer.address)
    }

    private fun onPeerDisconnected(peer: Peer) {
        removePeer(peer.addressKey)
    }
}

class NoPeersException(override val message: String) : RuntimeException()
