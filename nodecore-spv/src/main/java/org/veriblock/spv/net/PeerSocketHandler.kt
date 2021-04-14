// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.spv.net

import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.isClosed
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.util.network.hostname
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.launch
import nodecore.api.grpc.RpcEvent
import org.veriblock.core.utilities.createLogger
import org.veriblock.core.utilities.debugInfo
import org.veriblock.spv.serialization.MessageSerializer.deserialize
import org.veriblock.spv.util.Threading.PEER_INPUT_POOL
import java.io.IOException
import java.net.SocketException
import java.util.concurrent.atomic.AtomicBoolean

private val logger = createLogger {}

class PeerSocketHandler(
    private val peer: SpvPeer,
    private val socket: Socket
) {
    private val coroutineDispatcher = PEER_INPUT_POOL.asCoroutineDispatcher()
    private val coroutineScope = CoroutineScope(coroutineDispatcher)

    private val readChannel = socket.openReadChannel()
    private val writeChannel = socket.openWriteChannel()

    private lateinit var inputJob: Job
    private lateinit var outputJob: Job

    private val writeQueue: Channel<RpcEvent> = Channel(1100)
    private val running = AtomicBoolean(false)

    fun isRunning(): Boolean {
        return running.get()
    }

    fun start() {
        running.set(true)
        inputJob = coroutineScope.launch { runInput() }
        outputJob = coroutineScope.launch { runOutput() }
        inputJob.invokeOnCompletion { stop() }
        outputJob.invokeOnCompletion { stop() }
    }

    @Synchronized
    fun stop() {
        if (isRunning()) {
            running.set(false)
            coroutineScope.cancel()
            writeQueue.close()
            if (!socket.isClosed) {
                try {
                    socket.close()
                } catch (e: IOException) {
                    logger.warn("Exception closing socket", e)
                }
            }
            peer.onPeerSocketClosed()
        }
    }

    fun write(message: RpcEvent) {
        logger.debug { "Sending ${message.resultsCase.name} message to ${peer.address}" }
        try {
            val wasAdded = writeQueue.offer(message)
            if (!wasAdded) {
                logger.warn(
                    "Not writing event {} to peer {} because write queue is full.", message.resultsCase.name,
                    socket.remoteAddress
                )
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
                val event = writeQueue.receive()
                val message = event.toByteArray()
                writeChannel.writeInt(message.size)
                writeChannel.writeFully(message, 0, message.size)
                writeChannel.flush()
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
                logger.error("Error in output stream thread!", e)
                break
            }
            //delay(10L)
        }
    }

    private suspend fun runInput() {
        while (isRunning()) {
            try {
                val nextMessageSize = readChannel.readInt()
                val raw = ByteArray(nextMessageSize)
                readChannel.readFully(raw, 0, nextMessageSize)
                val message = deserialize(raw)
                if (message == null) {
                    // Handle bad messages
                } else {
                    logger.debug { "Received ${message.resultsCase.name} from ${peer.address}" }
                    peer.processMessage(message)
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
                break
            }
            //delay(10L)
        }
    }
}
