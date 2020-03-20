// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.p2p;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.veriblock.core.params.NetworkParameters;
import org.veriblock.core.utilities.Utility;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PeerTableBootstrapper {
    private static final Logger logger = LoggerFactory.getLogger(PeerTableBootstrapper.class);

    private final ArrayDeque<PeerEndpoint> bootstrapPeerQueue = new ArrayDeque<>();

    private final NetworkParameters networkParameters;
    private final DnsResolver dnsResolver;
    private final List<String> bootstrapPeers;
    private final List<String> dnsSeeds;

    private int lastInitTimestamp;

    public PeerTableBootstrapper(P2PConfiguration configuration, DnsResolver dnsResolver) {
        this.networkParameters = configuration.getNetworkParameters();
        this.dnsResolver = dnsResolver;
        this.bootstrapPeers = configuration.getBootstrapPeers();
        this.dnsSeeds = configuration.getBootstrappingDnsSeeds();
        this.lastInitTimestamp = 0;
    }

    public PeerEndpoint getNext() {
        // If there's nothing left in the queue and it's been five minutes since exhausting the bootstrap peers,
        // run the initialization routine again
        if (bootstrapPeerQueue.isEmpty() && Utility.hasElapsed(lastInitTimestamp, 300)) {
            init();
        }

        return bootstrapPeerQueue.poll();
    }

    public List<PeerEndpoint> getNext(int count) {
        List<PeerEndpoint> peers = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            PeerEndpoint peer = getNext();
            if (peer != null) {
                peers.add(peer);
            } else {
                break;
            }
        }

        return peers;
    }

    private void init() {
        Set<PeerEndpoint> peerPool = new HashSet<>();

        if (bootstrapPeers != null && bootstrapPeers.size() > 0) {
            for (String peer : bootstrapPeers) {
                if (peer == null || peer.length() == 0) continue;

                try {
                    peerPool.add(new PeerEndpoint(peer, networkParameters.getP2PPort()));
                } catch (Exception e) {
                    logger.error("Unable to add bootstrap peer '{}'", peer, e);
                }
            }
        }

        if (dnsSeeds != null && dnsSeeds.size() > 0) {
            for (String dns : dnsSeeds) {
                try {
                    List<String> ipAddresses = dnsResolver.query(dns);
                    ipAddresses.forEach(ip -> peerPool.add(new PeerEndpoint(ip, networkParameters.getP2PPort())));
                } catch (Exception e) {
                    logger.error("Could not query '{}' for bootstrap nodes", dns, e);
                }
            }
        }

        // Randomize
        List<PeerEndpoint> pool = new ArrayList<>(peerPool);
        Collections.shuffle(pool);

        bootstrapPeerQueue.clear();
        bootstrapPeerQueue.addAll(pool);
        lastInitTimestamp = Utility.getCurrentTimeSeconds();
    }
}
