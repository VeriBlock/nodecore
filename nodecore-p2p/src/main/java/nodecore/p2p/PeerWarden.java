// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.p2p;

import com.google.common.eventbus.Subscribe;
import nodecore.p2p.events.PeerBannedEvent;
import nodecore.p2p.events.PeerMisbehaviorEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class PeerWarden {
    private static final Logger logger = LoggerFactory.getLogger(PeerWarden.class);

    private final ConcurrentHashMap<String, Integer> peerScores;
    private final int peerBanThreshold;

    public PeerWarden(P2PConfiguration configuration) {
        this.peerScores = new ConcurrentHashMap<>();
        this.peerBanThreshold = configuration.getPeerBanThreshold();
        Threading.PEER_WARDEN_THREAD.scheduleWithFixedDelay(this::reducePenalties, 1L, 1L, TimeUnit.MINUTES);

        InternalEventBus.getInstance().register(this);
    }

    public void shutdown() {
        logger.info("Shutting down peer warden");
        Threading.shutdown(Threading.PEER_WARDEN_THREAD);
    }

    private void handleMisbehavior(Peer peer, PeerMisbehaviorEvent.Reason reason) {
        int penalty = 0;
        switch (reason) {
            case MALFORMED_EVENT:
                penalty = 20;
                break;
            case UNANNOUNCED:
                penalty = 5;
                break;
            case UNREQUESTED_BLOCK:
                // TODO: penalty = 2; but amnesty because of versions prior to 0.3.3
                penalty = 0;
                break;
            case UNREQUESTED_TRANSACTION:
                // TODO: penalty = 2; but amnesty because of versions prior to 0.3.3
                penalty = 0;
                break;
            case INVALID_BLOCK:
                penalty = 20;
                break;
            case INVALID_TRANSACTION:
                penalty = 20;
                break;
            case ADVERTISEMENT_SIZE:
                penalty = 20;
                break;
            case MESSAGE_SIZE:
                penalty = 20;
                break;
            case MESSAGE_SIZE_EXCESSIVE:
                penalty = 100;
                break;
            case UNFULFILLED_REQUEST_LIMIT:
                penalty = 20;
                break;
            case UNKNOWN_BLOCK_REQUESTED:
                penalty = 5;
                break;
            case FREQUENT_KEYSTONE_QUERY:
                // TODO: penalty = 5; but amnesty because of versions prior to 0.3.3
                penalty = 0;
                break;
        }

        int revisedScore = peerScores.getOrDefault(peer.getAddress(), 0) + penalty;
        peerScores.put(peer.getAddress(), revisedScore);

        if (revisedScore >= peerBanThreshold) {
            logger.info("Peer {} has exceeded the ban threshold", peer.getAddress());
            InternalEventBus.getInstance().post(new PeerBannedEvent(peer));
        }
    }

    private void reducePenalties() {
        for (String key : peerScores.keySet()) {
            int score = peerScores.get(key);
            if (score > 0) {
                score--;
            } else {
                score = 0;
            }
            peerScores.put(key, score);
        }
    }

    @Subscribe public void onPeerMisbehavior(PeerMisbehaviorEvent event) {
        logger.info("Peer {} misbehaved: {}", event.getPeer().getAddress(), event.getReason().name());
        handleMisbehavior(event.getPeer(), event.getReason());
    }


}
