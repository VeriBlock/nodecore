// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.p2p;

import com.google.protobuf.InvalidProtocolBufferException;
import nodecore.api.grpc.VeriBlockMessages;
import nodecore.p2p.events.PeerDisconnectedEvent;
import nodecore.p2p.events.PeerMisbehaviorEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.veriblock.core.crypto.BloomFilter;
import org.veriblock.core.utilities.Utility;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

public class Peer {
    private static final Logger logger = LoggerFactory.getLogger(Peer.class);

    public enum Status {
        Initialized,
        Connecting,
        Connected,
        Errored,
        Closed
    }

    private SelectionKey selectionKey;
    private SocketChannel socketChannel;

    private Integer port;
    public Integer getPort() {
        return port;
    }

    private Integer reconnectPort;
    public Integer getReconnectPort() {
        return reconnectPort;
    }

    public void setReconnectPort(Integer reconnectPort) {
        this.reconnectPort = reconnectPort;
    }

    private String application;
    public String getApplication() { return application != null ? application : "Unknown"; }
    public void setApplication(String application) {
        this.application = application;
    }

    private int protocolVersion;
    public int getProtocolVersion() { return protocolVersion; }
    public void setProtocolVersion(int protocolVersion) {
        this.protocolVersion = protocolVersion;
    }

    private String platform;
    public String getPlatform() { return platform != null ? platform : "Unknown"; }
    public void setPlatform(String platform) {
        this.platform = platform;
    }

    private int startTimestamp;
    public int getStartTimestamp() { return startTimestamp; }
    public void setStartTimestamp(int startTimestamp) {
        this.startTimestamp = startTimestamp;
    }

    private boolean canShareAddress;
    public boolean shareAddress() { return canShareAddress; }
    public void setShareAddress(boolean value) { this.canShareAddress = value; }

    private PeerCapabilities capabilities;
    public PeerCapabilities getCapabilities() {
        return capabilities;
    }
    public void setCapabilities(PeerCapabilities value) {
        this.capabilities = value;
    }

    private String id;
    public String getId() { return id != null ? id : ""; }
    public void setId(String id) {
        this.id = id;
    }

    private Status status;

    public Status getStatus() {
        return this.status;
    }

    private String address;
    public String getAddress() {
        return address;
    }

    public String getAddressKey() {
        return address + ":" + port;
    }

    private PeerState state;
    public PeerState getState() {
        return state;
    }

    private BloomFilter filter;
    public BloomFilter getFilter() {
        return filter;
    }
    public void setFilter(BloomFilter filter) {
        this.filter = filter;
    }

    private boolean ownsConnection;

    protected Peer() {}

    private Peer(SelectionKey selectionKey, boolean ownsConnection) {
        this.selectionKey = selectionKey;
        this.socketChannel = (SocketChannel)selectionKey.channel();
        initialize(ownsConnection);
    }

    private void initialize(boolean ownsConnection) {
        this.address = socketChannel.socket().getInetAddress().getHostAddress();
        this.port = socketChannel.socket().getPort();

        this.ownsConnection = ownsConnection;

        this.state = new PeerState();
        status = Status.Connected;
    }

    private final ByteBuffer sizeBuffer = ByteBuffer.allocate(4);

    private int nextMessageSize;
    private ByteBuffer readBuffer = null;

    public void read() {
        try {
            if (readBuffer == null) {
                // Read size to nextMessageSize
                int bytesRead = socketChannel.read(sizeBuffer);
                if (bytesRead == 4) {
                    sizeBuffer.flip();
                    nextMessageSize = sizeBuffer.getInt();
                    sizeBuffer.clear();
                } else {
                    disconnect();
                    return;
                }

                logger.debug("Next message size: {}", nextMessageSize);
                // Create the read buffer and reset the read size
                readBuffer = ByteBuffer.allocate(nextMessageSize);

                if (nextMessageSize > Constants.PEER_BAN_MESSAGE_SIZE_LIMIT) {
                    InternalEventBus.getInstance().post(new PeerMisbehaviorEvent(this, PeerMisbehaviorEvent.Reason.MESSAGE_SIZE_EXCESSIVE));

                    disconnect();

                    return;
                }
            }

            // Read the packet content to the read buffer
            logger.trace("Buffer's remaining bytes: {}", readBuffer.remaining());
            int readSize = socketChannel.read(readBuffer);
            logger.trace("Read {} bytes! Remaining: {}", readSize, readBuffer.remaining());
            // If we didn't get the entire packet yet, wait for the next call to read()
            if (readBuffer.hasRemaining()) {
                return;
            }

            getState().recordBytesReceived(nextMessageSize + 4L);

            if (nextMessageSize > Constants.PEER_MESSAGE_SIZE_LIMIT) {
                logger.info("Received a message size greater than the limit of {} bytes, message will be ignored", Constants.PEER_MESSAGE_SIZE_LIMIT);
                InternalEventBus.getInstance().post(new PeerMisbehaviorEvent(this, PeerMisbehaviorEvent.Reason.MESSAGE_SIZE));
                return;
            } else {
                byte[] data = readBuffer.array();

                // Handle message in a different thread
                Threading.PEER_READ_THREAD.execute(() -> handleMessage(data));
            }
            // Ready to receive another packet
            readBuffer = null;
        } catch (IOException e) {
            logger.warn("Attempted to read from a socket that has been closed.");
            disconnect();
        } catch (Exception e) {
            logger.error("Socket error: ", e);

            handleSocketError(e);
        }
    }

