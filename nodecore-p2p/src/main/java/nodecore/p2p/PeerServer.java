// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.p2p;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class PeerServer {

    private static final Logger _logger = LoggerFactory.getLogger(PeerServer.class);

    private static final long SLEEP_TIME = 50; // milliseconds

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final InetSocketAddress bindAddress;
    private final PeerTable peerTable;

    public PeerServer(P2PConfiguration configuration, PeerTable peerTable) {
        this.bindAddress = new InetSocketAddress(configuration.getPeerBindAddress(), configuration.getPeerBindPort());
        this.peerTable = peerTable;
    }

    public void start() {
        running.set(true);
        Threading.PEER_SERVER_THREAD.submit(this::run);
    }

    private void run() {
        try (Selector selector = Selector.open()) {
            peerTable.setSelector(selector);
            try (ServerSocketChannel serverSocket = ServerSocketChannel.open()) {
                serverSocket.bind(this.bindAddress);
                serverSocket.configureBlocking(false);
                serverSocket.register(selector, SelectionKey.OP_ACCEPT);
                while (running.get()) {
                    try {
                        selector.selectNow();
                        Set<SelectionKey> selectedKeys = selector.selectedKeys();
                        Iterator<SelectionKey> iter = selectedKeys.iterator();
                        while (iter.hasNext()) {

                            SelectionKey key = iter.next();

                            if (key.isAcceptable()) {
                                SocketChannel clientSocket = serverSocket.accept();
                                peerTable.registerIncomingConnection(selector, clientSocket);
                            }

                            if (key.isReadable() && key.attachment() instanceof Peer) {
                                ((Peer)key.attachment()).read();
                            }

                            if (key.isWritable() && key.attachment() instanceof Peer) {
                                ((Peer) key.attachment()).write();
                            }
                            iter.remove();
                        }

                        // We will sleep a small amount of time to prevent too frequent calls to the selector
                        try {
                            Thread.sleep(SLEEP_TIME);
                        } catch (InterruptedException ignored) {
                        }
                    } catch (IOException e) {
                        _logger.warn("Exception occurred accepting new network event", e);
                    } catch (CancelledKeyException ignored) {
                    }
                }
            } catch (Exception e) {
                _logger.error("Unable to accept incoming socket connection at " + this.bindAddress + "!", e);
            }
        } catch (Exception e) {
            _logger.error("Unable to open selector!", e);
        } finally {
            peerTable.setSelector(null);
        }

        System.out.println("BYE");
    }

    public void stop() {
        running.set(false);
        Threading.shutdown(Threading.PEER_SERVER_THREAD);
    }
}
