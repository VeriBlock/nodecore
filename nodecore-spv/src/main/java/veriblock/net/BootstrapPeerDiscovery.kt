// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package veriblock.net

import io.ktor.util.network.NetworkAddress
import nodecore.p2p.DnsResolver
import org.veriblock.core.params.NetworkParameters
import org.veriblock.core.utilities.createLogger
import org.xbill.DNS.TextParseException
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
        try {
            peers.addAll(dnsResolver.query(dns).map {
                logger.info("Found peer ${it}:${port}")
                NetworkAddress(it, port)
            })
        } catch (e: TextParseException) {
            logger.error(e.message, e)
            throw RuntimeException(e)
        }
    }

    override fun getPeers(count: Int): Collection<NetworkAddress> {
        return peers.shuffled().take(count)
    }
}
