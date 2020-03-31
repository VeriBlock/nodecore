package org.veriblock.miners.pop.common

import org.bitcoinj.core.Context
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.core.VersionMessage
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.params.RegTestParams
import org.bitcoinj.params.TestNet3Params

data class NodeCoreRpcConfig(
    val host: String = "127.0.0.1",
    val port: Int = 10500,
    val password: String? = null,
    val ssl: Boolean = false,
    val certificateChainPath: String? = null
)

data class ApiConfig(
    val host: String = "127.0.0.1",
    val port: Int = 10500
)

class BitcoinConfig(
    network: String = "mainnet",
    val maxFee: Long = 8000,
    val feePerKB: Long = 20000,
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
    val peerPingIntervalMillis: Long = 2000L,
    val downloadBlockchainPeriodSeconds: Int = 10,
    val downloadBlockchainBytesPerSecond: Int = 800
) {
    val network = when (network.toLowerCase()) {
        "mainnet" -> BitcoinNetwork.MainNet
        "testnet" -> BitcoinNetwork.TestNet
        "regtest" -> BitcoinNetwork.RegTest
        else -> error("Unable to parse BitCoin network '$network'")
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
