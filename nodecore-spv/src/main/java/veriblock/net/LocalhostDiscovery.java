// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package veriblock.net;

import veriblock.Context;
import veriblock.model.PeerAddress;
import veriblock.params.NetworkParameters;

import java.util.Collection;
import java.util.Collections;

/**
 * Discovery peer locally.
 */
public class LocalhostDiscovery implements PeerDiscovery {

    @Override
    public Collection<PeerAddress> getPeers(int count) {
        return Collections.singletonList(new PeerAddress(NetworkParameters.LOCALHOST, Context.getNetworkParameters().getP2pPort()));
    }
}
