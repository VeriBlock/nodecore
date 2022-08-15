package org.veriblock.spv.service

import nodecore.p2p.Peer
import org.veriblock.core.crypto.VbkTxId
import org.veriblock.sdk.models.Address
import org.veriblock.sdk.models.Coin
import org.veriblock.sdk.models.Output
import org.veriblock.sdk.models.VeriBlockBlock
import org.veriblock.sdk.models.VeriBlockTransaction
import org.veriblock.spv.model.AddressLight
import org.veriblock.spv.model.StandardTransaction

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
    val unlockedAmount: Coin,
    val lockedAmount: Coin,
    val totalAmount: Coin
)

data class WalletBalance(
    val confirmed: List<AddressBalance>,
    val unconfirmed: List<Output>
)

data class AltChainEndorsement(
    val transaction: VeriBlockTransaction,
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
    val bitcoinConfirmations: Int,
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
    val txId: VbkTxId
)

data class OutputData(
    val address: String,
    val amount: Long
)

data class AddressAvailableBalance(
    val address: Address,
    val availableBalance: Long
)

data class NetworkBlock(
    val block: VeriBlockBlock,
    val source: Peer
)
