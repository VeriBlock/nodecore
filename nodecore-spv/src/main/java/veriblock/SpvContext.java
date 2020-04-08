// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package veriblock;

import io.grpc.Server;
import io.grpc.ServerInterceptors;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.veriblock.core.bitcoinj.BitcoinUtilities;
import org.veriblock.core.contracts.AddressManager;
import org.veriblock.core.wallet.DefaultAddressManager;
import org.veriblock.sdk.auditor.store.AuditorChangesStore;
import org.veriblock.sdk.blockchain.store.BitcoinStore;
import org.veriblock.sdk.blockchain.store.StoredBitcoinBlock;
import org.veriblock.sdk.blockchain.store.StoredVeriBlockBlock;
import org.veriblock.sdk.blockchain.store.VeriBlockStore;
import org.veriblock.sdk.models.BitcoinBlock;
import org.veriblock.sdk.models.VeriBlockBlock;
import org.veriblock.sdk.sqlite.ConnectionSelector;
import org.veriblock.sdk.sqlite.FileManager;
import veriblock.conf.NetworkParameters;
import veriblock.listeners.PendingTransactionDownloadedListener;
import veriblock.model.TransactionPool;
import veriblock.net.P2PService;
import veriblock.net.PeerDiscovery;
import veriblock.net.PeerTable;
import veriblock.net.impl.P2PServiceImpl;
import veriblock.net.impl.PeerTableImpl;
import veriblock.service.AdminApiService;
import veriblock.service.PendingTransactionContainer;
import veriblock.service.impl.AdminApiServiceImpl;
import veriblock.service.impl.AdminServerInterceptor;
import veriblock.service.impl.AdminServiceFacade;
import veriblock.service.impl.Blockchain;
import veriblock.service.impl.PendingTransactionContainerImpl;
import veriblock.service.impl.TransactionFactoryImpl;
import veriblock.service.impl.TransactionService;
import veriblock.wallet.PendingTransactionDownloadedListenerImpl;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.Instant;

/**
 * Initialize and hold beans/classes.
 * Required initialization. Context.init(....)
 */
public class SpvContext {
    private static final Logger log = LoggerFactory.getLogger(SpvContext.class);

    private NetworkParameters networkParameters;
    private File directory;
    private String filePrefix;
    private TransactionPool transactionPool;

    private VeriBlockStore veriBlockStore;
    private BitcoinStore bitcoinStore;

    private AuditorChangesStore auditStore;
    private Blockchain blockchain;
    private Instant startTime = Instant.now();
    private AdminApiService adminApiService;
    private AdminServiceFacade adminService;
    private AdminServerInterceptor adminServerInterceptor;
    private PeerTable peerTable;
    private P2PService p2PService;
    private Server server;
    private AddressManager addressManager;
    private TransactionService transactionService;
    private PendingTransactionContainer pendingTransactionContainer;
    private PendingTransactionDownloadedListener pendingTransactionDownloadedListener;

    public static final String FILE_EXTENSION = ".vbkwallet";

    /**
     * Initialise context. This method should be call on the start app.
     *
     * @param networkParam   Config for particular network.
     * @param baseDir        will use as a start point for inner directories. (db, wallet so on.)
     * @param filePr         prefix for wallet name. (for example: vbk-MainNet, vbk-TestNet)
     * @param peerDiscovery  discovery peers.
     * @param runAdminServer Start Admin RPC service. (It can be not necessary for tests.)
     */
    public synchronized void init(NetworkParameters networkParam, File baseDir, String filePr, PeerDiscovery peerDiscovery, boolean runAdminServer) {
        try {
            Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown, "ShutdownHook nodecore-spv"));

            networkParameters = networkParam;
            directory = baseDir;
            filePrefix = filePr;

            String databasePath = Paths.get(FileManager.getDataDirectory(), networkParam.getDatabaseName()).toString();
            veriBlockStore = new VeriBlockStore(ConnectionSelector.setConnection(databasePath));
            bitcoinStore = new BitcoinStore(ConnectionSelector.setConnection(databasePath));

            init(veriBlockStore, networkParameters);
            init(bitcoinStore, networkParameters);

            auditStore = new AuditorChangesStore(ConnectionSelector.setConnectionDefault());
            transactionPool = new TransactionPool();
            blockchain = new Blockchain(networkParameters.getGenesisBlock(), veriBlockStore);

            pendingTransactionContainer = new PendingTransactionContainerImpl();
            p2PService = new P2PServiceImpl(pendingTransactionContainer, networkParameters);

