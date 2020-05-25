package veriblock.service

import org.veriblock.core.crypto.Sha256Hash
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

enum class TransactionType{
    ZERO_UNUSED,
    STANDARD,
    PROOF_OF_PROOF,
    MULTISIG
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
    val confirmations: Int,
    val transaction: TransactionData,
    val blockNumber: Int,
    val timestamp: Int,
    val endorsedBlockHash: String,
    val bitcoinBlockHash: String,
    val bitcoinTxId: String,
    val bitcoinConfiormations: Int,
    val blockHash: String,
    val merklePath: String
)

data class TransactionData(
    val type: TransactionType,
    val sourceAddress: String,
    val sourceAmount: Long,
    val outputs: List<OutputData>,
    val transactionFee: Long,
    val data: String,
    val bitcoinTransaction: String,
    val endorsedBlockHeader: String,
    val bitcoinBlockHeaderOfProof: String,
    val merklePath: String,
    val contextBitcoinBlockHeaders: List<String>,
    val timestamp: Int,
    val size: Int,
    val txId: Sha256Hash
)

data class OutputData(
    val address: String,
    val amount: Long
)
