// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package veriblock.net

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nodecore.api.grpc.VeriBlockMessages
import org.veriblock.core.utilities.Utility
import org.veriblock.core.utilities.createLogger
import veriblock.serialization.MessageSerializer.deserialize
import veriblock.util.Threading.PEER_INPUT_POOL
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.IOException
import java.lang.Thread.sleep
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedTransferQueue
import java.util.concurrent.atomic.AtomicBoolean

private val logger = createLogger {}

class PeerSocketHandler(
    private val peer: Peer
) {
    private val socket = Socket(peer.address, peer.port)
    private val inputStream = DataInputStream(socket.getInputStream())
    private val outputStream = DataOutputStream(socket.getOutputStream())

    private val coroutineScope = CoroutineScope(PEER_INPUT_POOL.asCoroutineDispatcher())
    private lateinit var inputJob: Job
    private lateinit var outputJob: Job

    private val writeQueue: BlockingQueue<VeriBlockMessages.Event> = LinkedTransferQueue()
    private val running = AtomicBoolean(false)
    private val errored = AtomicBoolean(false)

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
            writeQueue.clear()
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

    fun write(message: VeriBlockMessages.Event) {
        logger.debug { "Sending ${message.resultsCase.name} message to ${peer.address}" }
        try {
            if (writeQueue.size < 1100) {
                writeQueue.put(message)
            } else {
                logger.warn(
                    "Not writing event {} to peer {} because write queue is full.", message.resultsCase.name,
                    socket.inetAddress.hostAddress
                )
            }
        } catch (e: InterruptedException) {
            logger.warn { "Output stream thread shutting down for peer ${socket.inetAddress.hostAddress}" }
        }
    }

    suspend fun runOutput() {
        while (isRunning()) {
            try {
                val event = writeQueue.take()
                val message = event.toByteArray()
                val messageSize = Utility.intToByteArray(message.size)
                outputStream.write(messageSize)
                outputStream.write(message)
                outputStream.flush()
            } catch (e: InterruptedException) {
                logger.info("Output stream thread shutting down")
                break
            } catch (e: CancellationException) {
                logger.info("Output stream thread shutting down")
                break
            } catch (e: Exception) {
                logger.error("Error in output stream thread!", e)
                break
            }
            delay(10L)
        }
    }

    private suspend fun runInput() {
        while (isRunning()) {
            try {
                val nextMessageSize = inputStream.readInt()
                val raw = ByteArray(nextMessageSize)
                inputStream.readFully(raw)
                val message = deserialize(raw)
                if (message == null) {
                    // Handle bad messages
                } else {
                    logger.debug { "Received ${message.resultsCase.name} from ${peer.address}" }
                    peer.processMessage(message)
                }
            } catch (e: SocketException) {
                logger.info("Attempted to read from a socket that has been closed.")
                // Disconnect?
                break
            } catch (e: EOFException) {
                logger.info("Disconnected from peer ${peer.address}.")
                break
            } catch (e: Exception) {
                logger.error("Socket error: ", e)
                break
            }
            delay(10)
        }
    }
}
