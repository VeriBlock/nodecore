// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package veriblock.net;

import nodecore.p2p.DnsResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.TextParseException;
import veriblock.model.PeerAddress;
import veriblock.conf.NetworkParameters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Discovery peers from bootstrap nodes.
 */
public class BootstrapPeerDiscovery implements PeerDiscovery {
    private static final Logger logger = LoggerFactory.getLogger(BootstrapPeerDiscovery.class);

    private static final List<PeerAddress> peers = new ArrayList<>();

    public BootstrapPeerDiscovery(NetworkParameters networkParameters) {
        DnsResolver dnsResolver = new DnsResolver();
        String dns = networkParameters.getBootstrapDns();
        Integer port  = networkParameters.getP2pPort();
        try {
            peers.addAll(dnsResolver.query(dns).stream()
                    .map(address -> new PeerAddress(address, port))
                    .collect(Collectors.toList()));
        } catch (TextParseException e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public Collection<PeerAddress> getPeers(int count) {
        Collections.shuffle(peers);
        if (count > peers.size()) {
            return peers;
        }
        return peers.stream().limit(count).collect(Collectors.toList());
    }
}