    private final List<VeriBlockMessages.Event> writeQueue = new CopyOnWriteArrayList<>();

    public void write() {
        logger.trace("Writing queued events ({})", writeQueue.size());
        synchronized (writeQueue) {
            List<VeriBlockMessages.Event> written = new ArrayList<>();
            for (VeriBlockMessages.Event eventToWrite : writeQueue) {
                try {
                    boolean success = writeToStream(eventToWrite);
                    if (!success) {
                        logger.info("Failed to write a queued event with the id: " + eventToWrite.getId());
                        break;
                    }
                    written.add(eventToWrite);
                } catch (Exception e) {
                    logger.error("Error while writing event!", e);
                    handleSocketError(e);
                }
            }
            writeQueue.removeAll(written);
            if (!writeQueue.isEmpty()) {
                selectionKey.interestOps(selectionKey.interestOps() & ~SelectionKey.OP_WRITE);
            }
        }
    }

    private synchronized boolean writeToStream(VeriBlockMessages.Event event) {
        if (sendBuffer != null) {
            return writeSendBuffer();
        }

        logger.debug("Sending event of type {} to {}", event.getResultsCase().name(), address);

        logBlocks(event);

        // Prepare the send buffer
        byte[] message = event.toByteArray();
        sendBuffer = ByteBuffer.allocate(message.length + 4);
        sendBuffer.putInt(message.length);
        sendBuffer.put(message);
        sendBuffer.flip();
        // Write to it
        boolean success = writeSendBuffer();
        // If we didn't finish writing, return and wait for next call for completion
        if (!success) {
            return false;
        }

        lastSuccessfulSend = System.currentTimeMillis();

        getState().recordBytesSent(message.length + 4L);
        return true;
    }

    private ByteBuffer sendBuffer = null;

    private synchronized boolean writeSendBuffer() {
        try {
            int written = socketChannel.write(sendBuffer);
            logger.trace("Written {} bytes. Remaining: {}", written, sendBuffer.remaining());
            if (sendBuffer.hasRemaining()) {
                // Leave the send buffer set for the next write attempt
                return false;
            } else{
                // Set the buffer to null in order to let new packets be written
                sendBuffer = null;
                return true;
            }
        } catch (Exception e) {
            logger.warn("Unable to write to peer stream for peer " + address + "!", e);

            // Reset buffer
            sendBuffer = null;
            synchronized (writeQueue) {
                writeQueue.clear();

                logger.warn("Write queue to peer " + address + " cleared, size: " + writeQueue.size());
            }

            handleSocketError(e);
            return false;
        }
    }

    private HashMap<Integer, Integer> sentBlocks = new HashMap<>();

    private void logBlocks(VeriBlockMessages.Event event) {
        if (event.getResultsCase() == VeriBlockMessages.Event.ResultsCase.BLOCK) {
            logger.info("Writing block num " + event.getBlock().getNumber());
            if (sentBlocks.containsKey(event.getBlock().getNumber())) {
                logger.info("Note: already sent this block " + sentBlocks.get(event.getBlock().getNumber()) + " times.");
                sentBlocks.put(event.getBlock().getNumber(), sentBlocks.get(event.getBlock().getNumber()) + 1);
            } else {
                sentBlocks.put(event.getBlock().getNumber(), 1);
            }
        }
    }

    public void disconnect() {
        if (status == Status.Closed || status == Status.Errored) {
            return;
        }

        status = Status.Closed;
        closeSocket();

        InternalEventBus.getInstance().post(new PeerDisconnectedEvent(this));
    }

    private boolean reconnect() {
        try {
            SocketAddress socketAddress = new InetSocketAddress(address, port);
            this.socketChannel = SocketChannel.open(socketAddress);
            this.socketChannel.configureBlocking(false);

            // Cancel the current selection key
            Selector selector = selectionKey.selector();
            selectionKey.cancel();

            // Get a new selection key
            selectionKey = socketChannel.register(selector, SelectionKey.OP_READ);
            selectionKey.attach(this);

            initialize(ownsConnection);
            return true;
        } catch (Exception e) {
            logger.debug("Unable to open connection to " + address + ":" + port + "!", e);
            return false;
        }
    }

