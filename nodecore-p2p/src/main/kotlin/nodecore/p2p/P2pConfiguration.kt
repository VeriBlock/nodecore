package nodecore.p2p

import io.ktor.util.network.NetworkAddress
import org.veriblock.core.params.NetworkParameters
import org.veriblock.core.params.defaultMainNetParameters

class P2pConfiguration(
    val networkParameters: NetworkParameters = defaultMainNetParameters,
    val peerBindAddress: String = "0.0.0.0",
    val peerBindPort: Int = 6500,
    val peerMinCount: Int = 8,
    val peerMaxCount: Int = 15,
    val peerBanThreshold: Int = 100,
    val peerBootstrapEnabled: Boolean = false,
    val bootstrapLimit: Int = 4,
    val bootstrapPeers: List<String> = emptyList(),
    val bootstrappingDnsSeeds: List<String> = emptyList(),
    val externalPeerEndpoints: List<NetworkAddress> = emptyList(),
    val useAdditionalPeers: Boolean = true,
    val peerSharePlatform: Boolean = false,
    val peerPublishAddress: String = "",
    val peerShareMyAddress: Boolean = false,
    val capabilities: PeerCapabilities = PeerCapabilities.defaultCapabilities(),
    val capabilitiesMapper: PeerCapabilities.(Peer) -> PeerCapabilities = { this },
    val neededCapabilities: PeerCapabilities = PeerCapabilities.defaultCapabilities(),
    val fullProgramNameVersion: String = P2pConstants.FULL_PROGRAM_NAME_VERSION
)
