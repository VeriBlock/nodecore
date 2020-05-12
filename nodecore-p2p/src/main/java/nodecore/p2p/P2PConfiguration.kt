package nodecore.p2p

import org.veriblock.core.params.NetworkParameters
import org.veriblock.core.params.defaultMainNetParameters

class P2PConfiguration(
    val networkParameters: NetworkParameters = defaultMainNetParameters,
    val peerBindAddress: String = "0.0.0.0",
    val peerBindPort: Int = 6500,
    val peerMinCount: Int = 8,
    val peerMaxCount: Int = 20,
    val peerBanThreshold: Int = 100,
    val peerBootstrapEnabled: Boolean = false,
    val bootstrapLimit: Int = 4,
    val bootstrapPeers: List<String> = emptyList(),
    val bootstrappingDnsSeeds: List<String> = emptyList(),
    val externalPeerEndpoints: List<PeerEndpoint> = emptyList(),
    val peerSharePlatform: Boolean = false,
    val peerPublishAddress: String = "",
    val peerShareMyAddress: Boolean = false
)
