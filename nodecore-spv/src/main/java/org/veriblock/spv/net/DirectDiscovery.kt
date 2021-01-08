// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.spv.net

import io.ktor.util.network.*
import org.veriblock.core.params.LOCALHOST
import org.veriblock.core.params.NetworkParameters
import org.veriblock.core.utilities.createLogger

/**
 * Discovery peer locally.
 */
class DirectDiscovery(
    private val addresses: List<NetworkAddress>
) : PeerDiscovery {

    override fun getPeers(): Sequence<NetworkAddress> {
        return addresses.asSequence()
    }

    override fun name(): String {
        val addrs = if (addresses.isEmpty()) "empty" else "${addresses.joinToString { "$it" } }}"
        return "direct=[$addrs]"
    }
}
