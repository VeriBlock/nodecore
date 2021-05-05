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
import nodecore.api.grpc.RpcGetDebugVTBsReply
import nodecore.api.grpc.RpcGetDebugVTBsRequest
import nodecore.api.grpc.RpcGetStateInfoReply
import nodecore.api.grpc.RpcGetStateInfoRequest
import nodecore.api.grpc.RpcGetTransactionReply
import nodecore.api.grpc.RpcGetTransactionRequest
import nodecore.api.grpc.RpcGetVeriBlockPublicationsReply
import nodecore.api.grpc.RpcGetVeriBlockPublicationsRequest
import nodecore.api.grpc.RpcHeartbeat
import nodecore.api.grpc.RpcKeystoneQuery
import nodecore.api.grpc.RpcLedgerProofReply
import nodecore.api.grpc.RpcLedgerProofRequest
import nodecore.api.grpc.RpcNetworkInfoReply
import nodecore.api.grpc.RpcNetworkInfoRequest
import nodecore.api.grpc.RpcNotFound
import nodecore.api.grpc.RpcTransactionRequest
import nodecore.api.grpc.RpcTransactionUnion
import nodecore.p2p.events.P2pEvent
import nodecore.p2p.events.PeerMisbehaviorEvent
import nodecore.p2p.events.toP2pEvent
import org.veriblock.core.Context
import org.veriblock.core.utilities.AsyncEvent
import org.veriblock.core.utilities.Event
import org.veriblock.core.utilities.createLogger

object P2pEventBus {
    private val logger = createLogger {}

    private val asyncEventDispatcher = Threading.P2P_EVENT_BUS_POOL.asCoroutineDispatcher()

    val addBlock = AsyncEvent<P2pEvent<RpcBlock>>("Add block", asyncEventDispatcher)
    val addTransaction = AsyncEvent<P2pEvent<RpcTransactionUnion>>("Add transaction", asyncEventDispatcher)
    val announce = AsyncEvent<P2pEvent<RpcAnnounce>>("Announce", asyncEventDispatcher)
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
    val getDebugVtbsRequest = AsyncEvent<P2pEvent<RpcGetDebugVTBsRequest>>("Get debug VTBs request", asyncEventDispatcher)
    val getDebugVtbsReply = AsyncEvent<P2pEvent<RpcGetDebugVTBsReply>>("Get debug VTBs reply", asyncEventDispatcher)
    val getStateInfoRequest = AsyncEvent<P2pEvent<RpcGetStateInfoRequest>>("Get state info request", asyncEventDispatcher)
    val getStateInfoReply = AsyncEvent<P2pEvent<RpcGetStateInfoReply>>("Get state info reply", asyncEventDispatcher)

    val externalPeerAdded = Event<NetworkAddress>("External peer added")
    val externalPeerRemoved = Event<NetworkAddress>("External peer removed")

    val peerConnected = Event<Peer>("Peer connected")
    val peerDisconnected = Event<Peer>("Peer disconnected")
    val peerBanned = Event<Peer>("Peer banned")

    val peerMisbehavior = Event<PeerMisbehaviorEvent>("Peer misbehavior")
    