    private void closeSocket() {
        try {
            socketChannel.close();
        } catch (Exception ignored) {
        }
    }

    private long lastSuccessfulSend = System.currentTimeMillis();

    public boolean send(VeriBlockMessages.Event message) {
        if (status == Status.Closed || status == Status.Errored) {
            logger.warn("Peer {} is not in a state for sending, message will not be sent", address);
            return false;
        }

        logger.trace("Attempting to send {} to peer {}", message.getResultsCase().name(), address);
        try {
            if (System.currentTimeMillis() - 60000 > lastSuccessfulSend) {
                logger.info("Note: peer " + address + " has not had a successful write in " + (System.currentTimeMillis() / 1000 - lastSuccessfulSend / 1000) + " seconds and it has " + writeQueue.size() + " messages pending to be sent");
            }

            if (System.currentTimeMillis() - (1000 * 5 * 60) > lastSuccessfulSend) {
                logger.warn("Peer " + address + " has not had a successful write in over 5 minutes and it has " + writeQueue.size() + " messages pending to be sent, closing connection...");
                try {
                    disconnect();
                    return false;
                } catch (Exception ignored) { }
            }
            synchronized (writeQueue) {
                if (writeQueue.size() < 1100) {
                    writeQueue.add(message);
                    logger.trace("{} event has been added to peer {}'s write queue ({})!", message.getResultsCase().name(), address, writeQueue.size());
                    selectionKey.interestOps(selectionKey.interestOps() | SelectionKey.OP_WRITE);
                    logger.trace("Peer {}'s selection key has been given interest to write.", address);
                } else {
                    logger.warn("Not writing event " + message.getResultsCase().name() + " to peer " + address + " because write queue is full.");
                }
            }

            return true;

        } catch (CancelledKeyException e) {
            logger.warn("Key has been cancelled for {}!", address);
            disconnect();
            return false;
        } catch (Exception e) {
            logger.warn("Unable to send message to {}", address, e);

            throw e;
        }
    }

    private void handleMessage(byte[] rawMessage) {
        VeriBlockMessages.Event event;
        try {
            event = VeriBlockMessages.Event.parseFrom(rawMessage);
            logger.debug("Received event of type {}", event.getResultsCase().name());
        } catch (InvalidProtocolBufferException e) {
            logger.debug("Malformed event: {}", Utility.bytesToHex(rawMessage));
            InternalEventBus.getInstance().post(new PeerMisbehaviorEvent(this, PeerMisbehaviorEvent.Reason.MALFORMED_EVENT));
            return;
        }

        this.state.setLastMessageReceivedAt(Utility.getCurrentTimeSeconds());

        EventRegistrar.newEvent(event, this);
    }

    private void handleSocketError(Throwable t) {
        closeSocket();

        Threading.PEER_TABLE_POOL.schedule(() -> {
            boolean recoverSuccess = false;

            if (ownsConnection) {
                try {
                    logger.info("Attempting to self-heal peer connection to " + address + ":" + port);
                    recoverSuccess = reconnect();
                    if (recoverSuccess) {
                        logger.info("Connection successfully recovered to " + address + ":" + port + "!");
                    }
                } catch (Exception e) {
                    logger.warn("Error while attempting to self-heal P2P connection!", e);
                }
            }

            if (!recoverSuccess) {
                status = Status.Errored;
                InternalEventBus.getInstance().post(new PeerDisconnectedEvent(this));
            }
        }, ownsConnection ? 5000L : 0L, TimeUnit.MILLISECONDS);
    }

    public static Peer createOutboundPeer(String address, int port, Selector selector) {
        try {
            logger.debug("Attempting to establish client connection to peer " + address + ":" + port);
            SocketAddress socketAddress = new InetSocketAddress(address, port);
            SocketChannel socketChannel = SocketChannel.open(socketAddress);
            socketChannel.configureBlocking(false);
            SelectionKey selectionKey = socketChannel.register(selector, SelectionKey.OP_READ);

            Peer peer = new Peer(selectionKey, true);
            selectionKey.attach(peer);
            return peer;
        } catch (IOException e) {
            logger.debug("Unable to open connection to " + address + ":" + port + "!");
        }

        return null;
    }

    public static Peer createInboundPeer(SocketChannel socketChannel, Selector selector) throws IOException {
        SelectionKey selectionKey = socketChannel.register(selector, SelectionKey.OP_READ);

        Peer peer = new Peer(selectionKey, false);
        selectionKey.attach(peer);
        return peer;
    }
}
