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
import io.ktor.utils.io.cancel
import io.ktor.utils.io.close
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
    private val socket: Socket
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
            readChannel.cancel()
            writeChannel.close()
            writeEventChannel.close()
            if (!socket.isClosed) {
                try {
                    socket.close()
                } catch (e: IOException) {
                    logger.warn("Exception closing socket", e)
                }
            }
        }
    }

    fun write(message: RpcEvent) {
        logger.debug { "Sending ${message.resultsCase.name} message to $peer" }
        try {
            val result = writeEventChannel.trySend(message)
            if (!result.isSuccess) {
                logger.warn { "Not writing event ${message.resultsCase.name} to peer $peer because write queue is full." }
                logger.debug { "Not writing event ${message.resultsCase.name} to peer $peer because write queue is full. Exception: ${result.exceptionOrNull()?.message}" }
            }
        } catch (e: InterruptedException) {
            logger.warn { "Output stream thread shutting down for peer $peer: $e" }
        } catch (e: ClosedSendChannelException) {
            logger.debug { "Trying to send message to peer $peer when the socket was already closed" }
            handleSocketError(e)
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
                handleSocketError(e)
                break
            } catch (e: CancellationException) {
                logger.debug("Output stream thread shutting down")
                handleSocketError(e)
                break
            } catch (e: IOException) {
                logger.info("Socket closed")
                handleSocketError(e)
                break
            } catch (e: SocketException) {
                logger.info("Socket closed")
                handleSocketError(e)
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

                if (nextMessageSize > P2pConstants.PEER_MESSAGE_SIZE_LIMIT) {
                    logger.info {
                        "Received a message size greater than the limit of ${P2pConstants.PEER_MESSAGE_SIZE_LIMIT} bytes, message will be ignored"
                    }
                    P2pEventBus.peerMisbehavior.trigger(PeerMisbehaviorEvent(
                        peer = peer,
                        reason = PeerMisbehaviorEvent.Reason.MESSAGE_SIZE,
                        message = "The peer sent a too long message ($nextMessageSize, the maximum is ${P2pConstants.PEER_MESSAGE_SIZE_LIMIT})"
                    ))
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
                logger.info("Disconnected from peer $peer.")
                break
            } catch (e: ClosedReceiveChannelException) {
                logger.info("Disconnected from peer $peer.")
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

    private val errored = AtomicBoolean(false)

    private fun handleSocketError(t: Throwable) {
        socket.close()

        if (errored.getAndSet(true)) {
            return
        }

        coroutineScope.launch {
            if (peer.ownsConnection) {
                delay(5000L)
            }

            if (peer.status == Peer.Status.Closed || peer.status == Peer.Status.Errored) {
                return@launch
            }

            var recoverSuccess = false

            if (peer.ownsConnection && peer.reconnectionAttempts < 3) {
                try {
                    logger.info { "Attempting to self-heal peer connection to $peer" }
                    recoverSuccess = peer.reconnect()
                    if (recoverSuccess) {
                        logger.info { "Connection to $peer successfully recovered!" }
                    }
                } catch (e: Exception) {
                    logger.warn(e) { "Error while attempting to self-heal P2P connection!" }
                }
            }

            if (!recoverSuccess) {
                peer.status = Peer.Status.Errored
                P2pEventBus.peerDisconnected.trigger(peer)
            }

            coroutineScope.cancel()
        }
    }

    private val sentBlocks = HashMap<Int, Int>()

    private fun logBlocks(event: RpcEvent) {
        if (event.resultsCase == RpcEvent.ResultsCase.BLOCK) {
            logger.debug { "Writing block num ${event.block.number}" }
            val sentTimes = sentBlocks[event.block.number]
            if (sentTimes != null) {
                logger.debug { "Note: already sent this block $sentTimes times." }
                sentBlocks[event.block.number] = sentTimes + 1
            } else {
                sentBlocks[event.block.number] = 1
            }
        }
    }
}
