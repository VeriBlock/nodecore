package org.veriblock.spv.net

import io.ktor.util.network.*
import org.veriblock.core.utilities.createLogger


private val logger = createLogger {}

class MixedDiscovery(
    strategies: List<PeerDiscovery?>
) : PeerDiscovery {
    val strategies: List<PeerDiscovery> = strategies.mapNotNull { it }

    init {
        logger.info { "Doing Mixed peer discovery: ${strategies.joinToString { ", " }}" }
    }

    override fun getPeers(): Sequence<NetworkAddress> {
        return sequenceOf(strategies.flatMap { it.getPeers() }).flatten()
    }

    override fun name(): String {
        return "mixed=[${strategies.map { it.name() }.joinToString { ", " }}]"
    }
}
