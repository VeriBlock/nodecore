// VeriBlock NodeCore
// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.p2p

import com.google.protobuf.InvalidProtocolBufferException
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.aSocket
import io.ktor.util.network.NetworkAddress
import io.ktor.util.network.port
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withTimeout
import nodecore.api.grpc.RpcEvent
import nodecore.p2p.event.PeerMisbehaviorEvent
import java.lang.Exception
import java.nio.channels.CancelledKeyException
import org.veriblock.core.crypto.BloomFilter
import org.veriblock.core.utilities.Utility
import org.veriblock.core.utilities.createLogger
import java.util.concurrent.ConcurrentHashMap

private val logger = createLogger {}

class Peer(
    private val selectorManager: SelectorManager,
    socket: Socket,
    ownsConnection: Boolean
) {
    enum class Status {
        Connected,
        Errored,
        Closed
    }

    var address: String = socket.remoteAddress.address
    var port: Int = socket.remoteAddress.port

    val addressKey: String
        get() = "$address:$port"

    var reconnectPort: Int? = null

    var metadata = NodeMetadata()

    val protocolVersion get() = metadata.protocolVersion
    val canShareAddress get() = metadata.canShareAddress
    val capabilities get() = metadata.capabilities
    val id: String get() = metadata.id

    var status: Status = Status.Connected

    var state: PeerState = PeerState()

    var filter: BloomFilter? = null

    private var socketHandler = PeerSocketHandler(this, socket, ownsConnection)

    /**
     * Expected responses from message requests see [requestMessage]
     */
    private val expectedResponses: MutableMap<String, Channel<RpcEvent>> = ConcurrentHashMap()

    fun disconnect() {
        if (status == Status.Closed || status == Status.Errored) {
            return
        }

        status = Status.Closed
        P2pEventBus.peerDisconnected.trigger(this)
        socketHandler.stop()
    }

    suspend fun reconnect(): Boolean {
        return try {
            val socketAddress = NetworkAddress(address, port)
            
            // Stop the current connection
            socketHandler.stop()
            
            // Get a new socket
            val socket = aSocket(selectorManager)
                .tcp()
                .connect(socketAddress)

            // Reset basic info
            address = socket.remoteAddress.address
            port = socket.remoteAddress.port
            state = PeerState()
            status = Status.Connected
            socketHandler = PeerSocketHandler(this, socket, true)
            true
        } catch (e: Exception) {
            logger.debug(e) { "Unable to open connection to $address:$port!" }
            false
        }
    }
    
    var lastSuccessfulSend = System.currentTimeMillis()

    fun send(message: RpcEvent): Boolean {
        if (status == Status.Closed || status == Status.Errored) {
            logger.warn { "Peer $address is not in a state for sending, message will not be sent" }
            return false
        }

        logger.trace { "Attempting to send ${message.resultsCase.name} to peer $address" }
        return try {
            if (System.currentTimeMillis() - 60000 > lastSuccessfulSend) {
                logger.info {
                    "Note: peer $address has not had a successful write in ${System.currentTimeMillis() / 1000 - lastSuccessfulSend / 1000} seconds"
                }
            }

            if (System.currentTimeMillis() - 1000 * 5 * 60 > lastSuccessfulSend) {
                logger.warn { "Peer $address has not had a successful write in over 5 minutes, closing connection..." }
                try {
                    disconnect()
                    return false
                } catch (ignored: Exception) {
                }
            }

            socketHandler.write(message)
            true
        } catch (e: CancelledKeyException) {
            logger.warn { "Key has been cancelled for $address!" }
            disconnect()
            false
        } catch (e: Exception) {
            logger.warn(e) { "Unable to send message to $address" }
            throw e
        }
    }
    
    fun handleMessage(rawMessage: ByteArray) {
        val event: RpcEvent
        try {
            event = RpcEvent.parseFrom(rawMessage)
            logger.debug { "Received event of type ${event.resultsCase.name}" }
        } catch (e: InvalidProtocolBufferException) {
            P2pEventBus.peerMisbehavior.trigger(PeerMisbehaviorEvent(
                peer = this,
                reason = PeerMisbehaviorEvent.Reason.MALFORMED_EVENT,
                message = "Malformed event: ${Utility.bytesToHex(rawMessage)}"
            ))
            return
        }

        state.lastMessageReceivedAt = Utility.getCurrentTimeSeconds()

        // Handle as an expected response if possible
        expectedResponses[event.requestId]?.offer(event)

        // Broadcast to event bus
        P2pEventBus.newEvent(event, this)
    }

    /**
     * Sends a P2P request and waits for the peer to respond it during the given timeout (or a default of 5 seconds).
     * This applies to Request/Response event type pairs.
     */
    suspend fun requestMessage(
        request: RpcEvent,
        timeoutInMillis: Long = 5000L
    ): RpcEvent = try {
        // Create conflated channel
        val expectedResponseChannel = Channel<RpcEvent>(Channel.CONFLATED)
        // Set this channel as the expected response for the request id
        logger.debug { "Expecting a response to ${request.resultsCase.name} from $address" }
        expectedResponses[request.id] = expectedResponseChannel
        // Send the request
        send(request)
        // Wait until the expected response arrives (or times out)
        withTimeout(timeoutInMillis) {
            expectedResponseChannel.receive()
        }
    } finally {
        // Unregister the channel
        expectedResponses.remove(request.id)
    }
}

inline fun Peer.sendMessage(crossinline buildBlock: RpcEvent.Builder.() -> Unit) = send(
    buildMessage(buildBlock = buildBlock)
)

suspend inline fun Peer.requestMessage(
    timeoutInMillis: Long = 5000L,
    crossinline buildBlock: RpcEvent.Builder.() -> Unit
) = requestMessage(
    buildMessage(buildBlock = buildBlock),
    timeoutInMillis
)
