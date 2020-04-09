// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package veriblock.net;

import com.google.protobuf.ByteString;
import nodecore.api.grpc.VeriBlockMessages;
import nodecore.api.grpc.utilities.ByteStringUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.veriblock.core.crypto.BloomFilter;
import org.veriblock.sdk.models.Sha256Hash;
import org.veriblock.sdk.models.VeriBlockBlock;
import org.veriblock.sdk.services.SerializeDeserializeService;
import veriblock.SpvContext;
import veriblock.model.ListenerRegistration;
import veriblock.model.NodeMetadata;
import veriblock.service.impl.Blockchain;
import veriblock.util.MessageIdGenerator;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

public class Peer implements PeerSocketClosedEventListener {
    private static final Logger logger = LoggerFactory.getLogger(Peer.class);

    private final CopyOnWriteArrayList<ListenerRegistration<PeerConnectedEventListener>> connectedEventListeners =
            new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<ListenerRegistration<PeerDisconnectedEventListener>> disconnectedEventListeners =
            new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<ListenerRegistration<MessageReceivedEventListener>> messageReceivedEventListeners =
            new CopyOnWriteArrayList<>();

    private final SpvContext spvContext;
    private final Blockchain blockchain;
    private final NodeMetadata self;
    private final String address;
    private final int port;

    private PeerSocketHandler handler;
    private int bestBlockHeight;
    private String bestBlockHash;

    public Peer(SpvContext spvContext, Blockchain blockchain, NodeMetadata self, String address, int port) {
        this.spvContext = spvContext;
        this.blockchain = blockchain;
        this.self = self;
        this.address = address;
        this.port = port;
    }

    public String getAddress() {
        return this.address;
    }

    public int getPort() {
        return this.port;
    }

    public int getBestBlockHeight() {
        return this.bestBlockHeight;
    }

    public String getBestBlockHash() {
        return this.bestBlockHash;
    }

    public void setConnection(PeerSocketHandler handler) {
        this.handler = handler;
        this.handler.setPeer(this);

        this.handler.start();

        VeriBlockMessages.Event announce = VeriBlockMessages.Event.newBuilder()
                .setId(MessageIdGenerator.next())
                .setAcknowledge(false)
                .setAnnounce(VeriBlockMessages.Announce.newBuilder()
                        .setReply(false)
                        .setNodeInfo(VeriBlockMessages.NodeInfo.newBuilder().setApplication(self.getApplication())
                            .setProtocolVersion(spvContext.getNetworkParameters().getProtocolVersion())
                                .setPlatform(self.getPlatform())
                                .setStartTimestamp(self.getStartTimestamp())
                                .setShare(false)
                                .setId(self.getId())
                                .setPort(self.getPort())
                                .build())
                        .build())
                .build();
        sendMessage(announce);
    }

    public void sendMessage(VeriBlockMessages.Event message) {
        handler.write(message);
    }

    public void processMessage(VeriBlockMessages.Event message) {
        switch (message.getResultsCase()) {
            case ANNOUNCE:
                // Set a status to "Open"
                // Extract peer info

                notifyPeerConnected();
                break;
            case ADVERTISE_BLOCKS:
                if (message.getAdvertiseBlocks().getHeadersCount() >= 1000) {
                    logger.info("Received advertisement of {} blocks", message.getAdvertiseBlocks().getHeadersCount());

                    // Extract latest keystones and ask for more
                    List<VeriBlockBlock> extractedKeystones = message.getAdvertiseBlocks().getHeadersList().stream()
                            .map(blockHeader -> SerializeDeserializeService.parseVeriBlockBlock(blockHeader.getHeader().toByteArray()))
                            .filter(block -> block.getHeight() % 20 == 0)
                            .sorted(Comparator.comparingInt(VeriBlockBlock::getHeight).reversed())
                            .limit(10)
                            .collect(Collectors.toList());

                    requestBlockDownload(extractedKeystones);
                }

                notifyMessageReceived(message);
                break;
            case ADVERTISE_TX:
                VeriBlockMessages.TransactionRequest.Builder txRequestBuilder = VeriBlockMessages.TransactionRequest.newBuilder();

                List<VeriBlockMessages.TransactionAnnounce> transactions = message.getAdvertiseTx().getTransactionsList();
                for (VeriBlockMessages.TransactionAnnounce tx : transactions) {
                    Sha256Hash txId = Sha256Hash.wrap(tx.getTxId().toByteArray());
                    int broadcastCount = spvContext.getTransactionPool().record(txId, getAddress());
                    if (broadcastCount == 1) {
                        txRequestBuilder.addTransactions(tx);
                    }
                }

                if (txRequestBuilder.getTransactionsCount() > 0) {
                    sendMessage(VeriBlockMessages.Event.newBuilder()
                            .setId(MessageIdGenerator.next())
                            .setAcknowledge(false)
                            .setTxRequest(txRequestBuilder)
                            .build());
                }

                break;
            case TRANSACTION:
                notifyMessageReceived(message);
                break;
            case HEARTBEAT:
                // TODO: Need a way to request this or get it sooner than the current cycle time
                bestBlockHeight = message.getHeartbeat().getBlock().getNumber();
                bestBlockHash = ByteStringUtility.byteStringToHex(message.getHeartbeat().getBlock().getHash());

                notifyMessageReceived(message);
                break;

            case TX_REQUEST:
                notifyMessageReceived(message);
                break;

            case LEDGER_PROOF_REPLY:
                notifyMessageReceived(message);
                break;

            case TRANSACTION_REPLY:
                notifyMessageReceived(message);
                break;

            case DEBUG_VTB_REPLY:
                notifyMessageReceived(message);
                break;

            case VERIBLOCK_PUBLICATIONS_REPLY:
                notifyMessageReceived(message);
                break;
        }

    }

