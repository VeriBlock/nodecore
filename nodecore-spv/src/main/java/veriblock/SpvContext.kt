// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package veriblock

import org.veriblock.core.Context
import org.veriblock.core.bitcoinj.BitcoinUtilities
import org.veriblock.core.params.NetworkParameters
import org.veriblock.core.utilities.createLogger
import org.veriblock.core.wallet.AddressManager
import org.veriblock.sdk.blockchain.store.StoredVeriBlockBlock
import veriblock.model.TransactionPool
import veriblock.net.BootstrapPeerDiscovery
import veriblock.net.LocalhostDiscovery
import veriblock.net.P2PService
import veriblock.net.PeerDiscovery
import veriblock.net.SpvPeerTable
import veriblock.service.BlockStore
import veriblock.service.SpvService
import veriblock.service.Blockchain
import veriblock.service.PendingTransactionContainer
import veriblock.service.TransactionService
import veriblock.wallet.PendingTransactionDownloadedListener
import java.io.File
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
    lateinit var blockStore: BlockStore
        private set
    lateinit var blockchain: Blockchain
        private set
    lateinit var spvService: SpvService
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

    var trustPeerHashes = true
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
            blockStore = BlockStore(networkParameters, directory)
            transactionPool = TransactionPool()
            blockchain = Blockchain(networkParameters.genesisBlock, blockStore)
            pendingTransactionContainer = PendingTransactionContainer()
            p2PService = P2PService(pendingTransactionContainer, networkParameters)
            addressManager = AddressManager()
            val walletFile = File(directory, filePrefix + FILE_EXTENSION)
            addressManager.load(walletFile)
            pendingTransactionDownloadedListener = PendingTransactionDownloadedListener(this)
            peerTable = SpvPeerTable(this, p2PService, peerDiscovery, pendingTransactionContainer)
            transactionService = TransactionService(addressManager, networkParameters)
            spvService = SpvService(
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
    fun init(config: SpvConfig) {
        val networkParameters = NetworkParameters {
            network = config.network
        }

        val peerDiscovery = if (config.useLocalNode) {
            LocalhostDiscovery(networkParameters)
        } else {
            BootstrapPeerDiscovery(networkParameters)
        }

        if (!Context.isCreated()) {
            Context.create(networkParameters)
        } else if (Context.get().networkParameters.name != networkParameters.name) {
            throw IllegalStateException("Attempting to create $networkParameters SPV context while on ${Context.get().networkParameters}")
        }

        val baseDir = File(config.dataDir)
        baseDir.mkdirs()

        trustPeerHashes = config.trustPeerHashes
        init(networkParameters, baseDir, networkParameters.name, peerDiscovery)
    }

    fun shutdown() {
        peerTable.shutdown()
    }
}

class SpvConfig(
    val network: String = "mainnet",
    val dataDir: String = ".",
    val useLocalNode: Boolean = false,
    val trustPeerHashes: Boolean = true
)
