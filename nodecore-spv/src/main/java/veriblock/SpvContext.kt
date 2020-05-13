// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package veriblock

import io.grpc.Server
import io.grpc.ServerInterceptors
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder
import org.veriblock.core.bitcoinj.BitcoinUtilities
import org.veriblock.core.contracts.AddressManager
import org.veriblock.core.params.NetworkParameters
import org.veriblock.core.utilities.createLogger
import org.veriblock.core.wallet.DefaultAddressManager
import org.veriblock.sdk.blockchain.store.BitcoinStore
import org.veriblock.sdk.blockchain.store.StoredBitcoinBlock
import org.veriblock.sdk.blockchain.store.StoredVeriBlockBlock
import org.veriblock.sdk.blockchain.store.VeriBlockStore
import org.veriblock.sdk.sqlite.ConnectionSelector
import org.veriblock.sdk.sqlite.FileManager
import veriblock.listeners.PendingTransactionDownloadedListener
import veriblock.model.TransactionPool
import veriblock.net.P2PService
import veriblock.net.PeerDiscovery
import veriblock.net.SpvPeerTable
import veriblock.service.AdminApiService
import veriblock.service.AdminServerInterceptor
import veriblock.service.AdminServiceFacade
import veriblock.service.Blockchain
import veriblock.service.PendingTransactionContainer
import veriblock.service.TransactionFactory
import veriblock.service.TransactionService
import veriblock.wallet.PendingTransactionDownloadedListenerImpl
import java.io.File
import java.io.IOException
import java.net.InetSocketAddress
import java.nio.file.Paths
import java.sql.SQLException
import java.time.Instant

const val FILE_EXTENSION = ".vbkwallet"

private val logger = createLogger {}

/**
 * Initialize and hold beans/classes.
 * Required initialization. Context.init(....)
 */
class SpvContext {
    lateinit var networkParameters: NetworkParameters
        private set
    lateinit var directory: File
        private set
    lateinit var filePrefix: String
        private set
    lateinit var transactionPool: TransactionPool
        private set
    lateinit var veriBlockStore: VeriBlockStore
        private set
    lateinit var bitcoinStore: BitcoinStore
        private set
    lateinit var blockchain: Blockchain
        private set
    val startTime = Instant.now()
    lateinit var adminApiService: AdminApiService
        private set
    lateinit var adminService: AdminServiceFacade
        private set
    lateinit var adminServerInterceptor: AdminServerInterceptor
        private set
    lateinit var peerTable: SpvPeerTable
        private set
    lateinit var p2PService: P2PService
        private set
    var server: Server? = null
        private set
    lateinit var addressManager: AddressManager
        private set
    lateinit var transactionService: TransactionService
        private set
    lateinit var pendingTransactionContainer: PendingTransactionContainer
        private set
    lateinit var pendingTransactionDownloadedListener: PendingTransactionDownloadedListener
        private set

    /**
     * Initialise context. This method should be call on the start app.
     *
     * @param networkParam   Config for particular network.
     * @param baseDir        will use as a start point for inner directories. (db, wallet so on.)
     * @param filePr         prefix for wallet name. (for example: vbk-MainNet, vbk-TestNet)
     * @param peerDiscovery  discovery peers.
     * @param runAdminServer Start Admin RPC service. (It can be not necessary for tests.)
     */
    @Synchronized
    fun init(
        networkParam: NetworkParameters,
        baseDir: File,
        filePr: String,
        peerDiscovery: PeerDiscovery,
        runAdminServer: Boolean
    ) {
        try {
            Runtime.getRuntime().addShutdownHook(Thread(Runnable { shutdown() }, "ShutdownHook nodecore-spv"))
            networkParameters = networkParam
            directory = baseDir
            filePrefix = filePr
            val databasePath = Paths.get(
                FileManager.getDataDirectory(), networkParam.databaseName
            ).toString()
            veriBlockStore = VeriBlockStore(ConnectionSelector.setConnection(databasePath))
            bitcoinStore = BitcoinStore(ConnectionSelector.setConnection(databasePath))
            init(veriBlockStore, networkParameters)
            init(bitcoinStore, networkParameters)
            transactionPool = TransactionPool()
            blockchain = Blockchain(networkParameters.genesisBlock, veriBlockStore)
            pendingTransactionContainer = PendingTransactionContainer()
            p2PService = P2PService(pendingTransactionContainer, networkParameters)
            addressManager = DefaultAddressManager()
            val walletFile = File(directory, filePrefix + FILE_EXTENSION)
            addressManager.load(walletFile)
            pendingTransactionDownloadedListener = PendingTransactionDownloadedListenerImpl(this)
            peerTable = SpvPeerTable(this, p2PService, peerDiscovery, pendingTransactionContainer)
            transactionService = TransactionService(addressManager, networkParameters)
            adminApiService = AdminApiService(
                this, peerTable, transactionService, addressManager, TransactionFactory(networkParameters),
                pendingTransactionContainer, blockchain
            )
            adminService = AdminServiceFacade(adminApiService)
            adminServerInterceptor = AdminServerInterceptor()
            if (runAdminServer) {
                server = createAdminServer()
            }
        } catch (e: Exception) {
            logger.error("Could not initialize VeriBlock security", e)
            throw RuntimeException(e)
        }
    }

    /**
     * Initialise context. This method should be call on the start app.
     *
     * @param networkParameters Config for particular network.
     * @param peerDiscovery     discovery peers.
     * @param runAdminServer    Start Admin RPC service. (It can be not necessary for tests.)
     */
    @Synchronized
    fun init(networkParameters: NetworkParameters, peerDiscovery: PeerDiscovery, runAdminServer: Boolean) {
        init(networkParameters, File("."), String.format("vbk-%s", networkParameters.name), peerDiscovery, runAdminServer)
    }

    fun shutdown() {
        peerTable.shutdown()
        server?.shutdown()
    }

    @Throws(NumberFormatException::class)
    private fun createAdminServer(): Server {
        return try {
            val rpcBindAddress = InetSocketAddress(networkParameters.rpcHost, networkParameters.rpcPort)
            logger.info("Starting Admin RPC service on {}", rpcBindAddress)
            NettyServerBuilder.forAddress(rpcBindAddress).addService(ServerInterceptors.intercept(adminService, adminServerInterceptor))
                .build()
                .start()
        } catch (ex: IOException) {
            throw RuntimeException(
                "Can't run admin RPC service. Address already in use " + networkParameters.rpcHost + ":" + networkParameters
                    .rpcPort
            )
        }
    }

    private fun init(veriBlockStore: VeriBlockStore, params: NetworkParameters?) {
        try {
            if (veriBlockStore.chainHead == null) {
                val genesis = params!!.genesisBlock
                val storedBlock = StoredVeriBlockBlock(
                    genesis, BitcoinUtilities.decodeCompactBits(genesis.difficulty.toLong())
                )
                veriBlockStore.put(storedBlock)
                veriBlockStore.chainHead = storedBlock
            }
        } catch (e: SQLException) {
            logger.error(e.message, e)
        }
    }

    private fun init(bitcoinStore: BitcoinStore, params: NetworkParameters?) {
        try {
            if (bitcoinStore.chainHead == null) {
                val origin = params!!.bitcoinOriginBlock
                val storedBlock = StoredBitcoinBlock(origin, BitcoinUtilities.decodeCompactBits(origin.bits.toLong()), 0)
                bitcoinStore.put(storedBlock)
                bitcoinStore.chainHead = storedBlock
            }
        } catch (e: SQLException) {
            logger.error(e.message, e)
        }
    }
}
