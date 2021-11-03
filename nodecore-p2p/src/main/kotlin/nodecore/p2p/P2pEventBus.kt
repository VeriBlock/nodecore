// VeriBlock NodeCore
// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.p2p

import io.ktor.util.network.NetworkAddress
import kotlinx.coroutines.asCoroutineDispatcher
import nodecore.api.grpc.RpcAcknowledgement
import nodecore.api.grpc.RpcAddFilter
import nodecore.api.grpc.RpcAdvertiseBlocks
import nodecore.api.grpc.RpcAdvertiseTransaction
import nodecore.api.grpc.RpcAnnounce
import nodecore.api.grpc.RpcBlock
import nodecore.api.grpc.RpcBlockHeadersReply
import nodecore.api.grpc.RpcBlockHeadersRequest
import nodecore.api.grpc.RpcBlockQuery
import nodecore.api.grpc.RpcBlockQueryReply
import nodecore.api.grpc.RpcBlockRequest
import nodecore.api.grpc.RpcClearFilter
import nodecore.api.grpc.RpcCreateFilter
import nodecore.api.grpc.RpcEvent
import nodecore.api.grpc.RpcFilteredBlock
import nodecore.api.grpc.RpcGetDebugVtbsReply
import nodecore.api.grpc.RpcGetDebugVtbsRequest
import nodecore.api.grpc.RpcGetStateInfoReply
import nodecore.api.grpc.RpcGetStateInfoRequest
import nodecore.api.grpc.RpcGetTransactionReply
import nodecore.api.grpc.RpcGetTransactionRequest
import nodecore.api.grpc.RpcGetVeriBlockPublicationsReply
import nodecore.api.grpc.RpcGetVeriBlockPublicationsRequest
import nodecore.api.grpc.RpcGetVtbsForBtcBlocksReply
import nodecore.api.grpc.RpcGetVtbsForBtcBlocksRequest
import nodecore.api.grpc.RpcHeartbeat
import nodecore.api.grpc.RpcKeystoneQuery
import nodecore.api.grpc.RpcLedgerProofReply
import nodecore.api.grpc.RpcLedgerProofRequest
import nodecore.api.grpc.RpcNetworkInfoReply
import nodecore.api.grpc.RpcNetworkInfoRequest
import nodecore.api.grpc.RpcNotFound
import nodecore.api.grpc.RpcTransactionRequest
import nodecore.api.grpc.RpcTransactionUnion
import nodecore.p2p.event.P2pEvent
import nodecore.p2p.event.PeerMisbehaviorEvent
import nodecore.p2p.event.toP2pEvent
import org.veriblock.core.Context
import org.veriblock.core.utilities.AsyncEvent
import org.veriblock.core.utilities.Event
import org.veriblock.core.utilities.createLogger

object P2pEventBus {
    private val logger = createLogger {}

    private val asyncEventDispatcher = Threading.P2P_EVENT_BUS_POOL.asCoroutineDispatcher()