    public void startBlockchainDownload() {
        /* 1. Notify download is starting
         * 2. Get the peer's best block?
         * 3. Compare against our local blockchain
         * 4. If there's a gap, send a keystone query
         * 5. Notify progress of downloading
         * 6. Allow the advertise to pass through to the handler for adding to blockchain
         * 7. If it was the maximum number of advertisements though, send another keystone query
         */

        requestBlockDownload(blockchain.getPeerQuery());
    }

    public void setFilter(BloomFilter filter) {
        sendMessage(VeriBlockMessages.Event.newBuilder()
                .setId(MessageIdGenerator.next())
                .setAcknowledge(false)
                .setCreateFilter(VeriBlockMessages.CreateFilter.newBuilder()
                        .setFilter(ByteString.copyFrom(filter.getBits()))
                        .setFlags(BloomFilter.Flags.BLOOM_UPDATE_NONE.Value)
                        .setHashIterations(filter.getHashIterations())
                        .setTweak(filter.getTweak()))
                .build());
    }

    public void closeConnection() {
        handler.stop();
    }

    private void requestBlockDownload(List<VeriBlockBlock> keystones) {
        VeriBlockMessages.KeystoneQuery.Builder queryBuilder = VeriBlockMessages.KeystoneQuery.newBuilder();
        for (VeriBlockBlock block : keystones) {
            queryBuilder.addHeaders(VeriBlockMessages.BlockHeader.newBuilder()
                    .setHash(ByteString.copyFrom(block.getHash().getBytes()))
                    .setHeader(ByteString.copyFrom(SerializeDeserializeService.serializeHeaders(block))));
        }

        logger.info("Sending keystone query, last block @ {}", keystones.get(0).getHeight());
        sendMessage(VeriBlockMessages.Event.newBuilder()
                .setId(MessageIdGenerator.next())
                .setAcknowledge(false)
                .setKeystoneQuery(queryBuilder.build())
                .build());
    }

    public void addConnectedEventListener(Executor executor, PeerConnectedEventListener listener) {
        connectedEventListeners.add(new ListenerRegistration<>(listener, executor));
    }

    private void notifyPeerConnected() {
        for (ListenerRegistration<PeerConnectedEventListener> registration : connectedEventListeners) {
            registration.executor.execute(() -> registration.listener.onPeerConnected(this));
        }
    }

    public void addDisconnectedEventListener(Executor executor, PeerDisconnectedEventListener listener) {
        disconnectedEventListeners.add(new ListenerRegistration<>(listener, executor));
    }

    private void notifyPeerDisconnected() {
        for (ListenerRegistration<PeerDisconnectedEventListener> registration : disconnectedEventListeners) {
            registration.executor.execute(() -> registration.listener.onPeerDisconnected(this));
        }
    }

    public void addMessageReceivedEventListeners(Executor executor, MessageReceivedEventListener listener) {
        messageReceivedEventListeners.add(new ListenerRegistration<>(listener, executor));
    }

    private void notifyMessageReceived(VeriBlockMessages.Event message) {
        for (ListenerRegistration<MessageReceivedEventListener> registration : messageReceivedEventListeners) {
            registration.executor.execute(() -> registration.listener.onMessageReceived(message,this));
        }
    }

    public void onPeerSocketClosed() {
        // Set a status to "Closed"
        notifyPeerDisconnected();
    }
}
