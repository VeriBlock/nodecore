// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package veriblock.net.impl;

import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.protobuf.ByteString;
import nodecore.api.grpc.VeriBlockMessages;
import nodecore.api.grpc.utilities.ByteStringUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.veriblock.core.crypto.BloomFilter;
import org.veriblock.core.wallet.Address;
import org.veriblock.sdk.models.Sha256Hash;
import org.veriblock.sdk.models.VeriBlockBlock;
import org.veriblock.sdk.util.Base58;
import spark.utils.CollectionUtils;
import veriblock.SpvContext;
import veriblock.listeners.PendingTransactionDownloadedListener;
import veriblock.model.DownloadStatus;
import veriblock.model.DownloadStatusResponse;
import veriblock.model.FutureEventReply;
import veriblock.model.LedgerContext;
import veriblock.model.ListenerRegistration;
import veriblock.model.NetworkMessage;
import veriblock.model.NodeMetadata;
import veriblock.model.PeerAddress;
import veriblock.model.StandardTransaction;
import veriblock.model.Transaction;
import veriblock.model.TransactionTypeIdentifier;
import veriblock.model.maper.LedgerProofReplyMapper;
import veriblock.net.MessageReceivedEventListener;
import veriblock.net.P2PService;
import veriblock.net.Peer;
import veriblock.net.PeerConnectedEventListener;
import veriblock.net.PeerDisconnectedEventListener;
import veriblock.net.PeerDiscovery;
import veriblock.net.PeerSocketHandler;
import veriblock.net.PeerTable;
import veriblock.serialization.MessageSerializer;
import veriblock.service.PendingTransactionContainer;
import veriblock.service.impl.Blockchain;
import veriblock.util.MessageIdGenerator;
import veriblock.util.Threading;
import veriblock.validator.LedgerProofReplyValidator;

import java.io.IOException;
import java.net.Socket;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;


