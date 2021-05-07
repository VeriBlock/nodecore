// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.spv

import io.ktor.util.network.*
import kotlinx.coroutines.launch
import nodecore.api.grpc.RpcBlockInfo
import nodecore.api.grpc.RpcHeartbeat
import nodecore.api.grpc.utilities.extensions.toByteString
import nodecore.p2p.DnsResolver
import nodecore.p2p.P2pConfiguration
import nodecore.p2p.PeerTable
import nodecore.p2p.PeerTableBootstrapper
import nodecore.p2p.PeerWarden
import nodecore.p2p.buildMessage
import org.veriblock.core.ConfigurationException
import org.veriblock.core.Context
import org.veriblock.core.launchWithFixedDelay
import org.veriblock.core.params.NetworkParameters
import org.veriblock.core.params.defaultMainNetParameters
import org.veriblock.core.utilities.createLogger
import org.veriblock.core.utilities.debugWarn
import org.veriblock.core.wallet.AddressManager
import org.veriblock.sdk.models.Address
import org.veriblock.spv.model.LedgerContext
import org.veriblock.spv.model.LedgerValue
import org.veriblock.spv.model.TransactionPool
import org.veriblock.spv.net.*
import org.veriblock.spv.service.*
import org.veriblock.spv.util.AddressStateChangeEvent
import org.veriblock.spv.util.SpvEventBus.addressStateUpdatedEvent
import org.veriblock.spv.util.Threading
import org.veriblock.spv.wallet.PendingTransactionDownloadedListener
import java.io.File
import java.net.URI
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

const val FILE_EXTENSION = ".vbkwallet"

private val logger = createLogger {}

/**
 * Initialize and hold beans/classes.
 */
class SpvContext(
    config: SpvConfig
) {
    val networkParameters: NetworkParameters = config.networkParameters

    val directory: File
    val filePrefix: String
    val transactionPool: TransactionPool
    val blockStore: BlockStore
    val blockchain: Blockchain
    val spvService: SpvService
    val peerTable: PeerTable
    val addressManager: AddressManager
    val transactionService: TransactionService
    val pendingTransactionContainer: PendingTransactionContainer
    val pendingTransactionDownloadedListener: PendingTransactionDownloadedListener

    private val addressState: ConcurrentHashMap<Address, LedgerContext> = ConcurrentHashMap()

    val trustPeerHashes = config.trustPeerHashes
    val startTime: Instant = Instant.now()

    init {
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

        if (trustPeerHashes) {
            logger.warn {
                "Fast sync mode is enabled." +
                    " With this feature SPV trusts the bootstrap nodes' block hashes to synchronize much faster." +
                    " In order to disable it, locate 'trustPeerHashes' in the configuration file and set it to false."
            }
        }

        if (!Context.isCreated()) {
            Context.create(networkParameters)
        } else if (Context.get().networkParameters.name != networkParameters.name) {
            throw IllegalStateException("Attempting to create $networkParameters SPV context while on ${Context.get().networkParameters}")
        }

        val baseDir = File(config.dataDir)
        baseDir.mkdirs()

        try {
            directory = baseDir
            filePrefix = networkParameters.name
            blockStore = BlockStore(networkParameters, directory)
            transactionPool = TransactionPool()
            blockchain = Blockchain(blockStore)
            pendingTransactionContainer = PendingTransactionContainer()
            addressManager = AddressManager()
            val walletFile = File(directory, filePrefix + FILE_EXTENSION)
            addressManager.load(walletFile)
            pendingTransactionDownloadedListener = PendingTransactionDownloadedListener(this)

            val p2pConfiguration = P2pConfiguration(networkParameters = config.networkParameters, peerBootstrapEnabled = true, bootstrappingDnsSeeds = config.networkParameters.bootstrapDns?.let { listOf(it) } ?: emptyList())
            val warden = PeerWarden(p2pConfiguration)
            val bootstrapper = PeerTableBootstrapper(p2pConfiguration, DnsResolver())
            peerTable = PeerTable(p2pConfiguration, warden, bootstrapper)

            transactionService = TransactionService(addressManager, networkParameters)
            spvService = SpvService(
                this, peerTable, transactionService, addressManager,
                pendingTransactionContainer, blockchain
            )

            Runtime.getRuntime().addShutdownHook(Thread({ shutdown() }, "ShutdownHook nodecore-spv"))
        } catch (e: Exception) {
            logger.debugWarn(e) { "Could not initialize SPV Context" }
            throw RuntimeException(e)
        }
    }

    fun start() {
        peerTable.initialize(
            onConnected = {
                logger.info { "Connected!" }
            },
            onDisconnected = {
                logger.info { "Disconnected!" }
            }
        )
        startPendingTransactionsUpdateTask()
        startAddressStateUpdateTask()

        Threading.PEER_TABLE_SCOPE.launchWithFixedDelay(40_000, 120_000) {
            val lastBlock = blockchain.getChainHeadBlock()
            val heartbeat = buildMessage {
                heartbeat = RpcHeartbeat.newBuilder().apply {
                    block = RpcBlockInfo.newBuilder().apply {
                        number = lastBlock.height
                        hash = lastBlock.hash.bytes.toByteString()
                    }.build()
                }.build()
            }
            launch {
                peerTable.getAvailablePeers().forEach {
                    it.send(heartbeat)
                }
            }
        }

        PeerEventListener(this, peerTable, blockchain, pendingTransactionContainer)
    }

    fun shutdown() {
        peerTable.shutdown()
    }

    fun getAddressState(address: Address): LedgerContext = addressState.getOrPut(address) {
        LedgerContext(
            address = address,
            ledgerValue = LedgerValue(
                availableAtomicUnits = 0L,
                frozenAtomicUnits = 0L,
                signatureIndex = -1L
            ),
            block = blockchain.networkParameters.genesisBlock
        )
    }

    fun getAllAddressesState(): Map<Address, LedgerContext> = addressState
    fun setAddressState(ledgerContext: LedgerContext) {
        val previousLedgerContext = getAddressState(ledgerContext.address)
        addressState[ledgerContext.address] = ledgerContext
        if (ledgerContext.ledgerValue != previousLedgerContext.ledgerValue) {
            addressStateUpdatedEvent.trigger(
                AddressStateChangeEvent(ledgerContext.address, previousLedgerContext.ledgerValue, ledgerContext.ledgerValue)
            )
        }
    }

    fun getSignatureIndex(address: Address): Long? = addressState[address]?.ledgerValue?.signatureIndex
}

class SpvConfig(
    val networkParameters: NetworkParameters = defaultMainNetParameters,
    val dataDir: String = ".",
    val connectDirectlyTo: List<String> = emptyList(),
    val trustPeerHashes: Boolean = false
)
