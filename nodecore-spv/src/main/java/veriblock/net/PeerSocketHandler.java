// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package veriblock.net;

import nodecore.api.grpc.VeriBlockMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.veriblock.core.utilities.Utility;
import veriblock.serialization.MessageSerializer;
import veriblock.util.Threading;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class PeerSocketHandler {
    private static final Logger logger = LoggerFactory.getLogger(PeerSocketHandler.class);

    private final BlockingQueue<VeriBlockMessages.Event> writeQueue = new LinkedTransferQueue<>();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean errored = new AtomicBoolean(false);

    private final Socket socket;

    private DataInputStream inputStream;
    private CompletableFuture<Void> inputThread;
    private DataOutputStream outputStream;
    private CompletableFuture<Void> outputThread;

    private Peer peer;
    public void setPeer(Peer peer) {
        this.peer = peer;
    }

    public boolean isRunning() {
        return running.get();
    }

    public PeerSocketHandler(Socket socket) {
        this.socket = socket;

        try {
            this.inputStream = new DataInputStream(socket.getInputStream());
            this.outputStream = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    public void start() {
        running.set(true);

        this.inputThread = CompletableFuture.runAsync(this::runInput, Threading.PEER_INPUT_POOL);
        this.outputThread = CompletableFuture.runAsync(this::runOutput, Threading.PEER_OUTPUT_POOL);

        this.inputThread.thenRun(this::stop);
        this.outputThread.thenRun(this::stop);
    }

    public synchronized void stop() {
        if (isRunning()) {
            running.set(false);

            if (!inputThread.isDone()) {
                inputThread.cancel(true);
            }
            if (!outputThread.isDone()) {
                outputThread.cancel(true);
            }

            writeQueue.clear();
            if (!socket.isClosed()) {
                try {
                    socket.close();
                } catch (IOException e) {
                    logger.warn("Exception closing socket", e);
                }
            }

            if (this.peer != null) {
                this.peer.onPeerSocketClosed();
            }
        }
    }

    public void write(VeriBlockMessages.Event message) {
        try {
            if (writeQueue.size() < 1100) {
                writeQueue.put(message);
            } else {
                logger.warn("Not writing event {} to peer {} because write queue is full.", message.getResultsCase().name(), socket.getInetAddress().getHostAddress());
            }
        } catch (InterruptedException e) {
            logger.warn("Output stream thread shutting down for peer {}", socket.getInetAddress().getHostAddress());
        }
    }

    public void runOutput() {
        while (isRunning()) {
            try {
                if (Thread.interrupted()) {
                    logger.info("Output thread for peer {} interrupted.", socket.getInetAddress().getHostAddress());
                    break;
                }
                VeriBlockMessages.Event event = writeQueue.take();
                byte[] message = event.toByteArray();
                byte[] messageSize = Utility.intToByteArray(message.length);

                outputStream.write(messageSize);
                outputStream.write(message);

                outputStream.flush();
            } catch (InterruptedException e) {
                logger.info("Output stream thread shutting down");
                break;
            } catch (Exception e) {
                logger.error("Error in output stream thread!", e);
                break;
            }
        }
    }

    public void runInput() {
        while (isRunning()) {
            try {
                byte[] sizeBuffer = new byte[4];
                inputStream.readFully(sizeBuffer);
                int nextMessageSize = Utility.byteArrayToInt(sizeBuffer);

                byte[] raw = new byte[nextMessageSize];
                inputStream.readFully(raw);

                VeriBlockMessages.Event message = MessageSerializer.deserialize(raw);
                if (message == null) {
                    // Handle bad messages
                } else {
                    if (this.peer != null) {
                        this.peer.processMessage(message);
                    }
                }
            } catch (SocketException e) {
                logger.info("Attempted to read from a socket that has been closed.");
                // Disconnect?
                break;
            } catch (Exception e) {
                logger.error("Socket error: ", e);
                break;
            }
        }
    }
}
