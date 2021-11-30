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
import nodecore.api.grpc.RpcBlock
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
import nodecore.p2p.BlockRequest
import nodecore.p2p.P2pConstants
import nodecore.p2p.P2pEventBus
import nodecore.p2p.Peer
import nodecore.p2p.PeerCapabilities
import nodecore.p2p.PeerTable
import nodecore.p2p.TrafficManager
import nodecore.p2p.TransactionRequest
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
import org.veriblock.core.params.allDefaultNetworkParameters
import org.veriblock.core.utilities.BlockUtility
import org.veriblock.core.utilities.Utility
import org.veriblock.core.utilities.createLogger
import org.veriblock.sdk.models.VeriBlockBlock
import org.veriblock.sdk.services.SerializeDeserializeService
import org.veriblock.spv.SpvContext
import org.veriblock.spv.SpvState
import org.veriblock.spv.model.Transaction
import org.veriblock.spv.model.TransactionTypeIdentifier
import org.veriblock.spv.serialization.MessageSerializer
import org.veriblock.spv.service.Blockchain
import org.veriblock.spv.service.NetworkBlock
import org.veriblock.spv.service.PendingTransactionContainer
import org.veriblock.spv.util.Threading
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

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
    private val networkParameters: NetworkParameters = spvContext.config.networkParameters
    private val trafficManager: TrafficManager = TrafficManager()

    private val hashDispatcher = Threading.HASH_EXECUTOR.asCoroutineDispatcher()

    private val bloomFilter = createBloomFilter()

    init {
        P2pEventBus.addBlock.register(this, ::onAddBlock)
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

    fun onAddBlock(event: P2pEvent<RpcBlock>) {
        event.acknowledge()

        val blockHash = ByteStringUtility.byteStringToHex(event.content.hash)
        try {
            if (trafficManager.blockReceived(blockHash, event.producer.addressKey)) {
                // TODO: add mempool management
            } else {
                P2pEventBus.peerMisbehavior.trigger(PeerMisbehaviorEvent(
                    peer = event.producer,
                    reason = PeerMisbehaviorEvent.Reason.UNREQUESTED_BLOCK,
                    message = "Peer sent a block this SPV instance didn't request"
                ))
            }
        } catch (e: Exception) {
            logger.warn("Could not queue network block $blockHash", e)
        }
    }
    
     fun onAddTransaction(event: P2pEvent<RpcTransactionUnion>) {
        event.acknowledge()

        try {
            val txId = extractTxIdFromMessage(event.content)
            if (!txId.isPresent) {
                P2pEventBus.peerMisbehavior.trigger(PeerMisbehaviorEvent(
                    event.producer,
                    PeerMisbehaviorEvent.Reason.INVALID_TRANSACTION,
                    "Peer sent a transaction which txId couldn't be computed"
                ))
                return
            }

            if (!trafficManager.transactionReceived(txId.get(), event.producer.addressKey)) {
                P2pEventBus.peerMisbehavior.trigger(PeerMisbehaviorEvent(
                    peer = event.producer,
                    reason = PeerMisbehaviorEvent.Reason.UNREQUESTED_TRANSACTION,
                    message = "Peer sent a transaction this SPV instance didn't request"
                ))
                return
            }

            val addTransactionResult = pendingTransactionContainer.addNetworkTransaction(event.content)
            if (addTransactionResult == PendingTransactionContainer.AddTransactionResult.INVALID) {
                P2pEventBus.peerMisbehavior.trigger(PeerMisbehaviorEvent(
                    peer = event.producer,
                    reason = PeerMisbehaviorEvent.Reason.INVALID_TRANSACTION,
                    message = "Peer sent a transaction that didn't pass the validations"
                ))
            }
        } catch (e: Exception) {
            logger.warn("Could not queue network transaction", e)
        }

    }

    private fun extractTxIdFromMessage(message: RpcTransactionUnion): Optional<String> {
        return when (message.transactionCase) {
            RpcTransactionUnion.TransactionCase.SIGNED -> Optional.of(ByteStringUtility.byteStringToHex(message.signed.transaction.txId))
            RpcTransactionUnion.TransactionCase.SIGNED_MULTISIG -> Optional.of(ByteStringUtility.byteStringToHex(message.signedMultisig.transaction.txId))
            else -> Optional.empty()
        }
    }

    private val announceLock = ReentrantLock()

    fun onAnnounce(event: P2pEvent<RpcAnnounce>) {
        logger.debug { "Announce received from ${event.producer.address}" }
        event.acknowledge()

        val peer = event.producer
        announceLock.withLock {
            if (peer.state.hasAnnounced()) {
                return
            }

            val nodeInfo = event.content.nodeInfo
            if (nodeInfo.protocolVersion != networkParameters.protocolVersion) {
                logger.warn {
                    val possibleNetworks = allDefaultNetworkParameters.asSequence()
                        .filter { it.protocolVersion == nodeInfo.protocolVersion }
                        .map { it.name }
                        .ifEmpty { sequenceOf("custom") }
                        .joinToString(separator = "/")
                    "Peer ${peer.address} is on protocol version ${nodeInfo.protocolVersion} ($possibleNetworks?) and will be disconnected"
                }
                peer.disconnect()
                return
            }
            val capabilities = PeerCapabilities.parse(nodeInfo.capabilities)
            if (!capabilities.hasCapability(PeerCapabilities.Capability.SpvRequests)) {
                logger.warn { "Peer ${peer.address} has no SPV support. Disconnecting..." }
                peer.disconnect()
                return
            }

            peer.metadata = nodeInfo.toModel()
            peer.reconnectPort = nodeInfo.port

            peerTable.updatePeer(peer)

            peer.state.setAnnounced(true)

            if (event.content.reply) {
                peerTable.announce(peer)
            }

            P2pEventBus.peerConnected.trigger(peer)
        }
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
        logger.debug { "Sending keystone query, last block @ ${keystones[0].height}" }
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
                replyBuilder.addAvailableNodes(peer.metadata.toRpcNodeInfo(peer.address))
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

        if (event.content.headersCount > P2pConstants.PEER_MAX_ADVERTISEMENTS) {
            P2pEventBus.peerMisbehavior.trigger(PeerMisbehaviorEvent(
                peer = event.producer,
                reason = PeerMisbehaviorEvent.Reason.ADVERTISEMENT_SIZE,
                message = "The peer exceeded the maximum allowed blocks in a single advertisement (${event.content.headersCount}, the maximum is ${P2pConstants.PEER_MAX_ADVERTISEMENTS})"
            ))
            return
        }

        val advertiseBlocks = event.content

        logger.debug {
            val lastBlock = MessageSerializer.deserialize(advertiseBlocks.headersList.last())
            "Received advertisement of ${advertiseBlocks.headersList.size} blocks," +
                " height: ${lastBlock.height}"
        }
        val trustHashes = spvContext.config.trustPeerHashes && advertiseBlocks.headersList.size > 10
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

        // download full block bodies to manage mempool
        try {
            val blocksToRequest = ArrayList<BlockRequest>()

            val list = event.content.headersList
            for (h in list) {
                val hash = ByteStringUtility.byteStringToHex(h.hash)

                if (event.producer.state.addSeenBlock(hash, Utility.getCurrentTimeSeconds())) {
                    blocksToRequest.add(BlockRequest(hash, h, event.producer))
                }
            }

            logger.debug {
                "Requesting blocks ${BlockUtility.extractBlockHeightFromBlockHeader(list[0].header.toByteArray())}" +
                    "-${BlockUtility.extractBlockHeightFromBlockHeader(list[list.size - 1].header.toByteArray())} from ${event.producer.address}"
            }

            trafficManager.requestBlocks(blocksToRequest)
        } catch (e: Exception) {
            logger.warn("Unable to handle advertised blocks", e)
        }
    }

    fun onAdvertiseTransactions(event: P2pEvent<RpcAdvertiseTransaction>) {
        logger.debug { "Transaction advertisement from ${event.producer.address}" }
        event.acknowledge()

        if (event.content.transactionsCount > P2pConstants.PEER_MAX_ADVERTISEMENTS) {
            P2pEventBus.peerMisbehavior.trigger(PeerMisbehaviorEvent(
                peer = event.producer,
                reason = PeerMisbehaviorEvent.Reason.ADVERTISEMENT_SIZE,
                message = "The peer exceeded the maximum allowed transactions in a single advertisement (${event.content.transactionsCount}, the maximum is ${P2pConstants.PEER_MAX_ADVERTISEMENTS})"
            ))
            return
        }
        
        // Ignore advertised transactions if the blockchain is not current
        // Commented out because SpvState.localBlockchainHeight never set (always 0)
        //if (SpvState.localBlockchainHeight < SpvState.getNetworkHeight() - P2pConstants.KEYSTONE_BLOCK_INTERVAL) {
        //    return
        //}

        try {
            val requestQueue = ArrayList<TransactionRequest>()
            for (tx in event.content.transactionsList) {
                val txId = ByteStringUtility.byteStringToHex(tx.txId)
                if (event.producer.state.addSeenTransaction(txId, Utility.getCurrentTimeSeconds())) {
                    requestQueue.add(TransactionRequest(txId, tx, event.producer))
                }
            }

            trafficManager.requestTransactions(requestQueue)
        } catch (e: Exception) {
            logger.warn("Unable to handle advertised transactions", e)
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
        this.filter = filter
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
        val addresses = spvContext.wallet.all
        val filter = BloomFilter(
            spvContext.wallet.numAddresses + 10, BLOOM_FILTER_FALSE_POSITIVE_RATE,
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

private fun BloomFilter.isRelevant(tx: Transaction): Boolean {
    if (isEmpty) {
        return true
    }
    if (contains(tx.txId.toString())) {
        return true
    }
    if (contains(tx.inputAddress?.get())) {
        return true
    }
    for (output in tx.getOutputs()) {
        if (contains(output.address.get())) {
            return true
        }
    }
    return false
}
