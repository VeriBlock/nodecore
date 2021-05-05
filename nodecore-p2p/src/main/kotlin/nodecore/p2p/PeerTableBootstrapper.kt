// VeriBlock NodeCore
// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.p2p

import io.ktor.util.network.NetworkAddress
import java.lang.Exception
import org.veriblock.core.params.NetworkParameters
import org.veriblock.core.utilities.Utility
import org.veriblock.core.utilities.createLogger
import java.util.ArrayDeque

private val logger = createLogger {}

class PeerTableBootstrapper(
    configuration: P2pConfiguration,
    private val dnsResolver: DnsResolver
) {
    private val bootstrapPeerQueue = ArrayDeque<NetworkAddress>()
    private val networkParameters: NetworkParameters = configuration.networkParameters
    private val bootstrapPeers: List<String> = configuration.bootstrapPeers
    private val dnsSeeds: List<String> = configuration.bootstrappingDnsSeeds
    private var lastInitTimestamp: Int = 0

    // If there's nothing left in the queue and it's been five minutes since exhausting the bootstrap peers,
    // run the initialization routine again
    fun getNext(): NetworkAddress? {
        // If there's nothing left in the queue and it's been five minutes since exhausting the bootstrap peers,
        // run the initialization routine again
        if (bootstrapPeerQueue.isEmpty() && Utility.hasElapsed(lastInitTimestamp, 300)) {
            init()
        }

        return bootstrapPeerQueue.poll()
    }
    
    fun getNext(count: Int): List<NetworkAddress> =
        generateSequence { getNext() }
            .take(count)
            .toList()
    
    private fun init() {
        val bootstrapPeersSequence = bootstrapPeers.asSequence().filter {
            it.isNotEmpty()
        }
        val dnsSeedsSequence = dnsSeeds.asSequence().mapNotNull { dns ->
            try {
                dnsResolver.query(dns)
            } catch (e: Exception) {
                logger.error(e) { "Could not query '$dns' for bootstrap nodes" }
                null
            }
        }.flatten()

        val pool = (bootstrapPeersSequence + dnsSeedsSequence).mapNotNull { peer ->
            try {
                NetworkAddress(peer, networkParameters.p2pPort)
            } catch (e: Exception) {
                logger.error(e) { "Unable to add bootstrap peer '$peer'" }
                null
            }
        }.shuffled().toList() // Randomize

        bootstrapPeerQueue.clear()
        bootstrapPeerQueue.addAll(pool)
        lastInitTimestamp = Utility.getCurrentTimeSeconds()
    }
}
