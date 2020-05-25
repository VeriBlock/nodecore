// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package veriblock.net

import nodecore.api.grpc.VeriBlockMessages
import org.slf4j.LoggerFactory
import org.veriblock.core.utilities.Utility
import org.veriblock.core.utilities.createLogger
import veriblock.serialization.MessageSerializer.deserialize
import veriblock.util.Threading
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.lang.Thread.sleep
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.BlockingQueue
import java.util.concurrent.CompletableFuture
import java.util.concurrent.LinkedTransferQueue
import java.util.concurrent.atomic.AtomicBoolean

private val logger = createLogger {}

class PeerSocketHandler(
    private val socket: Socket
) {
    private val writeQueue: BlockingQueue<VeriBlockMessages.Event> = LinkedTransferQueue()
    private val running = AtomicBoolean(false)
    private val errored = AtomicBoolean(false)
    private var inputStream: DataInputStream
    private lateinit var inputThread: CompletableFuture<Void>
    private var outputStream: DataOutputStream
    private lateinit var outputThread: CompletableFuture<Void>
    private var peer: Peer? = null

    init {
        inputStream = DataInputStream(socket.getInputStream())
        outputStream = DataOutputStream(socket.getOutputStream())
    }

    fun setPeer(peer: Peer?) {
        this.peer = peer
    }

    fun isRunning(): Boolean {
        return running.get()
    }

    fun start() {
        running.set(true)
        inputThread = CompletableFuture.runAsync(Runnable { runInput() }, Threading.PEER_INPUT_POOL)
        outputThread = CompletableFuture.runAsync(Runnable { runOutput() }, Threading.PEER_OUTPUT_POOL)
        inputThread.thenRun { stop() }
        outputThread.thenRun { stop() }
    }

    @Synchronized
    fun stop() {
        if (isRunning()) {
            running.set(false)
            if (!inputThread.isDone) {
                inputThread.cancel(true)
            }
            if (!outputThread.isDone) {
                outputThread.cancel(true)
            }
            writeQueue.clear()
            if (!socket.isClosed) {
                try {
                    socket.close()
                } catch (e: IOException) {
                    logger.warn("Exception closing socket", e)
                }
            }
            peer?.onPeerSocketClosed()
        }
    }

    fun write(message: VeriBlockMessages.Event) {
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
            logger.warn(
                "Output stream thread shutting down for peer {}", socket.inetAddress.hostAddress
            )
        }
    }

    fun runOutput() {
        while (isRunning()) {
            try {
                if (Thread.interrupted()) {
                    logger.info(
                        "Output thread for peer {} interrupted.", socket.inetAddress.hostAddress
                    )
                    break
                }
                val event = writeQueue.take()
                val message = event.toByteArray()
                val messageSize = Utility.intToByteArray(message.size)
                outputStream.write(messageSize)
                outputStream.write(message)
                outputStream.flush()
            } catch (e: InterruptedException) {
                logger.info("Output stream thread shutting down")
                break
            } catch (e: Exception) {
                logger.error("Error in output stream thread!", e)
                break
            }
            sleep(10)
        }
    }

    private fun runInput() {
        while (isRunning()) {
            try {
                val sizeBuffer = ByteArray(4)
                inputStream.readFully(sizeBuffer)
                val nextMessageSize = Utility.byteArrayToInt(sizeBuffer)
                val raw = ByteArray(nextMessageSize)
                inputStream.readFully(raw)
                val message = deserialize(raw)
                if (message == null) {
                    // Handle bad messages
                } else {
                    peer?.processMessage(message)
                }
            } catch (e: SocketException) {
                logger.info("Attempted to read from a socket that has been closed.")
                // Disconnect?
                break
            } catch (e: Exception) {
                logger.error("Socket error: ", e)
                break
            }
            sleep(10)
        }
    }
}
