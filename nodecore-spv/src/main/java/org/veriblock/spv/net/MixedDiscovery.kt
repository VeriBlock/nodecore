package org.veriblock.spv.net

import io.ktor.util.network.*
import org.veriblock.core.utilities.createLogger

private val logger = createLogger {}

class MixedDiscovery(
    val strategies: List<PeerDiscovery>
) : PeerDiscovery {

    override fun getPeers(): Sequence<NetworkAddress> {
        return sequenceOf(strategies.flatMap { it.getPeers() }).flatten()
    }

    override fun name(): String {
        val str = if(strategies.isEmpty()) "empty, discovery not configured" else strategies.joinToString { it.name() }
        return "Mixed Peer Discovery: $str"
    }
}
