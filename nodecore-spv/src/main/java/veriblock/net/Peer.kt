// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package veriblock.net

import com.google.protobuf.ByteString
import nodecore.api.grpc.VeriBlockMessages
import nodecore.api.grpc.VeriBlockMessages.Event.ResultsCase
import nodecore.api.grpc.VeriBlockMessages.KeystoneQuery
import nodecore.api.grpc.utilities.ByteStringUtility
import org.slf4j.LoggerFactory
import org.veriblock.core.crypto.BloomFilter
import org.veriblock.core.utilities.createLogger
import org.veriblock.sdk.models.Sha256Hash
import org.veriblock.sdk.models.VeriBlockBlock
import org.veriblock.sdk.services.SerializeDeserializeService
import veriblock.SpvContext
import veriblock.model.ListenerRegistration
import veriblock.model.NodeMetadata
import veriblock.service.Blockchain
import veriblock.util.MessageIdGenerator.next
import java.util.Comparator
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executor
import java.util.stream.Collectors

private val logger = createLogger {}

class Peer(
    private val spvContext: SpvContext,
    private val blockchain: Blockchain,
    private val self: NodeMetadata,
    val address: String,
    val port: Int
) : PeerSocketClosedEventListener {
    private val connectedEventListeners = CopyOnWriteArrayList<ListenerRegistration<PeerConnectedEventListener>>()
    private val disconnectedEventListeners = CopyOnWriteArrayList<ListenerRegistration<PeerDisconnectedEventListener>>()
    private val messageReceivedEventListeners = CopyOnWriteArrayList<ListenerRegistration<MessageReceivedEventListener>>()
    private lateinit var handler: PeerSocketHandler
    var bestBlockHeight = 0
        private set
    var bestBlockHash: String? = null
        private set

    fun setConnection(handler: PeerSocketHandler) {
        this.handler = handler
        this.handler.setPeer(this)
        this.handler.start()
        val announce = VeriBlockMessages.Event.newBuilder()
            .setId(next())
            .setAcknowledge(false)
            .setAnnounce(
                VeriBlockMessages.Announce.newBuilder()
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
            )
            .build()
        sendMessage(announce)
    }

    fun sendMessage(message: VeriBlockMessages.Event) {
        handler.write(message)
    }

    fun processMessage(message: VeriBlockMessages.Event) {
        when (message.resultsCase) {
            ResultsCase.ANNOUNCE ->                 // Set a status to "Open"
                // Extract peer info
                notifyPeerConnected()
            ResultsCase.ADVERTISE_BLOCKS -> {
                if (message.advertiseBlocks.headersCount >= 1000) {
                    logger.info("Received advertisement of {} blocks", message.advertiseBlocks.headersCount)

                    // Extract latest keystones and ask for more
                    val extractedKeystones = message.advertiseBlocks.headersList.asSequence().map {
                            SerializeDeserializeService.parseVeriBlockBlock(
                                it.header.toByteArray()
                            )
                        }.filter {
                        it.height % 20 == 0
                    }.sortedByDescending {
                        it.height
                    }.take(10).toList()
                    requestBlockDownload(extractedKeystones)
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
                    sendMessage(
                        VeriBlockMessages.Event.newBuilder()
                            .setId(next())
                            .setAcknowledge(false)
                            .setTxRequest(txRequestBuilder)
                            .build()
                    )
                }
            }
            ResultsCase.TRANSACTION -> notifyMessageReceived(message)
            ResultsCase.HEARTBEAT -> {
                // TODO: Need a way to request this or get it sooner than the current cycle time
                bestBlockHeight = message.heartbeat.block.number
                bestBlockHash = ByteStringUtility.byteStringToHex(message.heartbeat.block.hash)
                notifyMessageReceived(message)
            }
            ResultsCase.TX_REQUEST -> notifyMessageReceived(message)
            ResultsCase.LEDGER_PROOF_REPLY -> notifyMessageReceived(message)
            ResultsCase.TRANSACTION_REPLY -> notifyMessageReceived(message)
            ResultsCase.DEBUG_VTB_REPLY -> notifyMessageReceived(message)
            ResultsCase.VERIBLOCK_PUBLICATIONS_REPLY -> notifyMessageReceived(message)
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
        sendMessage(
            VeriBlockMessages.Event.newBuilder()
                .setId(next())
                .setAcknowledge(false)
                .setCreateFilter(
                    VeriBlockMessages.CreateFilter.newBuilder()
                        .setFilter(ByteString.copyFrom(filter.bits))
                        .setFlags(BloomFilter.Flags.BLOOM_UPDATE_NONE.Value)
                        .setHashIterations(filter.hashIterations)
                        .setTweak(filter.tweak)
                )
                .build()
        )
    }

    fun closeConnection() {
        handler.stop()
    }

    private fun requestBlockDownload(keystones: List<VeriBlockBlock>) {
        val queryBuilder = KeystoneQuery.newBuilder()
        for (block in keystones) {
            queryBuilder.addHeaders(
                VeriBlockMessages.BlockHeader.newBuilder()
                    .setHash(ByteString.copyFrom(block.hash.bytes))
                    .setHeader(ByteString.copyFrom(SerializeDeserializeService.serializeHeaders(block)))
            )
        }
        logger.info("Sending keystone query, last block @ {}", keystones[0].height)
        sendMessage(
            VeriBlockMessages.Event.newBuilder()
                .setId(next())
                .setAcknowledge(false)
                .setKeystoneQuery(queryBuilder.build())
                .build()
        )
    }

    fun addConnectedEventListener(executor: Executor?, listener: PeerConnectedEventListener) {
        connectedEventListeners.add(ListenerRegistration(listener, executor!!))
    }

    private fun notifyPeerConnected() {
        for (registration in connectedEventListeners) {
            registration.executor.execute { registration.listener.onPeerConnected(this) }
        }
    }

    fun addDisconnectedEventListener(executor: Executor?, listener: PeerDisconnectedEventListener) {
        disconnectedEventListeners.add(ListenerRegistration(listener, executor!!))
    }

    private fun notifyPeerDisconnected() {
        for (registration in disconnectedEventListeners) {
            registration.executor.execute { registration.listener.onPeerDisconnected(this) }
        }
    }

    fun addMessageReceivedEventListeners(executor: Executor?, listener: MessageReceivedEventListener) {
        messageReceivedEventListeners.add(ListenerRegistration(listener, executor!!))
    }

    private fun notifyMessageReceived(message: VeriBlockMessages.Event) {
        for (registration in messageReceivedEventListeners) {
            registration.executor.execute { registration.listener.onMessageReceived(message, this) }
        }
    }

    override fun onPeerSocketClosed() {
        // Set a status to "Closed"
        notifyPeerDisconnected()
    }

}
