// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
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
import veriblock.listeners.PendingTransactionDownloadedListener;
import veriblock.model.TransactionPool;
import veriblock.net.P2PService;
import veriblock.net.PeerDiscovery;
import veriblock.net.PeerTable;
import veriblock.net.impl.P2PServiceImpl;
import veriblock.net.impl.PeerTableImpl;
import veriblock.conf.NetworkParameters;
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
public class Context {
    private static final Logger log = LoggerFactory.getLogger(Context.class);

    private static NetworkParameters networkParameters;
    private static File directory;
    private static String filePrefix;
    private static TransactionPool transactionPool;

    private static VeriBlockStore veriBlockStore;
    private static BitcoinStore bitcoinStore;

    private static AuditorChangesStore auditStore;
    private static Blockchain blockchain;
    private static Instant startTime = Instant.now();
    private static AdminApiService adminApiService;
    private static AdminServiceFacade adminService;
    private static AdminServerInterceptor adminServerInterceptor;
    private static PeerTable peerTable;
    private static P2PService p2PService;
    private static Server server;
    private static AddressManager addressManager;
    private static TransactionService transactionService;
    private static PendingTransactionContainer pendingTransactionContainer;
    private static PendingTransactionDownloadedListener pendingTransactionDownloadedListener;

    public static final String FILE_EXTENSION = ".vbkwallet";

    /**
     * Initialise context. This method should be call on the start app.
     *
     * @param networkParam Config for particular network.
     * @param baseDir will use as a start point for inner directories. (db, wallet so on.)
     * @param filePr prefix for wallet name. (for example: vbk-MainNet, vbk-TestNet)
     * @param peerDiscovery discovery peers.
     * @param runAdminServer Start Admin RPC service. (It can be not necessary for tests.)
     */
    public static synchronized void init(NetworkParameters networkParam, File baseDir, String filePr, PeerDiscovery peerDiscovery, boolean runAdminServer) {
        try {
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
            blockchain = new Blockchain(veriBlockStore);



            pendingTransactionContainer = new PendingTransactionContainerImpl();
            p2PService = new P2PServiceImpl(pendingTransactionContainer);

            addressManager = new DefaultAddressManager();
            File walletFile = new File(Context.getDirectory(), Context.getFilePrefix() + FILE_EXTENSION);
            addressManager.load(walletFile);

            peerTable = new PeerTableImpl(p2PService, peerDiscovery);
            transactionService = new TransactionService(addressManager);
            adminApiService = new AdminApiServiceImpl(peerTable, transactionService, addressManager, new TransactionFactoryImpl(),
                    pendingTransactionContainer, blockchain);

            adminService = new AdminServiceFacade(adminApiService);
            adminServerInterceptor = new AdminServerInterceptor();

            if(runAdminServer) {
                server = createAdminServer();
            }

            pendingTransactionDownloadedListener = new PendingTransactionDownloadedListenerImpl();
        } catch (Exception e) {
            log.error("Could not initialize VeriBlock security", e);
            throw new RuntimeException(e);
        }

    }

    /**
     * Initialise context. This method should be call on the start app.
     *
     * @param networkParameters Config for particular network.
     * @param peerDiscovery discovery peers.
     * @param runAdminServer Start Admin RPC service. (It can be not necessary for tests.)
     */
    public static synchronized void init(NetworkParameters networkParameters, PeerDiscovery peerDiscovery, boolean runAdminServer) {
        init(networkParameters, new File("."), String.format("vbk-%s", networkParameters.getNetworkName()), peerDiscovery, runAdminServer);
    }

    private static Server createAdminServer() throws IOException, NumberFormatException {
        InetSocketAddress rpcBindAddress = new InetSocketAddress(Context.getNetworkParameters().getAdminHost(), Context.getNetworkParameters().getAdminPort());
        log.info("Starting Admin RPC service on {} with password {}", rpcBindAddress, Context.getNetworkParameters().getAdminPassword());

        return NettyServerBuilder
                .forAddress(rpcBindAddress)
                .addService(ServerInterceptors.intercept(adminService, adminServerInterceptor))
                .build()
                .start();
    }

    private static void init(VeriBlockStore veriBlockStore, NetworkParameters params){
        try {
            if(veriBlockStore.getChainHead() == null){
                VeriBlockBlock genesis = params.getGenesisBlock();
                StoredVeriBlockBlock storedBlock = new StoredVeriBlockBlock(genesis, BitcoinUtilities.decodeCompactBits(genesis.getDifficulty()));

                veriBlockStore.put(storedBlock);
                veriBlockStore.setChainHead(storedBlock);
            }
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
    }


    private static void init(BitcoinStore bitcoinStore, NetworkParameters params){
        try {
            if(bitcoinStore.getChainHead() == null) {
                BitcoinBlock origin = params.getBitcoinOriginBlock();
                StoredBitcoinBlock storedBlock = new StoredBitcoinBlock(origin, BitcoinUtilities.decodeCompactBits(origin.getBits()), 0);

                bitcoinStore.put(storedBlock);
                bitcoinStore.setChainHead(storedBlock);
            }
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
    }

    public static   NetworkParameters getNetworkParameters() {
        return networkParameters;
    }

    public static File getDirectory() {
        return directory;
    }

    public static String getFilePrefix() {
        return filePrefix;
    }

    public static TransactionPool getTransactionPool() {
        return transactionPool;
    }

    public static VeriBlockStore getVeriBlockStore() {
        return veriBlockStore;
    }

    public static BitcoinStore getBitcoinStore() {
        return bitcoinStore;
    }

    public static AuditorChangesStore getAuditStore() {
        return auditStore;
    }

    public static Blockchain getBlockchain() {
        return blockchain;
    }

    public static Instant getStartTime() {
        return startTime;
    }

    public static AdminApiService getAdminApiService() {
        return adminApiService;
    }

    public static AdminServiceFacade getAdminService() {
        return adminService;
    }

    public static AdminServerInterceptor getAdminServerInterceptor() {
        return adminServerInterceptor;
    }

    public static PeerTable getPeerTable() {
        return peerTable;
    }

    public static P2PService getP2PService() {
        return p2PService;
    }

    public static Server getServer() {
        return server;
    }

    public static PendingTransactionDownloadedListener getPendingTransactionDownloadedListener() {
        return pendingTransactionDownloadedListener;
    }

    public static AddressManager getAddressManager() {
        return addressManager;
    }

    public static TransactionService getTransactionService() {
        return transactionService;
    }

    public static PendingTransactionContainer getPendingTransactionContainer() {
        return pendingTransactionContainer;
    }
}