            addressManager = new DefaultAddressManager();
            File walletFile = new File(getDirectory(), getFilePrefix() + FILE_EXTENSION);
            addressManager.load(walletFile);

            peerTable = new PeerTableImpl(this, p2PService, peerDiscovery, pendingTransactionContainer);
            transactionService = new TransactionService(addressManager, networkParameters);
            adminApiService =
                new AdminApiServiceImpl(this, peerTable, transactionService, addressManager, new TransactionFactoryImpl(networkParameters),
                    pendingTransactionContainer, blockchain
                );

            adminService = new AdminServiceFacade(adminApiService);
            adminServerInterceptor = new AdminServerInterceptor();

            if (runAdminServer) {
                server = createAdminServer();
            }

            pendingTransactionDownloadedListener = new PendingTransactionDownloadedListenerImpl(this);
        } catch (Exception e) {
            log.error("Could not initialize VeriBlock security", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Initialise context. This method should be call on the start app.
     *
     * @param networkParameters Config for particular network.
     * @param peerDiscovery     discovery peers.
     * @param runAdminServer    Start Admin RPC service. (It can be not necessary for tests.)
     */
    public synchronized void init(NetworkParameters networkParameters, PeerDiscovery peerDiscovery, boolean runAdminServer) {
        init(networkParameters, new File("."), String.format("vbk-%s", networkParameters.getNetworkName()), peerDiscovery, runAdminServer);
    }

    public void shutdown() {
        peerTable.shutdown();
        if (server != null) {
            server.shutdown();
        }
    }

    private Server createAdminServer() throws NumberFormatException {
        try {
            InetSocketAddress rpcBindAddress = new InetSocketAddress(getNetworkParameters().getAdminHost(), getNetworkParameters().getAdminPort());
            log.info("Starting Admin RPC service on {} with password {}", rpcBindAddress, getNetworkParameters().getAdminPassword());

            return NettyServerBuilder.forAddress(rpcBindAddress).addService(ServerInterceptors.intercept(adminService, adminServerInterceptor))
                .build()
                .start();
        } catch (IOException ex) {
            throw new RuntimeException(
                "Can't run admin RPC service. Address already in use " + getNetworkParameters().getAdminHost() + ":" + getNetworkParameters()
                    .getAdminPort());
        }
    }

    private void init(VeriBlockStore veriBlockStore, NetworkParameters params) {
        try {
            if (veriBlockStore.getChainHead() == null) {
                VeriBlockBlock genesis = params.getGenesisBlock();
                StoredVeriBlockBlock storedBlock = new StoredVeriBlockBlock(genesis, BitcoinUtilities.decodeCompactBits(genesis.getDifficulty()));

                veriBlockStore.put(storedBlock);
                veriBlockStore.setChainHead(storedBlock);
            }
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
    }

    private void init(BitcoinStore bitcoinStore, NetworkParameters params) {
        try {
            if (bitcoinStore.getChainHead() == null) {
                BitcoinBlock origin = params.getBitcoinOriginBlock();
                StoredBitcoinBlock storedBlock = new StoredBitcoinBlock(origin, BitcoinUtilities.decodeCompactBits(origin.getBits()), 0);

                bitcoinStore.put(storedBlock);
                bitcoinStore.setChainHead(storedBlock);
            }
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
    }

    public NetworkParameters getNetworkParameters() {
        return networkParameters;
    }

    public File getDirectory() {
        return directory;
    }

    public String getFilePrefix() {
        return filePrefix;
    }

    public TransactionPool getTransactionPool() {
        return transactionPool;
    }

    public VeriBlockStore getVeriBlockStore() {
        return veriBlockStore;
    }

    public BitcoinStore getBitcoinStore() {
        return bitcoinStore;
    }

    public AuditorChangesStore getAuditStore() {
        return auditStore;
    }

    public Blockchain getBlockchain() {
        return blockchain;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public AdminApiService getAdminApiService() {
        return adminApiService;
    }

    public AdminServiceFacade getAdminService() {
        return adminService;
    }

    public AdminServerInterceptor getAdminServerInterceptor() {
        return adminServerInterceptor;
    }

    public PeerTable getPeerTable() {
        return peerTable;
    }

    public P2PService getP2PService() {
        return p2PService;
    }

    public Server getServer() {
        return server;
    }

    public PendingTransactionDownloadedListener getPendingTransactionDownloadedListener() {
        return pendingTransactionDownloadedListener;
    }

    public AddressManager getAddressManager() {
        return addressManager;
    }

    public TransactionService getTransactionService() {
        return transactionService;
    }

    public PendingTransactionContainer getPendingTransactionContainer() {
        return pendingTransactionContainer;
    }
}
