// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package veriblock.net;

import veriblock.model.PeerAddress;

import java.util.Collection;

/**
 * Discovery peers depends on strategy.
 */
public interface PeerDiscovery {
    Collection<PeerAddress> getPeers(int count);
}
