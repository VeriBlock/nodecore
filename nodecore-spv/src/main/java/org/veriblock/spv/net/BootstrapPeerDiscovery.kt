// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.spv.net

import io.ktor.util.network.NetworkAddress
import nodecore.p2p.DnsResolver
import org.veriblock.core.params.NetworkParameters
import org.veriblock.core.utilities.createLogger
import java.util.ArrayList

private val logger = createLogger {}

/**
 * Discovery peers from bootstrap nodes.
 */
class BootstrapPeerDiscovery(networkParameters: NetworkParameters) : PeerDiscovery {
    private val peers: MutableList<NetworkAddress> = ArrayList()

    init {
        val dnsResolver = DnsResolver()
        val dns = networkParameters.bootstrapDns
        val port = networkParameters.p2pPort
        if(dns == null) {
            logger.info { "Not doing DNS peer discovery, because bootstrapDns is null" }
        } else {
            logger.info { "Doing DNS peer discovery from $dns" }
            try {
                peers.addAll(dnsResolver.query(dns).map {
                    logger.debug("Found peer ${it}:${port}")
                    NetworkAddress(it, port)
                })
            } catch (e: Exception) {
                logger.error(e.message, e)
                throw RuntimeException(e)
            }
        }

    }

    override fun getPeers(count: Int): Collection<NetworkAddress> {
        return peers.shuffled().take(count)
    }
}