    val addBlock = AsyncEvent<P2pEvent<RpcBlock>>("Add block", asyncEventDispatcher)
    val addTransaction = AsyncEvent<P2pEvent<RpcTransactionUnion>>("Add transaction", asyncEventDispatcher)
    val announce = Event<P2pEvent<RpcAnnounce>>("Announce") // Not async because it must be handled immediately on receive
    val heartbeat = AsyncEvent<P2pEvent<RpcHeartbeat>>("Heartbeat", asyncEventDispatcher)
    val blockQuery = AsyncEvent<P2pEvent<RpcBlockQuery>>("Block query", asyncEventDispatcher)
    val blockQueryReply = AsyncEvent<P2pEvent<RpcBlockQueryReply>>("Block query reply", asyncEventDispatcher)
    val acknowledge = AsyncEvent<P2pEvent<RpcAcknowledgement>>("Acknowledge", asyncEventDispatcher)
    val networkInfoRequest = AsyncEvent<P2pEvent<RpcNetworkInfoRequest>>("Network info request", asyncEventDispatcher)
    val networkInfoReply = AsyncEvent<P2pEvent<RpcNetworkInfoReply>>("Network info reply", asyncEventDispatcher)
    val advertiseBlocks = AsyncEvent<P2pEvent<RpcAdvertiseBlocks>>("Advertise blocks", asyncEventDispatcher)
    val blockRequest = AsyncEvent<P2pEvent<RpcBlockRequest>>("Block request", asyncEventDispatcher)
    val keystoneQuery = AsyncEvent<P2pEvent<RpcKeystoneQuery>>("Keystone query", asyncEventDispatcher)
    val advertiseTransaction = AsyncEvent<P2pEvent<RpcAdvertiseTransaction>>("Advertise transaction", asyncEventDispatcher)
    val transactionRequest = AsyncEvent<P2pEvent<RpcTransactionRequest>>("Transaction request", asyncEventDispatcher)
    val notFound = AsyncEvent<P2pEvent<RpcNotFound>>("Not found", asyncEventDispatcher)
    val createFilter = AsyncEvent<P2pEvent<RpcCreateFilter>>("Create filter", asyncEventDispatcher)
    val addFilter = AsyncEvent<P2pEvent<RpcAddFilter>>("Add filter", asyncEventDispatcher)
    val clearFilter = AsyncEvent<P2pEvent<RpcClearFilter>>("Clear filter", asyncEventDispatcher)
    val filteredBlockRequest = AsyncEvent<P2pEvent<RpcBlockRequest>>("Filtered block request", asyncEventDispatcher)
    val filteredBlockReply = AsyncEvent<P2pEvent<RpcFilteredBlock>>("Filtered block reply", asyncEventDispatcher)
    val ledgerProofRequest = AsyncEvent<P2pEvent<RpcLedgerProofRequest>>("Ledger proof request", asyncEventDispatcher)
    val ledgerProofReply = AsyncEvent<P2pEvent<RpcLedgerProofReply>>("Ledger proof reply", asyncEventDispatcher)
    val blockHeadersRequest = AsyncEvent<P2pEvent<RpcBlockHeadersRequest>>("Block headers request", asyncEventDispatcher)
    val blockHeadersReply = AsyncEvent<P2pEvent<RpcBlockHeadersReply>>("Block headers reply", asyncEventDispatcher)
    val getTransactionRequest = AsyncEvent<P2pEvent<RpcGetTransactionRequest>>("Get transaction request", asyncEventDispatcher)
    val getTransactionReply = AsyncEvent<P2pEvent<RpcGetTransactionReply>>("Get transaction reply", asyncEventDispatcher)
    val getVeriBlockPublicationsRequest = AsyncEvent<P2pEvent<RpcGetVeriBlockPublicationsRequest>>("Get VeriBlock publications request", asyncEventDispatcher)
    val getVeriBlockPublicationsReply = AsyncEvent<P2pEvent<RpcGetVeriBlockPublicationsReply>>("Get VeriBlock publications reply", asyncEventDispatcher)
    val getDebugVtbsRequest = AsyncEvent<P2pEvent<RpcGetDebugVtbsRequest>>("Get debug VTBs request", asyncEventDispatcher)
    val getDebugVtbsReply = AsyncEvent<P2pEvent<RpcGetDebugVtbsReply>>("Get debug VTBs reply", asyncEventDispatcher)
    val getStateInfoRequest = AsyncEvent<P2pEvent<RpcGetStateInfoRequest>>("Get state info request", asyncEventDispatcher)
    val getStateInfoReply = AsyncEvent<P2pEvent<RpcGetStateInfoReply>>("Get state info reply", asyncEventDispatcher)
    val getVtbsForBtcBlocksRequest = AsyncEvent<P2pEvent<RpcGetVtbsForBtcBlocksRequest>>("Get VTBs for BTC blocks request", asyncEventDispatcher)
    val getVtbsForBtcBlocksReply = AsyncEvent<P2pEvent<RpcGetVtbsForBtcBlocksReply>>("Get VTBs for BTC blocks reply", asyncEventDispatcher)

    val externalPeerAdded = Event<NetworkAddress>("External peer added")
    val externalPeerRemoved = Event<NetworkAddress>("External peer removed")

    val peerConnected = Event<Peer>("Peer connected")
    val peerDisconnected = Event<Peer>("Peer disconnected")
    val peerBanned = Event<Peer>("Peer banned")

    val peerMisbehavior = Event<PeerMisbehaviorEvent>("Peer misbehavior")
    
