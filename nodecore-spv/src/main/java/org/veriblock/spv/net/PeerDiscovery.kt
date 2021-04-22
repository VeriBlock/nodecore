// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.spv.net

import io.ktor.util.network.NetworkAddress

/**
 * Discovery peers depends on strategy.
 */
interface PeerDiscovery {
    fun getPeers(): List<NetworkAddress>
    fun name(): String
}
