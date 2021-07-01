// VeriBlock NodeCore
// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.spv.net

import com.google.protobuf.ByteString
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import nodecore.api.grpc.RpcAcknowledgement
import nodecore.api.grpc.RpcAdvertiseBlocks
import nodecore.api.grpc.RpcAdvertiseTransaction
import nodecore.api.grpc.RpcAnnounce
import nodecore.api.grpc.RpcBlockHeader
import nodecore.api.grpc.RpcCreateFilter
import nodecore.api.grpc.RpcEvent
import nodecore.api.grpc.RpcGetStateInfoReply
import nodecore.api.grpc.RpcGetStateInfoRequest
import nodecore.api.grpc.RpcHeartbeat
import nodecore.api.grpc.RpcKeystoneQuery
import nodecore.api.grpc.RpcNetworkInfoReply
import nodecore.api.grpc.RpcNetworkInfoRequest
import nodecore.api.grpc.RpcNotFound
import nodecore.api.grpc.RpcTransactionRequest
import nodecore.api.grpc.RpcTransactionUnion
import nodecore.api.grpc.utilities.ByteStringUtility
import nodecore.p2p.P2pConstants
import nodecore.p2p.P2pEventBus
import nodecore.p2p.Peer
import nodecore.p2p.PeerCapabilities
import nodecore.p2p.PeerTable
import nodecore.p2p.buildMessage
import nodecore.p2p.event.P2pEvent
import nodecore.p2p.event.PeerMisbehaviorEvent
import nodecore.p2p.nextMessageId
import nodecore.p2p.sendMessage
import nodecore.p2p.toModel
import org.veriblock.core.crypto.BloomFilter
import org.veriblock.core.crypto.asVbkHash
import org.veriblock.core.crypto.asVbkTxId
import org.veriblock.core.params.NetworkParameters
import org.veriblock.core.utilities.BlockUtility
import org.veriblock.core.utilities.createLogger
import org.veriblock.sdk.models.VeriBlockBlock
import org.veriblock.sdk.services.SerializeDeserializeService
import org.veriblock.spv.SpvContext
import org.veriblock.spv.SpvState
import org.veriblock.spv.model.TransactionTypeIdentifier
import org.veriblock.spv.serialization.MessageSerializer
import org.veriblock.spv.service.Blockchain
import org.veriblock.spv.service.NetworkBlock
import org.veriblock.spv.service.PendingTransactionContainer
import org.veriblock.spv.util.SpvEventBus
import org.veriblock.spv.util.Threading

private val logger = createLogger {}

const val BLOOM_FILTER_TWEAK = 710699166
const val BLOOM_FILTER_FALSE_POSITIVE_RATE = 0.02
const val BLOCK_DIFFERENCE_TO_SWITCH_ON_ANOTHER_PEER = 200
const val AMOUNT_OF_BLOCKS_WHEN_WE_CAN_START_WORKING = 4//50