    fun newEvent(event: RpcEvent, remote: Peer) {
        logger.debug { "Read event ${event.id} from peer ${remote.address} of type: ${event.resultsCase.name}" }

        if (remote.state.hasAnnounced()) {
            if (remote.protocolVersion != Context.get().networkParameters.protocolVersion) {
                logger.warn { "Peer ${remote.address} is on protocol version ${remote.protocolVersion} and will be disconnected" }
                remote.disconnect()
                return
            }
        } else if (event.resultsCase != RpcEvent.ResultsCase.ANNOUNCE) {
            peerMisbehavior.trigger(PeerMisbehaviorEvent(
                peer = remote,
                reason = PeerMisbehaviorEvent.Reason.UNANNOUNCED,
                message = "The peer sent an event of type ${event.resultsCase} before the ANNOUNCE"
            ))
            return
        }

        when (event.resultsCase) {
            RpcEvent.ResultsCase.BLOCK ->
                addBlock.trigger(event.toP2pEvent(remote, event.block))
            RpcEvent.ResultsCase.TRANSACTION ->
                addTransaction.trigger(event.toP2pEvent(remote, event.transaction))
            RpcEvent.ResultsCase.ANNOUNCE ->
                announce.trigger(event.toP2pEvent(remote, event.announce))
            RpcEvent.ResultsCase.HEARTBEAT ->
                heartbeat.trigger(event.toP2pEvent(remote, event.heartbeat))
            RpcEvent.ResultsCase.BLOCK_QUERY ->
                blockQuery.trigger(event.toP2pEvent(remote, event.blockQuery))
            RpcEvent.ResultsCase.ACKNOWLEDGEMENT ->
                acknowledge.trigger(event.toP2pEvent(remote, event.acknowledgement))
            RpcEvent.ResultsCase.BLOCK_QUERY_REPLY ->
                blockQueryReply.trigger(event.toP2pEvent(remote, event.blockQueryReply))
            RpcEvent.ResultsCase.NETWORK_INFO_REPLY ->
                networkInfoReply.trigger(event.toP2pEvent(remote, event.networkInfoReply))
            RpcEvent.ResultsCase.NETWORK_INFO_REQUEST ->
                networkInfoRequest.trigger(event.toP2pEvent(remote, event.networkInfoRequest))
            RpcEvent.ResultsCase.ADVERTISE_BLOCKS ->
                advertiseBlocks.trigger(event.toP2pEvent(remote, event.advertiseBlocks))
            RpcEvent.ResultsCase.BLOCK_REQUEST ->
                blockRequest.trigger(event.toP2pEvent(remote, event.blockRequest))
            RpcEvent.ResultsCase.KEYSTONE_QUERY ->
                keystoneQuery.trigger(event.toP2pEvent(remote, event.keystoneQuery))
            RpcEvent.ResultsCase.ADVERTISE_TX ->
                advertiseTransaction.trigger(event.toP2pEvent(remote, event.advertiseTx))
            RpcEvent.ResultsCase.TX_REQUEST ->
                transactionRequest.trigger(event.toP2pEvent(remote, event.txRequest))
            RpcEvent.ResultsCase.NOT_FOUND ->
                notFound.trigger(event.toP2pEvent(remote, event.notFound))
            RpcEvent.ResultsCase.CREATE_FILTER ->
                createFilter.trigger(event.toP2pEvent(remote, event.createFilter))
            RpcEvent.ResultsCase.ADD_FILTER ->
                addFilter.trigger(event.toP2pEvent(remote, event.addFilter))
            RpcEvent.ResultsCase.CLEAR_FILTER ->
                clearFilter.trigger(event.toP2pEvent(remote, event.clearFilter))
            RpcEvent.ResultsCase.FILTERED_BLOCK_REQUEST ->
                filteredBlockRequest.trigger(event.toP2pEvent(remote, event.filteredBlockRequest))
            RpcEvent.ResultsCase.FILTERED_BLOCK ->
                filteredBlockReply.trigger(event.toP2pEvent(remote, event.filteredBlock))
            RpcEvent.ResultsCase.LEDGER_PROOF_REQUEST ->
                ledgerProofRequest.trigger(event.toP2pEvent(remote, event.ledgerProofRequest))
            RpcEvent.ResultsCase.LEDGER_PROOF_REPLY ->
                ledgerProofReply.trigger(event.toP2pEvent(remote, event.ledgerProofReply))
            RpcEvent.ResultsCase.BLOCK_HEADERS_REQUEST ->
                blockHeadersRequest.trigger(event.toP2pEvent(remote, event.blockHeadersRequest))
            RpcEvent.ResultsCase.TRANSACTION_REQUEST ->
                getTransactionRequest.trigger(event.toP2pEvent(remote, event.transactionRequest))
            RpcEvent.ResultsCase.TRANSACTION_REPLY ->
                getTransactionReply.trigger(event.toP2pEvent(remote, event.transactionReply))
            RpcEvent.ResultsCase.VERIBLOCK_PUBLICATIONS_REQUEST ->
                getVeriBlockPublicationsRequest.trigger(event.toP2pEvent(remote, event.veriblockPublicationsRequest))
            RpcEvent.ResultsCase.VERIBLOCK_PUBLICATIONS_REPLY ->
                getVeriBlockPublicationsReply.trigger(event.toP2pEvent(remote, event.veriblockPublicationsReply))
            RpcEvent.ResultsCase.DEBUG_VTB_REQUEST ->
                getDebugVtbsRequest.trigger(event.toP2pEvent(remote, event.debugVtbRequest))
            RpcEvent.ResultsCase.DEBUG_VTB_REPLY ->
                getDebugVtbsReply.trigger(event.toP2pEvent(remote, event.debugVtbReply))
            RpcEvent.ResultsCase.STATE_INFO_REQUEST ->
                getStateInfoRequest.trigger(event.toP2pEvent(remote, event.stateInfoRequest))
            RpcEvent.ResultsCase.STATE_INFO_REPLY ->
                getStateInfoReply.trigger(event.toP2pEvent(remote, event.stateInfoReply))
            RpcEvent.ResultsCase.VTB_FOR_BTC_REQUEST ->
                getVtbsForBtcBlocksRequest.trigger(event.toP2pEvent(remote, event.vtbForBtcRequest))
            RpcEvent.ResultsCase.VTB_FOR_BTC_REPLY ->
                getVtbsForBtcBlocksReply.trigger(event.toP2pEvent(remote, event.vtbForBtcReply))
            else ->
                logger.warn { "Unhandled event type: ${event.resultsCase}" }
        }
    }
}
