// VeriBlock NodeCore
// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.p2p

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.ConcurrentHashMap
import nodecore.p2p.event.PeerMisbehaviorEvent
import org.veriblock.core.utilities.createLogger
import org.veriblock.core.launchWithFixedDelay

private val logger = createLogger {}

class PeerWarden(
    configuration: P2pConfiguration
) {
    private val peerScores: ConcurrentHashMap<String, Int> = ConcurrentHashMap()
    private val peerBanThreshold: Int = configuration.peerBanThreshold

    init {
        CoroutineScope(
            Threading.PEER_WARDEN_THREAD.asCoroutineDispatcher()
        ).launchWithFixedDelay(60, 60) {
            reducePenalties()
        }

        P2pEventBus.peerMisbehavior.register(this) { (peer, reason) ->
            handleMisbehavior(peer, reason)
        }
    }

    fun shutdown() {
        logger.info("Shutting down peer warden")
        Threading.PEER_WARDEN_THREAD.safeShutdown()
    }
    
    private fun handleMisbehavior(peer: Peer, reason: PeerMisbehaviorEvent.Reason) {
        logger.info { "Peer ${peer.address} misbehaved: ${reason.name}" }
        val penalty = when (reason) {
            PeerMisbehaviorEvent.Reason.MALFORMED_EVENT ->           20
            PeerMisbehaviorEvent.Reason.UNANNOUNCED ->                5
            PeerMisbehaviorEvent.Reason.UNREQUESTED_BLOCK ->          0 // TODO: penalty = 2; but amnesty because of versions prior to 0.3.3
            PeerMisbehaviorEvent.Reason.UNREQUESTED_TRANSACTION ->    0 // TODO: penalty = 2; but amnesty because of versions prior to 0.3.3
            PeerMisbehaviorEvent.Reason.INVALID_BLOCK ->             20
            PeerMisbehaviorEvent.Reason.INVALID_TRANSACTION ->       20
            PeerMisbehaviorEvent.Reason.ADVERTISEMENT_SIZE ->        20
            PeerMisbehaviorEvent.Reason.MESSAGE_SIZE ->              20
            PeerMisbehaviorEvent.Reason.MESSAGE_SIZE_EXCESSIVE ->   100
            PeerMisbehaviorEvent.Reason.UNFULFILLED_REQUEST_LIMIT -> 20
            PeerMisbehaviorEvent.Reason.UNKNOWN_BLOCK_REQUESTED ->    5
            PeerMisbehaviorEvent.Reason.FREQUENT_KEYSTONE_QUERY ->    0 // TODO: penalty = 5; but amnesty because of versions prior to 0.3.3
            else ->                                                   0
        }

        val revisedScore = (peerScores[peer.address] ?: 0) + penalty
        peerScores[peer.address] = revisedScore

        if (revisedScore >= peerBanThreshold) {
            logger.info("Peer {} has exceeded the ban threshold", peer.address)
            P2pEventBus.peerBanned.trigger(peer)
        }
    }
    
    private fun reducePenalties() {
        peerScores.replaceAll { _, score ->
            (score - 1).coerceAtLeast(0)
        }
    }
}
