package veriblock.service

import veriblock.model.AddressLight
import veriblock.model.Output
import veriblock.model.Transaction

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

data class AddressBalance(
    val address: AddressLight,
    val unlockedAmount: Long,
    val lockedAmount: Long,
    val totalAmount: Long
)

data class WalletBalance(
    val confirmed: List<AddressBalance>,
    val unconfirmed: List<Output>
)

data class AltChainEndorsement(
    val transaction: Transaction,
    val signatureIndex: Long

)

data class TransactionInfo(
    val confirmations: Int = 0,
    val transaction: Transaction? = null,
    val blockNumber: Int = 0,
    val timestamp: Int = 0,
    val endorsedBlock: String = "",
    val bitcoinBlockHash: String = "",
    val bitcoinTxId: String = "",
    val bitcoinConfiormations: Int = 0,
    val blockHash: String = "",
    val merklePath: String = ""
)
