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

/**
 * Discovery peer locally.
 */
class LocalhostDiscovery(
    private val networkParameters: NetworkParameters
) : PeerDiscovery {
    init {
        val logger = createLogger { }
        logger.info { "Doing local peer discovery. Using peer ${LOCALHOST}:${networkParameters.p2pPort}." }
    }

    override fun getPeers(count: Int): Collection<NetworkAddress> {
        return listOf(NetworkAddress(LOCALHOST, networkParameters.p2pPort))
    }
}
