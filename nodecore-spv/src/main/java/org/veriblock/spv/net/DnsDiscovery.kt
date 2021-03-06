// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2021 Xenios SEZC
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
class DnsDiscovery(
    val dns: String,
    val port: Int
) : PeerDiscovery {
    private val peers: MutableList<NetworkAddress> = ArrayList()

    init {
        val dnsResolver = DnsResolver()
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

    override fun getPeers(): Sequence<NetworkAddress> {
        return peers.asSequence()
    }

    override fun name(): String {
        return "DNS=[${dns}]"
    }
}
