// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.spv.net

import io.ktor.util.network.NetworkAddress
import org.veriblock.core.params.LOCALHOST
import org.veriblock.core.params.NetworkParameters
import org.veriblock.core.utilities.createLogger

private val logger = createLogger { }

/**
 * Discovery peer locally.
 */
class DirectDiscovery(
    private val addresses: List<NetworkAddress>
) : PeerDiscovery {
    init {
        logger.info { "Doing direct peer discovery. Using peers $addresses" }
    }

    override fun getPeers(): Sequence<NetworkAddress> {
        return addresses.asSequence()
    }

    override fun name(): String {
        return "direct[$addresses]"
    }
}
