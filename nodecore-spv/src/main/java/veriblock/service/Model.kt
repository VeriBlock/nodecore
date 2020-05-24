package veriblock.service

import veriblock.model.AddressLight

enum class BlockchainState {
    LOADING,
    NORMAL,
    PAUSED,
    STALE,
    LOADED
}

enum class OperatingState {
    STARTED,
    INITIALIZING,
    RUNNING,
    TERMINATING
}

enum class NetworkState {
    DISCONNECTED,
    CONNECTED
}

enum class WalletState {
    DEFAULT,
    LOCKED,
    UNLOCKED
}

data class StateInfo(
    val blockchainState: BlockchainState,
    val operatingState: OperatingState,
    val networkState: NetworkState,
    val connectedPeerCount: Int,
    val networkHeight: Int,
    val localBlockchainHeight: Int,
    val networkVersion: String,
    val dataDirectory: String,
    val programVersion: String,
    val nodecoreStartTime: Long,
    val walletCacheSyncHeight: Int,
    val walletState: WalletState
)

data class AddressSignatureIndex(
    val address: AddressLight,
    val poolIndex: Long,
    val blockchainIndex: Long
)
