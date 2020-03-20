// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.p2p;

import com.google.common.eventbus.Subscribe;
import com.google.common.net.InetAddresses;
import nodecore.p2p.events.ExternalPeerAdded;
import nodecore.p2p.events.ExternalPeerRemoved;
import nodecore.p2p.events.PeerBannedEvent;
import nodecore.p2p.events.PeerDisconnectedEvent;
import nodecore.p2p.tasks.AnnounceTask;
import nodecore.p2p.tasks.RequestPeerTableTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.veriblock.core.params.NetworkParameters;
import org.veriblock.core.utilities.Utility;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class PeerTable {

    private static final Logger logger = LoggerFactory.getLogger(PeerTable.class);

    private final PeerWarden warden;

    private final NetworkParameters networkParameters;
    private final int bootstrapPeerLimit;
    private final int minimumPeerCount;
    private final int maximumPeerCount;
    private final boolean bootstrapEnabled;
    private final PeerTableBootstrapper bootstrapper;

    private final NodeMetadata self;
    private final List<PeerEndpoint> externalPeers;

    private final ConcurrentHashMap<String, Peer> peers;
    private final ConcurrentHashMap<String, NodeMetadata> peerCandidates;
    private final ConcurrentHashMap<String, Ban> blacklist;
    private final ConcurrentHashMap<String, Integer> doNotConnect;

    private Selector selector = null;

    public NodeMetadata getSelf() { return self; }

    private Runnable onConnected = null;
    private Runnable onDisconnected = null;

    public PeerTable(P2PConfiguration configuration, PeerWarden warden, PeerTableBootstrapper bootstrapper) {
        this.networkParameters = configuration.getNetworkParameters();
        this.bootstrapPeerLimit = configuration.getBootstrapLimit();
        this.minimumPeerCount = configuration.getPeerMinCount();
        this.maximumPeerCount = configuration.getPeerMaxCount();
        this.bootstrapEnabled = configuration.getPeerBootstrapEnabled();
        this.warden = warden;
        this.bootstrapper = bootstrapper;

        if (configuration.getExternalPeerEndpoints().size() == 0 && bootstrapEnabled) {
            logger.debug("Discovered 0 external peers configured, searching for bootstrap nodes");
            externalPeers = bootstrapper.getNext(this.bootstrapPeerLimit);
        } else {
            externalPeers = configuration.getExternalPeerEndpoints();
        }

        self = NodeMetadata.newBuilder()
                .setAddress(configuration.getPeerPublishAddress())
                .setPort(configuration.getPeerBindPort())
                .setApplication(Constants.FULL_PROGRAM_NAME_VERSION)
                .setProtocolVersion(this.networkParameters.getProtocolVersion())
                .setPlatform(configuration.getPeerSharePlatform() ? Constants.PLATFORM : "")
                .setStartTimestamp(Utility.getCurrentTimeSeconds())
                .setShareAddress(configuration.getPeerShareMyAddress())
                .setCapabilities(PeerCapabilities.allCapabilities().toBitVector())
                .setId(UUID.randomUUID().toString())
                .build();

        // Reserve space for incoming connections
        if (maximumPeerCount <= minimumPeerCount) {
            logger.warn("The configured peer counts make it impossible to accept incoming connections");
        }

        peers = new ConcurrentHashMap<>();
        peerCandidates = new ConcurrentHashMap<>();
        blacklist = new ConcurrentHashMap<>();
        this.doNotConnect = new ConcurrentHashMap<>();

        Threading.PEER_TABLE_POOL.scheduleWithFixedDelay(this::upkeep,
                1, 1, TimeUnit.MINUTES);

        InternalEventBus.getInstance().register(this);
    }

    public void initialize(Runnable onConnected, Runnable onDisconnected) {
        logger.info("Initializing peer table");
        this.onConnected = onConnected;
        this.onDisconnected = onDisconnected;
        // Introduce a bit of an initial delay when starting so that any nodes with a connect attempt to this node queued
        // have an opportunity to succeed first
        Threading.PEER_TABLE_POOL.schedule(() -> establishConnectionWithConfiguredPeers(externalPeers), 20, TimeUnit.SECONDS);
        Threading.PEER_TABLE_POOL.schedule(this::requestPeerTables, 35, TimeUnit.SECONDS);

    }

    public void shutdown() {
        Threading.shutdown(Threading.PEER_TABLE_POOL);
        warden.shutdown();

        InternalEventBus.getInstance().unregister(this);
        for (Peer peer : peers.values()) {
            peer.disconnect();
        }
    }

    private void establishConnectionWithConfiguredPeers(List<PeerEndpoint> peers) {
        // Filter out ones that have already been added
        List<PeerEndpoint> filtered = peers.stream()
                .filter(peerEndpoint -> !this.peers.containsKey(peerEndpoint.addressKey()))
                .collect(Collectors.toList());

        logger.info("Establishing connection with " + filtered.size() + " / " + peers.size() + " configured peers.");

        // Attempt to connect with the peer and add if successful
        for (PeerEndpoint endpoint : filtered) {
            attemptOutboundPeerConnect(endpoint.address(), endpoint.port());
        }
    }

    private Optional<Peer> attemptOutboundPeerConnect(String address, int port) {
        if (selector == null) {
            // This should never happen! Log it and shut down.
            logger.error("Attempting to connect to a peer while the Peer Server is down!");
            System.exit(0);
            return Optional.empty();
        }
        Peer peer = Peer.createOutboundPeer(address, port, selector);
        if (peer == null) {
            // TODO: Retry connections?
            return Optional.empty();
        }

        addPeer(peer);

        return Optional.of(peer);
    }

    private void requestPeerTables() {
        InternalEventBus.getInstance().postAsync(new RequestPeerTableTask(getAvailablePeers()));
    }

    public void registerIncomingConnection(Selector selector, SocketChannel socketChannel) throws IOException {
        Socket socket = socketChannel.socket();
        socketChannel.configureBlocking(false);
        String hostAddress = socket.getInetAddress().getHostAddress();
        if (peers.size() >= maximumPeerCount) {
            logger.info("Maximum amount of peers reached, rejecting connection");
            try {
                socketChannel.close();
            } catch (IOException ignored) {}

            return;
        }
        if (blacklist.containsKey(hostAddress)) {
            logger.info("Incoming connection is from a blacklisted peer, closing connection");
            try {
                socketChannel.close();
            } catch (IOException ignored) {}

            return;
        }

        // FIXME this key may not be enough (multiple peers may connect from the same IP and port)
        String addressKey = hostAddress + ":" + socket.getPort();
        if (peers.containsKey(addressKey)) {
            Peer original = peers.get(addressKey);
            if (original.getStatus() != Peer.Status.Closed && original.getStatus() != Peer.Status.Errored) {
                logger.info("Incoming connection is from an already connected peer, closing connection");
                try {
                    socketChannel.close();
                } catch (IOException ignored) {}

                return;
            }
        }

        Peer peer = null;
        try {
            peer = Peer.createInboundPeer(socketChannel, selector);
            addPeer(peer);

            InternalEventBus.getInstance().post(
                    new AnnounceTask(Collections.singletonList(peer), getSelf(), true));
            logger.info("New peer {} connected", peer.getAddress());
        }
        catch (Exception e) {
            logger.error("Error handling incoming peer", e);
            if (peer != null) {
                peer.disconnect();
            }
        }
    }

    public void processRemotePeerTable(List<NodeMetadata> peers) {
        for (NodeMetadata node : peers) {
            addPeerCandidate(node);
        }
    }

    public List<Peer> getAvailablePeers() {
        return peers.values().stream()
                .filter(peer -> peer.getStatus() == Peer.Status.Connected)
                .collect(Collectors.toList());
    }

    public List<Peer> getUnavailablePeers() {
        return peers.values().stream()
                .filter(peer -> peer.getStatus() != Peer.Status.Connected)
                .collect(Collectors.toList());
    }

    private void addPeerCandidate(NodeMetadata node) {
        if (node.getPort() == 0) {
            node.setPort(networkParameters.getP2PPort());
        }

        if (isNodeSelf(node.getId(), node.getAddress()) || isAddressPrivate(node.getAddress())) {
            return;
        }

        // TODO: Remove once this is no longer needed
        if (node.getPort() > 50000) {
            node.setPort(networkParameters.getP2PPort());
        }

        if (peers.containsKey(node.getAddressKey())) {
            return;
        }

        peerCandidates.put(node.getAddressKey(), node);
        logger.debug("Added {} as a peer candidate", node.getAddressKey());
    }

    public List<NodeMetadata> getPeerCandidates() {
        return new ArrayList<>(peerCandidates.values());
    }

    public void clearBans() {
        blacklist.clear();
    }

    public void updatePeer(Peer peer) {
        peers.replace(peer.getAddressKey(), peer);
    }

    private void banAddress(String address) {
        // Remove all peers connected from that address
        peers.entrySet().stream()
                .filter(entry -> entry.getValue().getAddress().equals(address))
                .forEach(entry -> removePeer(entry.getKey()));
        // Add that address to the blacklist
        blacklist.put(address, Ban.newTemporary(address));
    }

    private void addPeer(Peer peer) {
        if (isNodeSelf(peer.getId(), peer.getAddress())) {
            logger.debug("{} cannot be added as a peer because it is this node", peer.getAddress());
            return;
        }
        if (blacklist.containsKey(peer.getAddress())) {
            logger.info("{} is currently blacklisted and cannot be added", peer.getAddress());
        }

        peers.put(peer.getAddressKey(), peer);

        logger.debug("Added peer {}", peer.getAddressKey());

        if (onConnected != null) {
            onConnected.run();
        }
    }

    private void removePeer(String addressKey) {
        Peer peer = peers.remove(addressKey);
        if (peer != null)
            peer.disconnect();

        peerCandidates.remove(addressKey);
        logger.info("Removed {} from peer list", addressKey);

        if (getAvailablePeers().size() == 0 && onDisconnected != null) {
            onDisconnected.run();
        }
    }

    private void upkeep() {
        try {
            groomPeers();
            releaseDoNotConnect();
            logger.debug("Ensure min peers...");
            ensureMinimumConnectedPeers();
            if (getAvailablePeers().size() == 0) {
                logger.info("After attempting to ensure minimum peers we still have 0, removing all do-not-connect constraints...");
                // TODO: revisit clearing DNC
                doNotConnect.clear();
                ensureMinimumConnectedPeers();
                logger.info("After removing do-not-connect constraints, we have " + getAvailablePeers().size() + " available peers.");
            }
            logger.debug("Releasing bans...");
            releaseExpiredBans();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void groomPeers() {
        ArrayList<Peer> peerList = new ArrayList<>(this.peers.values());
        for (Peer p : peerList) {
            if (p.getState().getLastMessageReceivedAt() < Utility.getCurrentTimeSeconds() - Constants.PEER_TIMEOUT) {
                logger.info("Removing peer {} because it has sent no messages in the last {} seconds", p.getAddress(), Constants.PEER_TIMEOUT);
                removePeer(p.getAddressKey());
            }
        }
    }

    private void ensureMinimumConnectedPeers() {
        List<Peer> available = getAvailablePeers();
        int peerCount = available.size();
        if (peerCount >= minimumPeerCount) {
            logger.info("Minimum peer threshold met!");
            return;
        }

        if (peerCandidates.size() == 0) {
            logger.info("No peer candidates...");
            if (peerCount > 0) {
                logger.info("Requesting peer tables!");
                requestPeerTables();
                return;
            } else {
                if (bootstrapEnabled) {
                    List<PeerEndpoint> morePeers = bootstrapper.getNext(this.bootstrapPeerLimit);
                    establishConnectionWithConfiguredPeers(morePeers);
                    return;
                } else {
                    logger.info("bootstrap not enabled and no peers!");
                }
            }
        }

        logger.debug("Found {} peer candidates", peerCandidates.size());

        List<NodeMetadata> candidates = new ArrayList<>(peerCandidates.values());
        Collections.shuffle(candidates);

        int peerAttempts = 6;
        for (NodeMetadata candidate : candidates) {
            if (getAvailablePeers().size() < minimumPeerCount && peerAttempts > 0) {
                peerCandidates.remove(candidate.getAddressKey());

                // Skip if it's already an established peer, waiting to connect or in the do not connect list
                if (peers.containsKey(candidate.getAddressKey()) || doNotConnect.containsKey(candidate.getAddress()))
                    continue;

                logger.debug("Attempting to connect with {}:{}", candidate.getAddress(), candidate.getPort());
                Optional<Peer> peer = attemptOutboundPeerConnect(candidate.getAddress(), candidate.getPort());

                if (peer.isPresent()) {
                    peer.get().setId(candidate.getId());
                    peer.get().setApplication(candidate.getApplication());
                    peer.get().setPlatform(candidate.getPlatform());
                    peer.get().setProtocolVersion(candidate.getProtocolVersion());
                    peer.get().setStartTimestamp(candidate.getStartTimestamp());
                    peer.get().setShareAddress(candidate.shareAddress());
                    peer.get().setCapabilities(PeerCapabilities.parse(candidate.getCapabilities()));
                }
                peerAttempts--;
            }
        }
    }

    private void releaseDoNotConnect() {
        try {
            for (String key : doNotConnect.keySet()) {
                if (Utility.getCurrentTimeSeconds() >= doNotConnect.getOrDefault(key, 0)) {
                    doNotConnect.remove(key);
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void releaseExpiredBans() {
        try {
            List<Ban> expired = blacklist.values().stream()
                    .filter(Ban::isExpired)
                    .collect(Collectors.toList());

            for (Ban ban : expired) {
                blacklist.remove(ban.getAddress());
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private boolean isNodeSelf(String id, String address) {
        return self.getId().equals(id) || self.getAddress().equals(address);
    }

    public void setSelector(Selector selector) {
        this.selector = selector;
    }

    private boolean isAddressPrivate(String address) {
        if (InetAddresses.isInetAddress(address)) {
            InetAddress ipAddress = InetAddresses.forString(address);
            return ipAddress.isSiteLocalAddress() || ipAddress.isLoopbackAddress();
        }

        return false;
    }

    @Subscribe public void onExternalPeerAdded(ExternalPeerAdded event) {
        try {
            externalPeers.add(event.getPeerEndpoint());
            establishConnectionWithConfiguredPeers(Collections.singletonList(event.getPeerEndpoint()));
        } catch (Exception e) {
            logger.error("Exception occurred adding external peer", e);
        }
    }

    @Subscribe public void onExternalPeerRemoved(ExternalPeerRemoved event) {
        try {
            externalPeers.removeIf(peerEndpoint -> peerEndpoint.address().equals(event.getPeerEndpoint().address()));
            removePeer(event.getPeerEndpoint().addressKey());
        } catch (Exception e) {
            logger.error("Exception occurred removing external peer", e);
        }
    }

    @Subscribe public void onPeerBanned(PeerBannedEvent event) {
        banAddress(event.getPeer().getAddress());
    }

    @Subscribe public void onPeerDisconnected(PeerDisconnectedEvent event) {
        removePeer(event.getPeer().getAddressKey());
    }
}