    fun newEvent(value: RpcEvent, remote: Peer) {
        logger.debug { "Read event ${value.id} from peer ${remote.address} of type: ${value.resultsCase.name}" }

        if (remote.state.hasAnnounced()) {
            if (remote.protocolVersion != Context.get().networkParameters.protocolVersion) {
                logger.warn { "Peer ${remote.address} is on protocol version ${remote.protocolVersion} and will be disconnected" }
                remote.disconnect()
                return
            }
        } else {
            if (value.resultsCase != RpcEvent.ResultsCase.ANNOUNCE) {
                peerMisbehavior.trigger(PeerMisbehaviorEvent(remote, PeerMisbehaviorEvent.Reason.UNANNOUNCED))
                return
            }
        }

        when (value.resultsCase) {
            RpcEvent.ResultsCase.BLOCK ->
                addBlock.trigger(value.toP2pEvent(remote, value.block))
            RpcEvent.ResultsCase.TRANSACTION ->
                addTransaction.trigger(value.toP2pEvent(remote, value.transaction))
            RpcEvent.ResultsCase.ANNOUNCE ->
                announce.trigger(value.toP2pEvent(remote, value.announce))
            RpcEvent.ResultsCase.HEARTBEAT ->
                heartbeat.trigger(value.toP2pEvent(remote, value.heartbeat))
            RpcEvent.ResultsCase.BLOCK_QUERY ->
                blockQuery.trigger(value.toP2pEvent(remote, value.blockQuery))
            RpcEvent.ResultsCase.ACKNOWLEDGEMENT ->
                acknowledge.trigger(value.toP2pEvent(remote, value.acknowledgement))
            RpcEvent.ResultsCase.BLOCK_QUERY_REPLY ->
                blockQueryReply.trigger(value.toP2pEvent(remote, value.blockQueryReply))
            RpcEvent.ResultsCase.NETWORK_INFO_REPLY ->
                networkInfoReply.trigger(value.toP2pEvent(remote, value.networkInfoReply))
            RpcEvent.ResultsCase.NETWORK_INFO_REQUEST ->
                networkInfoRequest.trigger(value.toP2pEvent(remote, value.networkInfoRequest))
            RpcEvent.ResultsCase.ADVERTISE_BLOCKS ->
                advertiseBlocks.trigger(value.toP2pEvent(remote, value.advertiseBlocks))
            RpcEvent.ResultsCase.BLOCK_REQUEST ->
                blockRequest.trigger(value.toP2pEvent(remote, value.blockRequest))
            RpcEvent.ResultsCase.KEYSTONE_QUERY ->
                keystoneQuery.trigger(value.toP2pEvent(remote, value.keystoneQuery))
            RpcEvent.ResultsCase.ADVERTISE_TX ->
                advertiseTransaction.trigger(value.toP2pEvent(remote, value.advertiseTx))
            RpcEvent.ResultsCase.TX_REQUEST ->
                transactionRequest.trigger(value.toP2pEvent(remote, value.txRequest))
            RpcEvent.ResultsCase.NOT_FOUND ->
                notFound.trigger(value.toP2pEvent(remote, value.notFound))
            RpcEvent.ResultsCase.CREATE_FILTER ->
                createFilter.trigger(value.toP2pEvent(remote, value.createFilter))
            RpcEvent.ResultsCase.ADD_FILTER ->
                addFilter.trigger(value.toP2pEvent(remote, value.addFilter))
            RpcEvent.ResultsCase.CLEAR_FILTER ->
                clearFilter.trigger(value.toP2pEvent(remote, value.clearFilter))
            RpcEvent.ResultsCase.FILTERED_BLOCK_REQUEST ->
                filteredBlockRequest.trigger(value.toP2pEvent(remote, value.filteredBlockRequest))
            RpcEvent.ResultsCase.LEDGER_PROOF_REQUEST ->
                ledgerProofRequest.trigger(value.toP2pEvent(remote, value.ledgerProofRequest))
            RpcEvent.ResultsCase.LEDGER_PROOF_REPLY ->
                ledgerProofReply.trigger(value.toP2pEvent(remote, value.ledgerProofReply))
            RpcEvent.ResultsCase.BLOCK_HEADERS_REQUEST ->
                blockHeadersRequest.trigger(value.toP2pEvent(remote, value.blockHeadersRequest))
            RpcEvent.ResultsCase.TRANSACTION_REQUEST ->
                getTransactionRequest.trigger(value.toP2pEvent(remote, value.transactionRequest))
            RpcEvent.ResultsCase.TRANSACTION_REPLY ->
                getTransactionReply.trigger(value.toP2pEvent(remote, value.transactionReply))
            RpcEvent.ResultsCase.VERIBLOCK_PUBLICATIONS_REQUEST ->
                getVeriBlockPublicationsRequest.trigger(value.toP2pEvent(remote, value.veriblockPublicationsRequest))
            RpcEvent.ResultsCase.VERIBLOCK_PUBLICATIONS_REPLY ->
                getVeriBlockPublicationsReply.trigger(value.toP2pEvent(remote, value.veriblockPublicationsReply))
            RpcEvent.ResultsCase.DEBUG_VTB_REQUEST ->
                getDebugVtbsRequest.trigger(value.toP2pEvent(remote, value.debugVtbRequest))
            RpcEvent.ResultsCase.DEBUG_VTB_REPLY ->
                getDebugVtbsReply.trigger(value.toP2pEvent(remote, value.debugVtbReply))
            RpcEvent.ResultsCase.STATE_INFO_REQUEST ->
                getStateInfoRequest.trigger(value.toP2pEvent(remote, value.stateInfoRequest))
            RpcEvent.ResultsCase.STATE_INFO_REPLY ->
                getStateInfoReply.trigger(value.toP2pEvent(remote, value.stateInfoReply))
        }
    }
}
