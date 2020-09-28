// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package veriblock.net

import com.google.protobuf.ByteString
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.CONFLATED
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import nodecore.api.grpc.VeriBlockMessages
import nodecore.api.grpc.VeriBlockMessages.Event.ResultsCase
import nodecore.api.grpc.VeriBlockMessages.KeystoneQuery
import nodecore.p2p.PeerCapabilities
import org.veriblock.core.crypto.BloomFilter
import org.veriblock.core.utilities.createLogger
import org.veriblock.core.crypto.Sha256Hash
import org.veriblock.core.utilities.BlockUtility
import org.veriblock.sdk.models.VeriBlockBlock
import org.veriblock.sdk.services.SerializeDeserializeService
import veriblock.SpvContext
import veriblock.model.NodeMetadata
import veriblock.service.Blockchain
import veriblock.util.EventBus
import veriblock.util.MessageReceivedEvent
import veriblock.util.buildMessage
import java.util.concurrent.ConcurrentHashMap

private val logger = createLogger {}

class SpvPeer(
    private val spvContext: SpvContext,
    private val blockchain: Blockchain,
    self: NodeMetadata,
    val address: String,
    val port: Int
) {
    private val handler = PeerSocketHandler(this)

    var bestBlockHeight = 0
        private set

    private val expectedResponses: MutableMap<String, Channel<VeriBlockMessages.Event>> = ConcurrentHashMap()

    init {
        handler.start()
        val announce = buildMessage {
            announce = VeriBlockMessages.Announce.newBuilder()
                .setReply(false)
                .setNodeInfo(
                    VeriBlockMessages.NodeInfo.newBuilder().setApplication(self.application)
                        .setProtocolVersion(spvContext.networkParameters.protocolVersion)
                        .setPlatform(self.platform)
                        .setStartTimestamp(self.startTimestamp)
                        .setShare(false)
                        .setId(self.id)
                        .setPort(self.port)
                        .build()
                )
                .build()
        }
        sendMessage(announce)
    }

    fun sendMessage(message: VeriBlockMessages.Event) {
        handler.write(message)
    }

    fun sendMessage(buildBlock: VeriBlockMessages.Event.Builder.() -> Unit) = sendMessage(
        buildMessage(buildBlock = buildBlock)
    )

    fun processMessage(message: VeriBlockMessages.Event) {
        // Handle as an expected response if possible
        expectedResponses[message.requestId]?.offer(message)

        when (message.resultsCase) {
            ResultsCase.ANNOUNCE -> {
                // Set a status to "Open"
                // Extract peer info
                val info = message.announce.nodeInfo
                val capabilities = PeerCapabilities.parse(info.capabilities)
                //if (!capabilities.hasCapability(PeerCapabilities.Capabilities.SpvRequests)) {
                //    logger.warn { "Peer $address has no SPV support. Disconnecting..." }
                //    closeConnection()
                //    return
                //}
                EventBus.peerConnectedEvent.trigger(this)
            }
            ResultsCase.ADVERTISE_BLOCKS -> {
                if (message.advertiseBlocks.headersCount >= 1000) {
                    logger.debug("Received advertisement of {} blocks", message.advertiseBlocks.headersCount)

                    // TODO create a proper coroutine scope for this
                    GlobalScope.launch {
                        // Extract latest keystones and ask for more
                        val extractedKeystones = message.advertiseBlocks.headersList.asSequence()
                            .map {
                                SerializeDeserializeService.parseVeriBlockBlock(
                                    it.header.toByteArray()
                                )
                            }
                            .filter { it.height % 20 == 0 }
                            .sortedByDescending { it.height }
                            .take(10)
                            .toList()
                        logger.debug { "Received keystones ${extractedKeystones.map { it.height }}"}
                        requestBlockDownload(extractedKeystones)
                    }
                } else if (bestBlockHeight == 0) { // FIXME: Remove after we're able to retrieve best block height
                    val lastHeader = message.advertiseBlocks.headersList.last().header.toByteArray()
                    val lastHeight = BlockUtility.extractBlockHeightFromBlockHeader(lastHeader)
                    bestBlockHeight = lastHeight
                }
                notifyMessageReceived(message)
            }
            ResultsCase.ADVERTISE_TX -> {
                val txRequestBuilder = VeriBlockMessages.TransactionRequest.newBuilder()
                val transactions = message.advertiseTx.transactionsList
                for (tx in transactions) {
                    val txId = Sha256Hash.wrap(tx.txId.toByteArray())
                    val broadcastCount = spvContext.transactionPool.record(txId, address)
                    if (broadcastCount == 1) {
                        txRequestBuilder.addTransactions(tx)
                    }
                }
                if (txRequestBuilder.transactionsCount > 0) {
                    sendMessage {
                        setTxRequest(txRequestBuilder)
                    }
                }
            }
            ResultsCase.TRANSACTION -> notifyMessageReceived(message)
            ResultsCase.HEARTBEAT -> {
                // TODO: Need a way to request this or get it sooner than the current cycle time
                bestBlockHeight = message.heartbeat.block.number
                notifyMessageReceived(message)
            }
            ResultsCase.TX_REQUEST -> notifyMessageReceived(message)
            ResultsCase.LEDGER_PROOF_REPLY -> notifyMessageReceived(message)
            ResultsCase.TRANSACTION_REPLY -> notifyMessageReceived(message)
            ResultsCase.DEBUG_VTB_REPLY -> notifyMessageReceived(message)
            ResultsCase.VERIBLOCK_PUBLICATIONS_REPLY -> notifyMessageReceived(message)
            ResultsCase.STATE_INFO_REPLY -> {
                bestBlockHeight = message.stateInfoReply.localBlockchainHeight
            }
            else -> {
                // Ignore the other message types as they are irrelevant for SPV
            }
        }
    }

    fun startBlockchainDownload() {
        /* 1. Notify download is starting
         * 2. Get the peer's best block?
         * 3. Compare against our local blockchain
         * 4. If there's a gap, send a keystone query
         * 5. Notify progress of downloading
         * 6. Allow the advertise to pass through to the handler for adding to blockchain
         * 7. If it was the maximum number of advertisements though, send another keystone query
         */
        requestBlockDownload(blockchain.getPeerQuery())
    }

    fun setFilter(filter: BloomFilter) {
        sendMessage {
            setCreateFilter(
                VeriBlockMessages.CreateFilter.newBuilder()
                    .setFilter(ByteString.copyFrom(filter.bits))
                    .setFlags(BloomFilter.Flags.BLOOM_UPDATE_NONE.Value)
                    .setHashIterations(filter.hashIterations)
                    .setTweak(filter.tweak)
            )
        }
    }

    fun closeConnection() {
        handler.stop()
    }

    private fun requestBlockDownload(keystones: List<VeriBlockBlock>) {
        val queryBuilder = KeystoneQuery.newBuilder()
        for (block in keystones) {
            logger.debug { "Preparing keystone ${block.height}..." }
            queryBuilder.addHeaders(
                VeriBlockMessages.BlockHeader.newBuilder()
                    .setHash(ByteString.copyFrom(block.hash.bytes))
                    .setHeader(ByteString.copyFrom(SerializeDeserializeService.serializeHeaders(block)))
            )
        }
        logger.debug("Sending keystone query, last block @ {}", keystones[0].height)
        sendMessage {
            keystoneQuery = queryBuilder.build()
        }
    }

    /**
     * Sends a P2P request and waits for the peer to respond it during the given timeout (or a default of 2 seconds).
     * This applies to Request/Response event type pairs.
     */
    suspend fun requestMessage(
        request: VeriBlockMessages.Event,
        timeoutInMillis: Long = 5000L
    ): VeriBlockMessages.Event = try {
        // Create conflated channel
        val expectedResponseChannel = Channel<VeriBlockMessages.Event>(CONFLATED)
        // Set this channel as the expected response for the request id
        logger.debug { "Expecting a response to ${request.resultsCase.name} from $address" }
        expectedResponses[request.id] = expectedResponseChannel
        // Send the request
        sendMessage(request)
        // Wait until the expected response arrives (or times out)
        withTimeout(timeoutInMillis) {
            expectedResponseChannel.receive()
        }
    } finally {
        // Unregister the channel
        expectedResponses.remove(request.id)
    }

    private fun notifyMessageReceived(message: VeriBlockMessages.Event) {
        EventBus.messageReceivedEvent.trigger(MessageReceivedEvent(this, message))
    }

    fun onPeerSocketClosed() {
        // Set a status to "Closed"
        EventBus.peerDisconnectedEvent.trigger(this)
    }
}