class PeerEventListener(
    private val spvContext: SpvContext,
    private val peerTable: PeerTable,
    private val blockchain: Blockchain,
    private val pendingTransactionContainer: PendingTransactionContainer
) {
    private val networkParameters: NetworkParameters = spvContext.networkParameters

    private val hashDispatcher = Threading.HASH_EXECUTOR.asCoroutineDispatcher()

    private val bloomFilter = createBloomFilter()

    init {
        P2pEventBus.addTransaction.register(this, ::onAddTransaction)
        P2pEventBus.announce.register(this, ::onAnnounce)
        P2pEventBus.heartbeat.register(this, ::onHeartbeat)
        P2pEventBus.networkInfoRequest.register(this, ::onNetworkInfoRequest)
        P2pEventBus.advertiseBlocks.register(this, ::onAdvertiseBlocks)
        P2pEventBus.advertiseTransaction.register(this, ::onAdvertiseTransactions)
        P2pEventBus.transactionRequest.register(this, ::onTransactionRequest)
        P2pEventBus.getStateInfoReply.register(this, ::onGetStateInfoReply)

        P2pEventBus.peerConnected.register(this, ::onPeerConnected)
        P2pEventBus.peerDisconnected.register(this, ::onPeerDisconnected)

    }
    
    private fun onAddTransaction(event: P2pEvent<RpcTransactionUnion>) {
        event.acknowledge()

        // TODO: Different Transaction types
        val standardTransaction = MessageSerializer.deserializeNormalTransaction(event.content)
        SpvEventBus.pendingTransactionDownloadedEvent.trigger(standardTransaction)
    }
    
    fun onAnnounce(event: P2pEvent<RpcAnnounce>) {
        logger.debug { "Announce received from ${event.producer.address}" }
        event.acknowledge()

        val peer = event.producer
        if (peer.state.hasAnnounced()) {
            return
        }

        peer.state.setAnnounced(true)

        val nodeInfo = event.content.nodeInfo
        if (nodeInfo.protocolVersion != networkParameters.protocolVersion) {
            logger.warn { "Peer ${peer.address} is on protocol version ${nodeInfo.protocolVersion} and will be disconnected" }
            peer.disconnect()
            return
        }
        val capabilities = PeerCapabilities.parse(nodeInfo.capabilities)
        if (!capabilities.hasCapability(PeerCapabilities.Capabilities.SpvRequests)) {
            logger.warn { "Peer ${peer.address} has no SPV support. Disconnecting..." }
            peer.disconnect()
            return
        }

        peer.metadata = nodeInfo.toModel()
        peer.reconnectPort = nodeInfo.port

        peerTable.updatePeer(peer)

        if (event.content.reply) {
            peerTable.announce(peer)
        }

        P2pEventBus.peerConnected.trigger(peer)
    }
    
    fun onHeartbeat(event: P2pEvent<RpcHeartbeat>) {
        logger.debug { "Heartbeat received from ${event.producer.address} @ height: ${event.content.block.number}" }
        event.acknowledge()

        val heartbeat = event.content
        val blockInfo = heartbeat.block

        // Network height keeps track of all peers height
        SpvState.putNetworkHeight(event.producer.address, blockInfo.number)

        // Copy reference to local scope
        val downloadPeer = SpvState.downloadPeer
        if (downloadPeer == null && heartbeat.block.number > 0) {
            startBlockchainDownload(event.producer)
        } else if (downloadPeer != null &&
            heartbeat.block.number - (SpvState.getAllPeerHeights()[downloadPeer.addressKey] ?: 0) > BLOCK_DIFFERENCE_TO_SWITCH_ON_ANOTHER_PEER
        ) {
            startBlockchainDownload(event.producer)
        }
    }

    fun startBlockchainDownload(peer: Peer) {
        logger.debug("Beginning blockchain download")
        try {
            SpvState.downloadPeer = peer
            /* 1. Notify download is starting
             * 2. Get the peer's best block?
             * 3. Compare against our local blockchain
             * 4. If there's a gap, send a keystone query
             * 5. Notify progress of downloading
             * 6. Allow the advertise to pass through to the handler for adding to blockchain
             * 7. If it was the maximum number of advertisements though, send another keystone query
             */
            val query = blockchain.getPeerQuery()
            peer.requestBlockDownload(query)
        } catch (ex: Exception) {
            SpvState.downloadPeer = null
            //TODO SPV-70 add bun on some time.
            logger.error(ex.message, ex)
        }
    }

    private fun Peer.requestBlockDownload(keystones: List<VeriBlockBlock>) {
        require(keystones.isNotEmpty()) {
            "Trying to request block download with an empty keystone list!"
        }
        val queryBuilder = RpcKeystoneQuery.newBuilder()
        for (block in keystones) {
            queryBuilder.addHeaders(
                RpcBlockHeader.newBuilder()
                    .setHash(ByteString.copyFrom(block.hash.bytes))
                    .setHeader(ByteString.copyFrom(SerializeDeserializeService.serializeHeaders(block)))
            )
        }
        logger.debug {"Sending keystone query, last block @ ${keystones[0].height}" }
        sendMessage {
            keystoneQuery = queryBuilder.build()
        }
    }
    
    private fun onNetworkInfoRequest(event: P2pEvent<RpcNetworkInfoRequest>) {
        logger.debug("Network info request received from {}", event.producer.address)
        event.acknowledge()

        val availablePeers = peerTable.getConnectedPeers().filter { it.canShareAddress }

        val replyBuilder = RpcNetworkInfoReply.newBuilder()
        for (peer in availablePeers) {
            if (peer.state.hasAnnounced()) {
                replyBuilder.addAvailableNodes(peer.metadata.toRpcNodeInfo())
            } else {
                logger.info { "Not sending along peer ${peer.address}:${peer.port} because they have not yet sent their capabilities." }
            }
        }
        try {
            event.producer.send(
                buildMessage {
                    requestId = event.messageId
                    setNetworkInfoReply(replyBuilder)
                }
            )
        } catch (e: Exception) {
            logger.error(e) { "Unable to reply to network info request" }
        }
    }
    
    suspend fun onAdvertiseBlocks(event: P2pEvent<RpcAdvertiseBlocks>) {
        logger.debug { "Got advertise blocks event from ${event.producer.address}" }
        event.acknowledge()

        val advertiseBlocks = event.content

        logger.debug {
            val lastBlock = MessageSerializer.deserialize(advertiseBlocks.headersList.last())
            "Received advertisement of ${advertiseBlocks.headersList.size} blocks," +
                " height: ${lastBlock.height}"
        }
        val trustHashes = spvContext.trustPeerHashes && advertiseBlocks.headersList.size > 10
        val veriBlockBlocks: List<VeriBlockBlock> = coroutineScope {
            advertiseBlocks.headersList.map {
                async(hashDispatcher) {
                    val block = MessageSerializer.deserialize(it, trustHashes)
                    // pre-calculate hash in parallel
                    block.hash
                    block
                }
            }.awaitAll()
        }
        if (SpvState.downloadPeer == null && veriBlockBlocks.last().height > 0) {
            startBlockchainDownload(event.producer)
        }

        val allBlocksAccepted = veriBlockBlocks
            .sortedBy { it.height }
            .all {
                blockchain.addNetworkBlock(NetworkBlock(it, event.producer))
            }

        if (advertiseBlocks.headersCount >= 1000) {
            if (!allBlocksAccepted) {
                startBlockchainDownload(event.producer)
            } else {
                logger.debug { "Received advertisement of ${advertiseBlocks.headersCount} blocks" }

                // Extract latest keystones and ask for more
                val extractedKeystones = advertiseBlocks.headersList.asSequence()
                    // Using the peer's supplied hash because we're using it only to request more blocks. No exercise of trust is made here.
                    .map { SerializeDeserializeService.parseVeriBlockBlock(it.header.toByteArray(), it.hash.toByteArray().asVbkHash()) }
                    .filter { it.height % 20 == 0 }
                    .sortedByDescending { it.height }
                    .take(10)
                    .toList()
                logger.debug { "Received keystones ${extractedKeystones.map { it.height }}" }
                event.producer.requestBlockDownload(extractedKeystones)
            }
        } else if (SpvState.getPeerHeight(event.producer) == 0) { // FIXME: Remove after we're able to retrieve best block height
            val lastHeader = advertiseBlocks.headersList.last().header.toByteArray()
            val lastHeight = BlockUtility.extractBlockHeightFromBlockHeader(lastHeader)
            SpvState.putNetworkHeight(event.producer.addressKey, lastHeight)
        }

        // TODO(warchant): if allBlocksAccepted == false here, block can not be connected or invalid
        // maybe ban peer? for now, do nothing
    }

    fun onAdvertiseTransactions(event: P2pEvent<RpcAdvertiseTransaction>) {
        logger.debug { "Transaction advertisement from ${event.producer.address}" }
        event.acknowledge()

        if (event.content.transactionsCount > P2pConstants.PEER_MAX_ADVERTISEMENTS) {
            P2pEventBus.peerMisbehavior.trigger(PeerMisbehaviorEvent(event.producer, PeerMisbehaviorEvent.Reason.ADVERTISEMENT_SIZE))
            return
        }
        
        // Ignore advertised transactions if the blockchain is not current
        if (SpvState.localBlockchainHeight < SpvState.getNetworkHeight() - P2pConstants.KEYSTONE_BLOCK_INTERVAL) {
            return
        }

        val txRequestBuilder = RpcTransactionRequest.newBuilder()
        val transactions = event.content.transactionsList
        for (tx in transactions) {
            val txId = tx.txId.toByteArray().asVbkTxId()
            val broadcastCount = spvContext.transactionPool.record(txId, event.producer.addressKey)
            if (broadcastCount == 1) {
                txRequestBuilder.addTransactions(tx)
            }
        }
        if (txRequestBuilder.transactionsCount > 0) {
            event.producer.sendMessage {
                setTxRequest(txRequestBuilder)
            }
        }
    }

    fun onTransactionRequest(event: P2pEvent<RpcTransactionRequest>) {
        event.acknowledge()

        val txIds = event.content.transactionsList.map {
            ByteStringUtility.byteStringToHex(it.txId).asVbkTxId()
        }
        for (txId in txIds) {
            val transaction = pendingTransactionContainer.getTransaction(txId)
            if (transaction != null) {
                logger.debug("Found a transaction for the given transaction id: $txId")
                val builder = RpcEvent.newBuilder()
                    .setId(nextMessageId())
                    .setAcknowledge(false)
                when (transaction.transactionTypeIdentifier) {
                    TransactionTypeIdentifier.STANDARD -> builder.setTransaction(
                        RpcTransactionUnion.newBuilder().setSigned(transaction.getSignedMessageBuilder(networkParameters))
                    )
                    TransactionTypeIdentifier.MULTISIG -> throw UnsupportedOperationException()
                    TransactionTypeIdentifier.PROOF_OF_PROOF -> builder.setTransaction(
                        RpcTransactionUnion.newBuilder().setSigned(transaction.getSignedMessageBuilder(networkParameters))
                    )
                }
                try {
                    event.producer.send(builder.build())
                } catch (e: Exception) {
                    logger.error("Unable to respond to transaction request", e)
                    return
                }
            } else {
                logger.debug("Couldn't find a transaction for the given id $txId")
                val builder = RpcEvent.newBuilder()
                    .setId(nextMessageId())
                    .setAcknowledge(false)
                    .setNotFound(
                        RpcNotFound.newBuilder()
                            .setId(ByteString.copyFrom(txId.bytes))
                            .setType(RpcNotFound.Type.TX)
                    )
                try {
                    event.producer.send(builder.build())
                } catch (e: Exception) {
                    logger.error("Unable to respond to transaction request", e)
                    return
                }
            }
        }
    }

    fun onGetStateInfoReply(event: P2pEvent<RpcGetStateInfoReply>) {
        logger.debug { "State info reply received from ${event.producer.address}" }
        event.acknowledge()

        SpvState.putNetworkHeight(event.producer.addressKey, event.content.localBlockchainHeight)
    }

    fun onPeerConnected(peer: Peer) {
        logger.debug { "Peer ${peer.address} connected" }

        // Wallet related setup (bloom filter)
        peer.setFilter(bloomFilter)

        peer.sendMessage {
            stateInfoRequest = RpcGetStateInfoRequest.getDefaultInstance()
        }

        if (SpvState.downloadPeer == null) {
            startBlockchainDownload(peer)
        }
    }

    fun Peer.setFilter(filter: BloomFilter) {
        sendMessage {
            setCreateFilter(
                RpcCreateFilter.newBuilder()
                    .setFilter(ByteString.copyFrom(filter.bits))
                    .setFlags(BloomFilter.Flags.BLOOM_UPDATE_NONE.Value)
                    .setHashIterations(filter.hashIterations)
                    .setTweak(filter.tweak)
            )
        }
    }

    fun onPeerDisconnected(peer: Peer) {
        if (SpvState.downloadPeer?.address?.equals(peer.address) == true) {
            SpvState.downloadPeer = null
        }
    }

    private fun createBloomFilter(): BloomFilter {
        val addresses = spvContext.addressManager.all
        val filter = BloomFilter(
            spvContext.addressManager.numAddresses + 10, BLOOM_FILTER_FALSE_POSITIVE_RATE,
            BLOOM_FILTER_TWEAK
        )
        for (address in addresses) {
            filter.insert(address.hash)
        }
        return filter
    }

    private fun P2pEvent<*>.acknowledge() {
        if (acknowledgeRequested) {
            val ack = buildMessage {
                acknowledgement = RpcAcknowledgement.newBuilder().also {
                    it.messageId = messageId
                }.build()
            }
            Threading.PEER_TABLE_SCOPE.launch {
                producer.send(ack)
            }
        }
    }
}
