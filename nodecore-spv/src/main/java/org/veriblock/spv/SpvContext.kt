// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.spv

import io.ktor.util.network.*
import kotlinx.coroutines.channels.Channel
import org.veriblock.core.ConfigurationException
import org.veriblock.core.Context
import org.veriblock.core.params.NetworkParameters
import org.veriblock.core.utilities.createLogger
import org.veriblock.core.utilities.debugWarn
import org.veriblock.core.wallet.AddressManager
import org.veriblock.sdk.models.Address
import org.veriblock.spv.model.LedgerContext
import org.veriblock.spv.model.LedgerValue
import org.veriblock.spv.model.TransactionPool
import org.veriblock.spv.net.*
import org.veriblock.spv.service.*
import org.veriblock.spv.util.SpvEventBus.addressStateUpdatedEvent
import org.veriblock.spv.wallet.PendingTransactionDownloadedListener
import java.io.File
import java.net.URI
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

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

    private val addressState: ConcurrentHashMap<Address, LedgerContext> = ConcurrentHashMap()
    val addressStateUpdated: Channel<LedgerContext> = Channel()

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
            blockchain = Blockchain(blockStore)
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
            logger.debugWarn(e) { "Could not initialize SPV Context" }
            throw RuntimeException(e)
        }
    }

    /**
     * Initialise context. This method should be call on the start app.
     */
    @Synchronized
    fun init(config: SpvConfig) {
        val networkParameters = NetworkParameters {
            network = config.network
        }

        check(networkParameters.name == config.network) {
            "SPV configured to ${config.network}, and network to ${networkParameters.name}. They must be same."
        }

        // For now we'll be using either direct discovery or DNS discovery, not a mix.
        // This will change whenever other use cases appear.
        val peerDiscovery = if (config.connectDirectlyTo.isNotEmpty()) {
            DirectDiscovery(config.connectDirectlyTo.map {
                try {
                    val input =
                        // workaround: add p2p:// scheme, because URI must contain (any) scheme
                        if (it.contains("://")) it else "p2p://${it}"

                    val uri = URI.create(input)
                    // if port is not provided, use standard port from networkParameters
                    val port = if (uri.port == -1) networkParameters.p2pPort else uri.port
                    NetworkAddress(uri.host, port)
                } catch (e: Exception) {
                    throw ConfigurationException("Wrong format for peer address ${it}, it should be host:port")
                }
            })
        } else {
            networkParameters.bootstrapDns?.let {
                DnsDiscovery(it, networkParameters.p2pPort)
            } ?: error("Unable to create Peer Discovery for $networkParameters, as doesn't have any configured bootstrap DNS.")
        }

        logger.info { "Using ${peerDiscovery.name()} discovery" }

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
        if (::peerTable.isInitialized) {
            peerTable.shutdown()
        }
    }

    fun getAddressState(address: Address): LedgerContext = addressState.getOrDefault(
        key = address,
        defaultValue = LedgerContext(
            address = address,
            ledgerValue = LedgerValue(
                availableAtomicUnits = 0L,
                frozenAtomicUnits = 0L,
                signatureIndex = -1L
            ),
            block = blockchain.networkParameters.genesisBlock
        )
    )

    fun getAllAddressesState(): Map<Address, LedgerContext> = addressState
    fun setAddressState(ledgerContext: LedgerContext) {
        addressState[ledgerContext.address] = ledgerContext
        addressStateUpdatedEvent.trigger(ledgerContext)
    }

    fun getSignatureIndex(address: Address): Long? = addressState[address]?.ledgerValue?.signatureIndex
}

class SpvConfig(
    val network: String = "mainnet",
    val dataDir: String = ".",
    val connectDirectlyTo: List<String> = emptyList(),
    val trustPeerHashes: Boolean = true
)
