// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package veriblock

import org.veriblock.core.bitcoinj.BitcoinUtilities
import org.veriblock.core.params.NetworkParameters
import org.veriblock.core.utilities.createLogger
import org.veriblock.core.wallet.AddressManager
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
import veriblock.service.Blockchain
import veriblock.service.PendingTransactionContainer
import veriblock.service.TransactionService
import veriblock.wallet.PendingTransactionDownloadedListenerImpl
import java.io.File
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
    lateinit var adminApiService: AdminApiService
        private set
    lateinit var peerTable: SpvPeerTable
        private set
    lateinit var p2PService: P2PService
        private set
    lateinit var addressManager: AddressManager
        private set
    lateinit var transactionService: TransactionService
        private set
    lateinit var pendingTransactionContainer: PendingTransactionContainer
        private set
    lateinit var pendingTransactionDownloadedListener: PendingTransactionDownloadedListener
        private set

    val startTime: Instant = Instant.now()

    /**
     * Initialise context. This method should be call on the start app.
     *
     * @param networkParam   Config for particular network.
     * @param baseDir        will use as a start point for inner directories. (db, wallet so on.)
     * @param filePr         prefix for wallet name. (for example: vbk-MainNet, vbk-TestNet)
     * @param peerDiscovery  discovery peers.
     */
    @Synchronized
    fun init(
        networkParam: NetworkParameters,
        baseDir: File,
        filePr: String,
        peerDiscovery: PeerDiscovery
    ) {
        try {
            Runtime.getRuntime().addShutdownHook(Thread({ shutdown() }, "ShutdownHook nodecore-spv"))
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
            addressManager = AddressManager()
            val walletFile = File(directory, filePrefix + FILE_EXTENSION)
            addressManager.load(walletFile)
            pendingTransactionDownloadedListener = PendingTransactionDownloadedListenerImpl(this)
            peerTable = SpvPeerTable(this, p2PService, peerDiscovery, pendingTransactionContainer)
            transactionService = TransactionService(addressManager, networkParameters)
            adminApiService = AdminApiService(
                this, peerTable, transactionService, addressManager,
                pendingTransactionContainer, blockchain
            )
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
     */
    @Synchronized
    fun init(networkParameters: NetworkParameters, peerDiscovery: PeerDiscovery) {
        init(networkParameters, File("."), String.format("vbk-%s", networkParameters.name), peerDiscovery)
    }

    fun shutdown() {
        peerTable.shutdown()
    }

    private fun init(veriBlockStore: VeriBlockStore, params: NetworkParameters) {
        try {
            if (veriBlockStore.getChainHead() == null) {
                val genesis = params.genesisBlock
                val storedBlock = StoredVeriBlockBlock(
                    genesis, BitcoinUtilities.decodeCompactBits(genesis.difficulty.toLong())
                )
                veriBlockStore.put(storedBlock)
                veriBlockStore.setChainHead(storedBlock)
            }
        } catch (e: SQLException) {
            logger.error(e.message, e)
        }
    }

    private fun init(bitcoinStore: BitcoinStore, params: NetworkParameters) {
        try {
            if (bitcoinStore.getChainHead() == null) {
                val origin = params.bitcoinOriginBlock
                val storedBlock = StoredBitcoinBlock(origin, BitcoinUtilities.decodeCompactBits(origin.difficulty.toLong()), 0)
                bitcoinStore.put(storedBlock)
                bitcoinStore.setChainHead(storedBlock)
            }
        } catch (e: SQLException) {
            logger.error(e.message, e)
        }
    }
}