public class PeerTableImpl implements PeerTable, PeerConnectedEventListener, PeerDisconnectedEventListener, MessageReceivedEventListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(PeerTableImpl.class);

    public static final int DEFAULT_CONNECTIONS = 12;
    public static final int BLOOM_FILTER_TWEAK = 710699166;
    public static final double BLOOM_FILTER_FALSE_POSITIVE_RATE = 0.02;
    public static final int BLOCK_DIFFERENCE_TO_SWITCH_ON_ANOTHER_PEER = 200;
    public static final int AMOUNT_OF_BLOCKS_WHEN_WE_CAN_START_WORKING = 50;

    private final ReentrantLock lock = new ReentrantLock();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private static final ConcurrentHashMap<String, Peer> peers = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Peer> pendingPeers = new ConcurrentHashMap<>();
    private static final BlockingQueue<NetworkMessage> incomingQueue = new LinkedTransferQueue<>();
    private static final CopyOnWriteArrayList<ListenerRegistration<PendingTransactionDownloadedListener>> pendingTransactionDownloadedEventListeners =
        new CopyOnWriteArrayList<>();

    private static final ExecutorService futureExecutor = Executors.newCachedThreadPool();
    private static final Map<String, FutureEventReply> futureEventReplyList = new ConcurrentHashMap<>();

    private final SpvContext spvContext;
    private final PeerDiscovery discovery;
    private final Blockchain blockchain;
    private final NodeMetadata self = new NodeMetadata();
    private final ListeningScheduledExecutorService executor;
    private final ScheduledExecutorService messageExecutor;

    private int maximumPeers = DEFAULT_CONNECTIONS;
    private Peer downloadPeer;
    private final BloomFilter bloomFilter;
    private final P2PService p2PService;
    private final Map<String, LedgerContext> addressesState = new ConcurrentHashMap<>();
    private final PendingTransactionContainer pendingTransactionContainer;

    public PeerTableImpl(
        SpvContext spvContext,
        P2PService p2PService,
        PeerDiscovery peerDiscovery,
        PendingTransactionContainer pendingTransactionContainer
    ) {
        this.spvContext = spvContext;
        this.p2PService = p2PService;
        this.bloomFilter = createBloomFilter();

        this.blockchain = spvContext.getBlockchain();
        this.executor = MoreExecutors
            .listeningDecorator(Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat("PeerTable Thread").build()));
        this.messageExecutor = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat("Message Handler Thread").build());
        this.discovery = peerDiscovery;
        this.pendingTransactionContainer = pendingTransactionContainer;
        addPendingTransactionDownloadedEventListeners(executor, spvContext.getPendingTransactionDownloadedListener());

    }


    @Override
    public void start() {
        running.set(true);

        discoverPeers();

        this.executor.scheduleAtFixedRate(this::requestAddressState, 5, 60, TimeUnit.SECONDS);
        this.executor.scheduleAtFixedRate(this::discoverPeers, 0, 60, TimeUnit.SECONDS);
        this.executor.scheduleAtFixedRate(this::requestPendingTransactions, 5, 60, TimeUnit.SECONDS);

        // Scheduling with a fixed delay allows it to recover in the event of an unhandled exception
        this.messageExecutor.scheduleWithFixedDelay(this::processIncomingMessages, 1, 1, TimeUnit.SECONDS);
    }

    public void shutdown() {
        running.set(false);

        // Shut down executors
        Threading.shutdown(messageExecutor);
        Threading.shutdown(futureExecutor);
        Threading.shutdown(executor);

        // Close peer connections
        incomingQueue.clear();
        pendingPeers.clear();

        peers.forEachValue(4, Peer::closeConnection);
    }

    public Peer connectTo(PeerAddress address) {
        if (peers.containsKey(address.getAddress())) {
            return peers.get(address.getAddress());
        }

        Peer peer = createPeer(address);
        peer.addConnectedEventListener(Threading.LISTENER_THREAD, this);
        peer.addDisconnectedEventListener(Threading.LISTENER_THREAD, this);

        lock.lock();
        try {
            openConnection(peer);
            pendingPeers.put(address.getAddress(), peer);
            return peer;
        } catch (IOException e) {
            LOGGER.error("Unable to open connection", e);
            return null;
        } finally {
            lock.unlock();
        }
    }

    public Peer createPeer(PeerAddress address) {
        return new Peer(spvContext, blockchain, self, address.getAddress(), address.getPort());
    }

    public void openConnection(Peer peer) throws IOException {
        Socket socket = new Socket(peer.getAddress(), peer.getPort());
        PeerSocketHandler handler = new PeerSocketHandler(socket);

        peer.setConnection(handler);
    }

    public void startBlockchainDownload(Peer peer) {
        LOGGER.info("Beginning blockchain download");
        try {
            setDownloadPeer(peer);
            peer.startBlockchainDownload();
        } catch (Exception ex){
            setDownloadPeer(null);
            //TODO SPV-70 add bun on some time.
            LOGGER.error(ex.getMessage(), ex);
        }
    }

    private void requestAddressState() {
        List<Address> addresses = spvContext.getAddressManager().getAll();
        if (CollectionUtils.isEmpty(addresses)) {
            return;
        }

        VeriBlockMessages.LedgerProofRequest.Builder ledgerProof = VeriBlockMessages.LedgerProofRequest.newBuilder();
        for (Address address : addresses) {
            if (!addressesState.containsKey(address.getHash())) {
                addressesState.put(address.getHash(), new LedgerContext());
            }

            ledgerProof.addAddresses(ByteString.copyFrom(Base58.decode(address.getHash())));
        }

        VeriBlockMessages.Event request = VeriBlockMessages.Event.newBuilder()
            .setId(MessageIdGenerator.next())
            .setAcknowledge(false)
            .setLedgerProofRequest(ledgerProof.build())
            .build();

        LOGGER.info("Request address state.");
        for (Peer peer : peers.values()) {
            peer.sendMessage(request);
        }
    }

    private void requestPendingTransactions() {
        Set<Sha256Hash> pendingTransactionsId = pendingTransactionContainer.getPendingTransactionsId();

        for (Sha256Hash sha256Hash : pendingTransactionsId) {
            VeriBlockMessages.Event request = VeriBlockMessages.Event.newBuilder()
                .setId(MessageIdGenerator.next())
                .setAcknowledge(false)
                .setTransactionRequest(
                    VeriBlockMessages.GetTransactionRequest.newBuilder()
                        .setId(ByteString.copyFrom(sha256Hash.getBytes()))
                        .build()
                )
                .build();

            for (Peer peer : peers.values()) {
                peer.sendMessage(request);
            }
        }
    }

    private void discoverPeers() {
        int maxConnections = getMaximumPeers();
        if (maxConnections > 0 && countConnectedPeers() >= maxConnections) {
            return;
        }

        int needed = maxConnections - (countConnectedPeers() + countPendingPeers());
        if (needed > 0) {
            Collection<PeerAddress> candidates = getDiscovery().getPeers(needed);
            for (PeerAddress address : candidates) {
                if (peers.containsKey(address.getAddress()) || pendingPeers.containsKey(address.getAddress())) {
                    continue;
                }

                LOGGER.info("Attempting connection to {}:{}", address.getAddress(), address.getPort());
                Peer peer = connectTo(address);
                LOGGER.info("Discovered peer connected {}:{}", peer.getAddress(), peer.getPort());
            }
        }
    }

    private void processIncomingMessages() {
        try {
            while (running.get()) {
                try {
                    NetworkMessage message = incomingQueue.take();
                    LOGGER.info("{} message from {}", message.getMessage().getResultsCase().name(), message.getSender().getAddress());

                    switch (message.getMessage().getResultsCase()) {
                        case HEARTBEAT:
                            VeriBlockMessages.Heartbeat heartbeat = message.getMessage().getHeartbeat();

                            if(downloadPeer == null && heartbeat.getBlock().getNumber()>0){
                                startBlockchainDownload(message.getSender());
                            } else if(downloadPeer != null &&
                                    heartbeat.getBlock().getNumber() - downloadPeer.getBestBlockHeight()  > BLOCK_DIFFERENCE_TO_SWITCH_ON_ANOTHER_PEER){
                                startBlockchainDownload(message.getSender());
                            }
                            break;
                        case ADVERTISE_BLOCKS:
                            VeriBlockMessages.AdvertiseBlocks advertiseBlocks = message.getMessage().getAdvertiseBlocks();
                            LOGGER.info("PeerTable Received advertisement of {} blocks, height {}", advertiseBlocks.getHeadersList().size(), blockchain.getChainHead().getHeight());

                            List<VeriBlockBlock> veriBlockBlocks = advertiseBlocks.getHeadersList()
                                    .stream()
                                    .map(MessageSerializer::deserialize)
                                    .collect(Collectors.toList());
                            try {
                                blockchain.addAll(veriBlockBlocks);
                            } catch (SQLException e) {
                                LOGGER.error("Unable to add block to blockchain", e);
                            }

                            break;
                        case TRANSACTION:
                            // TODO: Different Transaction types
                            StandardTransaction standardTransaction = MessageSerializer.deserializeNormalTransaction(message.getMessage().getTransaction());
                            if (standardTransaction != null) {
                                notifyPendingTransactionDownloaded(standardTransaction);
                            }
                            break;

                        case TX_REQUEST:
                            List<Sha256Hash> txIds = message.getMessage().getTxRequest().getTransactionsList().stream()
                                .map(tx -> ByteStringUtility.byteStringToHex(tx.getTxId()))
                                .map(Sha256Hash::wrap)
                                .collect(Collectors.toList());
                            p2PService.onTransactionRequest(txIds, message.getSender());
                            break;

                        case LEDGER_PROOF_REPLY:
                            List<VeriBlockMessages.LedgerProofReply.LedgerProofResult> proofReply =
                                message.getMessage().getLedgerProofReply().getProofsList();
                            List<LedgerContext> ledgerContexts =
                                proofReply.stream().filter(lpr -> addressesState.containsKey(Base58.encode(lpr.getAddress().toByteArray())))
                                    .filter(LedgerProofReplyValidator::validate)
                                    .map(LedgerProofReplyMapper::map)
                                    .collect(Collectors.toList());

                            updateAddressState(ledgerContexts);
                            break;

                        case TRANSACTION_REPLY:
                            pendingTransactionContainer.updateTransactionInfo(message.getMessage().getTransactionReply().getTransaction());
                            break;

                        case DEBUG_VTB_REPLY:
                            // TODO
                            break;

                        case VERIBLOCK_PUBLICATIONS_REPLY:
                            if (futureEventReplyList.containsKey(message.getMessage().getRequestId())) {
                                futureEventReplyList.get(message.getMessage().getRequestId())
                                    .response(message.getMessage());
                            }
                            break;
                    }

                } catch (InterruptedException e) {
                    break;
                }
            }
        } catch (Exception e) {
            LOGGER.error("An unhandled exception occurred processing message queue", e);
        }
    }

    private BloomFilter createBloomFilter() {
        List<Address> addresses = spvContext.getAddressManager().getAll();
        BloomFilter filter =
            new BloomFilter((spvContext.getAddressManager().getNumAddresses() + 10), BLOOM_FILTER_FALSE_POSITIVE_RATE, BLOOM_FILTER_TWEAK);

        for (Address address : addresses) {
            filter.insert(address.getHash());
        }

        return filter;
    }

    private void updateAddressState(List<LedgerContext> ledgerContexts){
        for (LedgerContext ledgerContext : ledgerContexts) {
            if (addressesState.get(ledgerContext.getAddress().getAddress()).compareTo(ledgerContext) > 0) {
                addressesState.replace(ledgerContext.getAddress().getAddress(), ledgerContext);
            }
        }
    }

    public void addPendingTransactionDownloadedEventListeners(Executor executor, PendingTransactionDownloadedListener listener) {
        pendingTransactionDownloadedEventListeners.add(new ListenerRegistration<>(listener, executor));
    }

    private void notifyPendingTransactionDownloaded(StandardTransaction tx) {
        for (ListenerRegistration<PendingTransactionDownloadedListener> registration : pendingTransactionDownloadedEventListeners) {
            registration.executor.execute(() -> registration.listener.onPendingTransactionDownloaded(tx));
        }
    }

    @Override
    public void onPeerConnected(Peer peer) {
        lock.lock();
        try {
            LOGGER.info("Peer {} connected", peer.getAddress());
            pendingPeers.remove(peer.getAddress());
            peers.put(peer.getAddress(), peer);

            // TODO: Wallet related setup (bloom filter)

            // Attach listeners
            peer.addMessageReceivedEventListeners(executor, this);

            peer.setFilter(this.bloomFilter);

        } finally {
            lock.unlock();
        }
    }

    @Override
    public void onPeerDisconnected(Peer peer) {
        lock.lock();
        try {
            pendingPeers.remove(peer.getAddress());
            peers.remove(peer.getAddress());

            if (downloadPeer != null && downloadPeer.getAddress().equalsIgnoreCase(peer.getAddress())) {
                downloadPeer = null;
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void onMessageReceived(VeriBlockMessages.Event message, Peer sender) {
        try {
            LOGGER.info("Message Received messageId: {}, from: {}:{}", message.getId(), sender.getAddress(), sender.getPort());
            incomingQueue.put(new NetworkMessage(sender, message));
        } catch (InterruptedException e) {
            LOGGER.error("onMessageReceived interrupted", e);
        }
    }

    @Override
    public void advertise(Transaction transaction) {
        VeriBlockMessages.Event advertise = VeriBlockMessages.Event.newBuilder()
            .setId(MessageIdGenerator.next())
            .setAcknowledge(false)
            .setAdvertiseTx(VeriBlockMessages.AdvertiseTransaction.newBuilder()
                .addTransactions(VeriBlockMessages.TransactionAnnounce.newBuilder()
                    .setType(transaction.getTransactionTypeIdentifier() == TransactionTypeIdentifier.PROOF_OF_PROOF ?
                        VeriBlockMessages.TransactionAnnounce.Type.PROOF_OF_PROOF :
                        VeriBlockMessages.TransactionAnnounce.Type.NORMAL)
                    .setTxId(ByteString.copyFrom(transaction.getTxId().getBytes()))
                    .build())
                .build())
            .build();

        for (Peer peer : peers.values()) {
            try {
                peer.sendMessage(advertise);
            } catch (Exception ex) {
                LOGGER.error(ex.getMessage(), ex);
            }
        }
    }

    @Override
    public Future<VeriBlockMessages.Event> advertiseWithReply(VeriBlockMessages.Event event) {
        FutureEventReply futureEventReply = new FutureEventReply();

        futureEventReplyList.put(event.getId(), futureEventReply);

        for (Peer peer : peers.values()) {
            try {
                peer.sendMessage(event);
            } catch (Exception ex) {
                LOGGER.error(ex.getMessage(), ex);
            }
        }

        return futureExecutor.submit(() -> {
            while (!futureEventReply.isDone()) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }

            futureEventReplyList.remove(event.getId());
            return futureEventReply.getResponse();
        });
    }

    @Override
    public Long getSignatureIndex(String address) {
        return addressesState.get(address).getLedgerValue() != null ? addressesState.get(address).getLedgerValue().getSignatureIndex() : null;
    }

    @Override
    public Integer getAvailablePeers() {
        return peers.size();
    }

    @Override
    public Integer getBestBlockHeight(){
        return peers.values().stream()
                .map(Peer::getBestBlockHeight)
                .mapToInt(v -> v)
                .max()
                .orElse(0);
    }

    @Override
    public DownloadStatusResponse getDownloadStatus() {
        DownloadStatus status;
        Integer currentHeight = blockchain.getChainHead().getHeight();
        Integer bestBlockHeight = downloadPeer==null ? 0 : downloadPeer.getBestBlockHeight();
        if (downloadPeer == null) {
            status = DownloadStatus.DISCOVERING;
        } else if (bestBlockHeight - currentHeight < AMOUNT_OF_BLOCKS_WHEN_WE_CAN_START_WORKING) {
            status = DownloadStatus.READY;
        } else {
            status = DownloadStatus.DOWNLOADING;
        }

        return new DownloadStatusResponse(status, currentHeight, bestBlockHeight);
    }

    @Override
    public Map<String, LedgerContext> getAddressesState() {
        return addressesState;
    }

    @Override
    public LedgerContext getAddressState(String address) {
        return addressesState.get(address);
    }

    public PeerDiscovery getDiscovery() {
        return discovery;
    }

    public int getMaximumPeers() {
        return maximumPeers;
    }

    public void setMaximumPeers(int maximumPeers) {
        this.maximumPeers = maximumPeers;
    }

    public Peer getDownloadPeer() {
        return downloadPeer;
    }

    public void setDownloadPeer(Peer peer) {
        this.downloadPeer = peer;
    }

    BloomFilter getBloomFilter() {
        return bloomFilter;
    }

    public Collection<Peer> getConnectedPeers() {
        return Collections.unmodifiableCollection(peers.values());
    }

    public int countConnectedPeers() {
        return peers.size();
    }

    public int countPendingPeers() {
        return pendingPeers.size();
    }

}
