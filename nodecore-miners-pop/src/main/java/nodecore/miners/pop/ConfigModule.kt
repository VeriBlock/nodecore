package nodecore.miners.pop

import nodecore.miners.pop.common.BitcoinNetwork
import org.bitcoinj.core.Context
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.core.VersionMessage
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.params.RegTestParams
import org.bitcoinj.params.TestNet3Params
import org.koin.core.module.Module
import org.koin.dsl.module
import org.veriblock.core.utilities.Configuration
import org.veriblock.core.utilities.bootOption
import org.veriblock.core.utilities.bootOptions
import org.veriblock.core.utilities.createLogger
import java.io.File

private val logger = createLogger {}

fun configModule(args: Array<String>): Module {
    // Load boot options
    val bootOptions = bootOptions(
        listOf(
            bootOption(
                opt = "skipAck",
                desc = "Bypasses acknowledgement of seed words on first run",
                keyMapping = "vpm.skipAck"
            )
        ),
        args
    )

    // Load config with the boot options
    val configuration = Configuration(bootOptions = bootOptions)
    val vpmConfig = configuration.extract("vpm") ?: VpmConfig()

    return module {
        single { configuration }
        single { vpmConfig }
    }
}

fun Configuration.getDatabasePath(): String {
    val dataDir = if (getDataDirectory().isNotBlank()) {
        getDataDirectory() + File.separator
    } else {
        ""
    }
    return dataDir + Constants.DEFAULT_DATA_FILE
}

class VpmConfig(
    val bitcoin: BitcoinConfig = BitcoinConfig(),
    val nodeCoreRpc: NodeCoreRpcConfig = NodeCoreRpcConfig(),
    val autoMine: AutoMineConfig = AutoMineConfig(),
    val httpApiPort: Int = 8080,
    val skipAck: Boolean = false,
    val cronSchedule: String = ""
)

class BitcoinConfig(
    network: String = "mainnet",
    val maxFee: Long = 8000,
    val feePerKb: Long = 20000,
    val enableMinRelayFee: Boolean = true,
    val useLocalhostPeer: Boolean = false,
    // Experimental settings:
    minimalPeerProtocolVersion: String = "BLOOM_FILTER",
    val maxPeerConnections: Int = 12,
    val peerDiscoveryTimeoutMillis: Int = 5000,
    val peerDownloadTxDependencyDepth: Int = Int.MAX_VALUE,
    requiredPeerServices: String = "",
    val minPeerBroadcastConnections: Int = 0,
    val maxPeersToDiscoverCount: Int = 100,
    val peerPingIntervalMillis: Long = 2000L
) {
    val network = when (network.toLowerCase()) {
        "mainnet" -> BitcoinNetwork.MainNet
        "testnet" -> BitcoinNetwork.TestNet
        "regtest" -> BitcoinNetwork.RegTest
        else -> {
            logger.warn { "Unable to parse BitCoin network '$network', defaulting to MainNet" }
            BitcoinNetwork.MainNet
        }
    }
    val networkParameters = when (this.network) {
        BitcoinNetwork.MainNet -> MainNetParams.get()
        BitcoinNetwork.TestNet -> TestNet3Params.get()
        BitcoinNetwork.RegTest -> RegTestParams.get()
    }
    val context = Context(networkParameters)

    // Experimental settings:
    val minimalPeerProtocolVersion: NetworkParameters.ProtocolVersion = when (minimalPeerProtocolVersion) {
        "MINIMUM" -> NetworkParameters.ProtocolVersion.MINIMUM
        "PONG" -> NetworkParameters.ProtocolVersion.PONG
        "BLOOM_FILTER_BIP111" -> NetworkParameters.ProtocolVersion.BLOOM_FILTER_BIP111
        "WITNESS_VERSION" -> NetworkParameters.ProtocolVersion.WITNESS_VERSION
        "CURRENT" -> NetworkParameters.ProtocolVersion.CURRENT
        else -> NetworkParameters.ProtocolVersion.BLOOM_FILTER
    }
    val requiredPeerServices = requiredPeerServices.split(',').let { list ->
        var mask = 0
        for (service in list) {
            when (service) {
                "NODE_NETWORK" -> mask = mask or VersionMessage.NODE_NETWORK
                "NODE_GETUTXOS" -> mask = mask or VersionMessage.NODE_GETUTXOS
                "NODE_BLOOM" -> mask = mask or VersionMessage.NODE_BLOOM
                "NODE_WITNESS" -> mask = mask or VersionMessage.NODE_WITNESS
                "NODE_NETWORK_LIMITED" -> mask = mask or VersionMessage.NODE_NETWORK_LIMITED
                "NODE_BITCOIN_CASH" -> mask = mask or VersionMessage.NODE_BITCOIN_CASH
            }
        }
        mask.toLong()
    }
}

class NodeCoreRpcConfig(
    val host: String = "127.0.0.1",
    val port: Int = 10500,
    val password: String? = null,
    val ssl: Boolean = false,
    val certificateChainPath: String? = null
)

class AutoMineConfig(
    val round1: Boolean = false,
    val round2: Boolean = false,
    val round3: Boolean = false,
    val round4: Boolean = false
)
