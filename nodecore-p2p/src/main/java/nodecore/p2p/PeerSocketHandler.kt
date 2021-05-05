// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.p2p

import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.isClosed
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nodecore.api.grpc.RpcEvent
import nodecore.p2p.event.PeerMisbehaviorEvent
import org.veriblock.core.utilities.createLogger
import java.io.IOException
import java.net.SocketException
import java.util.HashMap
import java.util.concurrent.atomic.AtomicBoolean

private val logger = createLogger {}

class PeerSocketHandler(
    private val peer: Peer,
    private val socket: Socket,
    private var ownsConnection: Boolean
) {
    private val running = AtomicBoolean(true)

    private val coroutineDispatcher = Threading.PEER_IO_POOL.asCoroutineDispatcher()
    private val coroutineScope = CoroutineScope(coroutineDispatcher)

    private val readChannel = socket.openReadChannel()
    private val writeChannel = socket.openWriteChannel()

    private val writeEventChannel: Channel<RpcEvent> = Channel(1100)

    private val inputJob: Job = coroutineScope.launch {
        runInput()
    }.also {
        it.invokeOnCompletion { stop() }
    }

    private val outputJob: Job = coroutineScope.launch {
        runOutput()
    }.also {
        it.invokeOnCompletion { stop() }
    }

    @Synchronized
    fun stop() {
        if (isRunning()) {
            running.set(false)
            inputJob.cancel()
            outputJob.cancel()
            coroutineScope.cancel()
            writeEventChannel.close()
            if (!socket.isClosed) {
                try {
                    socket.close()
                } catch (e: IOException) {
                    logger.warn("Exception closing socket", e)
                }
            }

            P2pEventBus.peerDisconnected.trigger(peer)
        }
    }

    fun write(message: RpcEvent) {
        logger.debug { "Sending ${message.resultsCase.name} message to ${peer.address}" }
        try {
            val wasAdded = writeEventChannel.offer(message)
            if (!wasAdded) {
                logger.warn { "Not writing event ${message.resultsCase.name} to peer ${socket.remoteAddress} because write queue is full." }
            }
        } catch (e: InterruptedException) {
            logger.warn { "Output stream thread shutting down for peer ${socket.remoteAddress}: $e" }
        } catch (e: ClosedSendChannelException) {
            logger.debug { "Trying to send message to peer ${socket.remoteAddress} when the socket was already closed" }
            stop()
        }
    }

    suspend fun runOutput() {
        while (isRunning()) {
            try {
                val event = writeEventChannel.receive()
                logBlocks(event)
                val message = event.toByteArray()
                writeChannel.writeInt(message.size)
                writeChannel.writeFully(message, 0, message.size)
                writeChannel.flush()

                peer.lastSuccessfulSend = System.currentTimeMillis()
                peer.state.recordBytesSent(message.size + 4L)
            } catch (e: InterruptedException) {
                logger.debug("Output stream thread shutting down")
                break
            } catch (e: CancellationException) {
                logger.debug("Output stream thread shutting down")
                break
            } catch (e: IOException) {
                logger.info("Socket closed")
                break
            } catch (e: SocketException) {
                logger.info("Socket closed")
                break
            } catch (e: Exception) {
                logger.error(e) { "Error in output stream thread!" }
                handleSocketError(e)
                break
            }
        }
    }

    private suspend fun runInput() {
        while (isRunning()) {
            try {
                val nextMessageSize = readChannel.readInt()
                peer.state.recordBytesReceived(4L)

                if (nextMessageSize > Constants.PEER_MESSAGE_SIZE_LIMIT) {
                    logger.info {
                        "Received a message size greater than the limit of ${Constants.PEER_MESSAGE_SIZE_LIMIT} bytes, message will be ignored"
                    }
                    P2pEventBus.peerMisbehavior.trigger(PeerMisbehaviorEvent(peer, PeerMisbehaviorEvent.Reason.MESSAGE_SIZE))
                    return
                } else {
                    val raw = ByteArray(nextMessageSize)
                    readChannel.readFully(raw, 0, nextMessageSize)
                    peer.state.recordBytesReceived(nextMessageSize.toLong())

                    // Handle message in a different thread
                    Threading.PEER_READ_THREAD.execute { peer.handleMessage(raw) }
                }
            } catch (e: SocketException) {
                logger.info("Attempted to read from a socket that has been closed.")
                break
            } catch (e: IOException) {
                logger.info("Disconnected from peer ${peer.address}.")
                break
            } catch (e: ClosedReceiveChannelException) {
                logger.info("Disconnected from peer ${peer.address}.")
                break
            } catch (e: CancellationException) {
                logger.debug("Input stream thread shutting down")
                break
            } catch (e: Exception) {
                logger.error(e) { "Socket error" }
                handleSocketError(e)
                break
            }
        }
    }

    private fun isRunning(): Boolean {
        return running.get()
    }

    private fun handleSocketError(t: Throwable) {
        socket.close()

        coroutineScope.launch {
            if (ownsConnection) {
                delay(5000L)
            }

            var recoverSuccess = false

            if (ownsConnection) {
                try {
                    logger.info { "Attempting to self-heal peer connection to ${socket.remoteAddress.addressKey}" }
                    recoverSuccess = peer.reconnect()
                    if (recoverSuccess) {
                        logger.info { "Connection successfully recovered to ${socket.remoteAddress.addressKey}!" }
                    }
                } catch (e: Exception) {
                    logger.warn(e) { "Error while attempting to self-heal P2P connection!" }
                }
            }

            if (!recoverSuccess) {
                peer.status = Peer.Status.Errored
                P2pEventBus.peerDisconnected.trigger(peer)
            }
        }
    }

    private val sentBlocks = HashMap<Int, Int>()

    private fun logBlocks(event: RpcEvent) {
        if (event.resultsCase == RpcEvent.ResultsCase.BLOCK) {
            logger.info { "Writing block num ${event.block.number}" }
            val sentTimes = sentBlocks[event.block.number]
            if (sentTimes != null) {
                logger.info { "Note: already sent this block $sentTimes times." }
                sentBlocks[event.block.number] = sentTimes + 1
            } else {
                sentBlocks[event.block.number] = 1
            }
        }
    }
}
