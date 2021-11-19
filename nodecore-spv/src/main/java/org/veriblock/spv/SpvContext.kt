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
import nodecore.p2p.addressKey
import nodecore.p2p.buildMessage
import org.veriblock.core.ConfigurationException
import org.veriblock.core.Context
import org.veriblock.core.launchWithFixedDelay
import org.veriblock.core.params.NetworkParameters
import org.veriblock.core.params.defaultMainNetParameters
import org.veriblock.core.utilities.createLogger
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
import nodecore.p2p.PeerCapabilities

const val FILE_EXTENSION = ".vbkwallet"

private val logger = createLogger {}

/**
 * Initialize and hold beans/classes.
 */
class SpvContext(
    val config: SpvConfig
) {
    val directory: File
    val peerTable: PeerTable
    val wallet: AddressManager
    val blockchain: Blockchain
    val transactionPool: TransactionPool
    val pendingTransactionContainer: PendingTransactionContainer
    val pendingTransactionDownloadedListener: PendingTransactionDownloadedListener
    val transactionService: TransactionService
    val spvService: SpvService

    private val p2pConfiguration: P2pConfiguration
    private val addressState: ConcurrentHashMap<Address, LedgerContext> = ConcurrentHashMap()

    val startTime: Instant = Instant.now()

    init {
        if (config.trustPeerHashes) {
            logger.info { "Fast sync mode is enabled." }
        }

        if (!Context.isCreated()) {
            Context.create(config.networkParameters)
        } else if (Context.get().networkParameters.name != config.networkParameters.name) {
            throw IllegalStateException("Attempting to create $config.networkParameters SPV context while on ${Context.get().networkParameters}")
        }

        val baseDir = File(config.dataDir)
        baseDir.mkdirs()

        try {
            directory = baseDir
            transactionPool = TransactionPool()
            blockchain = Blockchain(config.networkParameters, directory)
            pendingTransactionContainer = PendingTransactionContainer(this)
            wallet = AddressManager()
            val walletFile = File(directory, config.networkParameters.name + FILE_EXTENSION)
            wallet.load(walletFile)
            pendingTransactionDownloadedListener = PendingTransactionDownloadedListener(this)

            val externalPeerEndpoints = config.connectDirectlyTo.map {
                try {
                    // workaround: add p2p:// scheme, because URI must contain (any) scheme
                    val input = if (it.contains("://")) it else "p2p://${it}"

                    val uri = URI.create(input)
                    // if port is not provided, use standard port from networkParameters
                    val port = if (uri.port == -1) config.networkParameters.p2pPort else uri.port
                    NetworkAddress(uri.host, port)
                } catch (e: Exception) {
                    throw ConfigurationException("Wrong format for peer address ${it}, it should be host:port")
                }
            }
            val bootstrappingDnsSeeds = config.networkParameters.bootstrapDns?.let { listOf(it) } ?: emptyList()
            p2pConfiguration = P2pConfiguration(
                networkParameters = config.networkParameters,
                // For now we'll be using either direct discovery or DNS discovery, not a mix.
                // This will change whenever other use cases appear.
                peerBootstrapEnabled = bootstrappingDnsSeeds.isNotEmpty() && externalPeerEndpoints.isEmpty() && config.connectOnlyToDirect,
                bootstrappingDnsSeeds = bootstrappingDnsSeeds,
                externalPeerEndpoints = externalPeerEndpoints,
                connectOnlyToExternal = config.connectOnlyToDirect,
                capabilities = PeerCapabilities.spvCapabilities(),
                neededCapabilities = PeerCapabilities.defaultCapabilities() and config.extraNeededCapabilities,
                fullProgramNameVersion = SpvConstants.FULL_PROGRAM_NAME_VERSION
            )
            if (p2pConfiguration.peerBootstrapEnabled) {
                logger.info { "Using bootstrap discovery" }
            } else {
                logger.info { "Using direct discovery (${p2pConfiguration.externalPeerEndpoints.joinToString { it.addressKey }})" }
            }

            val warden = PeerWarden(p2pConfiguration)
            val bootstrapper = PeerTableBootstrapper(p2pConfiguration, DnsResolver())
            peerTable = PeerTable(p2pConfiguration, warden, bootstrapper)

            transactionService = TransactionService(wallet, config.networkParameters)
            spvService = SpvService(
                this, peerTable, transactionService, wallet,
                pendingTransactionContainer, blockchain
            )

            Runtime.getRuntime().addShutdownHook(Thread({ shutdown() }, "ShutdownHook nodecore-spv"))
        } catch (e: Exception) {
            throw RuntimeException("Could not initialize SPV Context", e)
        }
    }

    fun start() {
        peerTable.initialize(
            onConnected = {
                logger.info { "SPV Connected" }
            },
            onDisconnected = {
                logger.info { "SPV Disconnected" }
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
                peerTable.getConnectedPeers().forEach {
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
    val connectOnlyToDirect: Boolean = true,
    val trustPeerHashes: Boolean = false,
    val extraNeededCapabilities: Set<PeerCapabilities.Capability> = emptySet()
)
