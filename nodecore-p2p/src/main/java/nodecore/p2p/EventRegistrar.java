// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.p2p;

import nodecore.api.grpc.VeriBlockMessages;
import nodecore.p2p.events.AcknowledgeStreamEvent;
import nodecore.p2p.events.AddBlockStreamEvent;
import nodecore.p2p.events.AddFilterStreamEvent;
import nodecore.p2p.events.AddTransactionStreamEvent;
import nodecore.p2p.events.AdvertiseBlocksStreamEvent;
import nodecore.p2p.events.AdvertiseTransactionStreamEvent;
import nodecore.p2p.events.AnnounceStreamEvent;
import nodecore.p2p.events.BlockHeadersRequestStreamEvent;
import nodecore.p2p.events.BlockQueryReplyStreamEvent;
import nodecore.p2p.events.BlockQueryStreamEvent;
import nodecore.p2p.events.BlockRequestStreamEvent;
import nodecore.p2p.events.ClearFilterStreamEvent;
import nodecore.p2p.events.CreateFilterStreamEvent;
import nodecore.p2p.events.FilteredBlockRequestStreamEvent;
import nodecore.p2p.events.GetDebugVTBsReplyStreamEvent;
import nodecore.p2p.events.GetDebugVTBsRequestStreamEvent;
import nodecore.p2p.events.GetTransactionReplyStreamEvent;
import nodecore.p2p.events.GetTransactionRequestStreamEvent;
import nodecore.p2p.events.GetVeriBlockPublicationsReplyStreamEvent;
import nodecore.p2p.events.GetVeriBlockPublicationsRequestStreamEvent;
import nodecore.p2p.events.HeartbeatStreamEvent;
import nodecore.p2p.events.KeystoneQueryStreamEvent;
import nodecore.p2p.events.LedgerProofReplyStreamEvent;
import nodecore.p2p.events.LedgerProofRequestStreamEvent;
import nodecore.p2p.events.NetworkInfoReplyStreamEvent;
import nodecore.p2p.events.NetworkInfoRequestStreamEvent;
import nodecore.p2p.events.NotFoundStreamEvent;
import nodecore.p2p.events.PeerMisbehaviorEvent;
import nodecore.p2p.events.TransactionRequestStreamEvent;
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
            case LEDGER_PROOF_REQUEST:
                InternalEventBus.getInstance().postAsync(new LedgerProofRequestStreamEvent(remote, value.getId(), value.getAcknowledge(), value.getLedgerProofRequest()));
                break;
            case LEDGER_PROOF_REPLY:
                InternalEventBus.getInstance().postAsync(new LedgerProofReplyStreamEvent(remote, value.getId(), value.getAcknowledge(), value.getLedgerProofReply()));
                break;
            case BLOCK_HEADERS_REQUEST:
                InternalEventBus.getInstance().postAsync(new BlockHeadersRequestStreamEvent(remote, value.getId(), value.getAcknowledge(), value.getBlockHeadersRequest()));
                break;
            case TRANSACTION_REQUEST:
                InternalEventBus.getInstance().postAsync(new GetTransactionRequestStreamEvent(remote, value.getId(), value.getAcknowledge(), value.getTransactionRequest()));
                break;
            case TRANSACTION_REPLY:
                InternalEventBus.getInstance().postAsync(new GetTransactionReplyStreamEvent(remote, value.getId(), value.getAcknowledge(), value.getTransactionReply()));
                break;
            case VERIBLOCK_PUBLICATIONS_REQUEST:
                InternalEventBus.getInstance().postAsync(new GetVeriBlockPublicationsRequestStreamEvent(remote, value.getId(), value.getAcknowledge(), value.getVeriblockPublicationsRequest()));
                break;
            case VERIBLOCK_PUBLICATIONS_REPLY:
                InternalEventBus.getInstance().postAsync(new GetVeriBlockPublicationsReplyStreamEvent(remote, value.getId(), value.getAcknowledge(), value.getVeriblockPublicationsReply()));
                break;
            case DEBUG_VTB_REQUEST:
                InternalEventBus.getInstance().postAsync(new GetDebugVTBsRequestStreamEvent(remote, value.getId(), value.getAcknowledge(), value.getDebugVtbRequest()));
                break;
            case DEBUG_VTB_REPLY:
                InternalEventBus.getInstance().postAsync(new GetDebugVTBsReplyStreamEvent(remote, value.getId(), value.getAcknowledge(), value.getDebugVtbReply()));
                break;
        }
    }
}
