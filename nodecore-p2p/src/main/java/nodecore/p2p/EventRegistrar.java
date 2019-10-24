// VeriBlock NodeCore
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.p2p;

import nodecore.api.grpc.VeriBlockMessages;
import nodecore.p2p.events.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.veriblock.core.Context;

public class EventRegistrar {
    private static final Logger logger = LoggerFactory.getLogger(EventRegistrar.class);

    public static void newEvent(VeriBlockMessages.Event value, Peer remote) {
        logger.debug("Read event {} from peer {} of type: {}", value.getId(), remote.getAddress(), value.getResultsCase().name());

        if (remote.getState().hasAnnounced()) {
            if (remote.getProtocolVersion() != Context.get().getNetworkParameters().getProtocolVersion()) {
                logger.warn("Peer {} is on protocol version {} and will be disconnected", remote.getAddress(), remote.getProtocolVersion());
                remote.disconnect();
                return;
            }
        } else {
            if (value.getResultsCase() != VeriBlockMessages.Event.ResultsCase.ANNOUNCE) {
                InternalEventBus.getInstance().post(new PeerMisbehaviorEvent(remote, PeerMisbehaviorEvent.Reason.UNANNOUNCED));
                return;
            }
        }

        switch (value.getResultsCase()) {
            case BLOCK:
                InternalEventBus.getInstance().postAsync(new AddBlockStreamEvent(remote, value.getId(), value.getAcknowledge(), value.getBlock()));
                break;
            case TRANSACTION:
                InternalEventBus.getInstance().postAsync(new AddTransactionStreamEvent(remote, value.getId(), value.getAcknowledge(), value.getTransaction()));
                break;
            case ANNOUNCE:
                InternalEventBus.getInstance().postAsync(new AnnounceStreamEvent(remote, value.getId(), value.getAcknowledge(), value.getAnnounce()));
                break;
            case HEARTBEAT:
                InternalEventBus.getInstance().postAsync(new HeartbeatStreamEvent(remote, value.getId(), value.getAcknowledge(), value.getHeartbeat()));
                break;
            case BLOCK_QUERY:
                InternalEventBus.getInstance().postAsync(new BlockQueryStreamEvent(remote, value.getId(), value.getAcknowledge(), value.getBlockQuery()));
                break;
            case ACKNOWLEDGEMENT:
                InternalEventBus.getInstance().postAsync(new AcknowledgeStreamEvent(remote, value.getId(), value.getAcknowledge(), value.getAcknowledgement()));
                break;
            case BLOCK_QUERY_REPLY:
                InternalEventBus.getInstance().postAsync(new BlockQueryReplyStreamEvent(remote, value.getId(), value.getAcknowledge(), value.getBlockQueryReply()));
                break;
            case NETWORK_INFO_REPLY:
                InternalEventBus.getInstance().postAsync(new NetworkInfoReplyStreamEvent(remote, value.getId(), value.getAcknowledge(), value.getNetworkInfoReply()));
                break;
            case NETWORK_INFO_REQUEST:
                InternalEventBus.getInstance().postAsync(new NetworkInfoRequestStreamEvent(remote, value.getId(), value.getAcknowledge(), value.getNetworkInfoRequest()));
                break;
            case ADVERTISE_BLOCKS:
                InternalEventBus.getInstance().postAsync(new AdvertiseBlocksStreamEvent(remote, value.getId(), value.getAcknowledge(), value.getAdvertiseBlocks()));
                break;
            case BLOCK_REQUEST:
                InternalEventBus.getInstance().postAsync(new BlockRequestStreamEvent(remote, value.getId(), value.getAcknowledge(), value.getBlockRequest()));
                break;
            case KEYSTONE_QUERY:
                InternalEventBus.getInstance().postAsync(new KeystoneQueryStreamEvent(remote, value.getId(), value.getAcknowledge(), value.getKeystoneQuery()));
                break;
            case ADVERTISE_TX:
                InternalEventBus.getInstance().postAsync(new AdvertiseTransactionStreamEvent(remote, value.getId(), value.getAcknowledge(), value.getAdvertiseTx()));
                break;
            case TX_REQUEST:
                InternalEventBus.getInstance().postAsync(new TransactionRequestStreamEvent(remote, value.getId(), value.getAcknowledge(), value.getTxRequest()));
                break;
            case NOT_FOUND:
                InternalEventBus.getInstance().postAsync(new NotFoundStreamEvent(remote, value.getId(), value.getAcknowledge(), value.getNotFound()));
                break;
            case CREATE_FILTER:
                InternalEventBus.getInstance().postAsync(new CreateFilterStreamEvent(remote, value.getId(), value.getAcknowledge(), value.getCreateFilter()));
                break;
            case ADD_FILTER:
                InternalEventBus.getInstance().postAsync(new AddFilterStreamEvent(remote, value.getId(), value.getAcknowledge(), value.getAddFilter()));
                break;
            case CLEAR_FILTER:
                InternalEventBus.getInstance().postAsync(new ClearFilterStreamEvent(remote, value.getId(), value.getAcknowledge(), value.getClearFilter()));
                break;
            case FILTERED_BLOCK_REQUEST:
                InternalEventBus.getInstance().postAsync(new FilteredBlockRequestStreamEvent(remote, value.getId(), value.getAcknowledge(), value.getFilteredBlockRequest()));
                break;
        }
    }

}
