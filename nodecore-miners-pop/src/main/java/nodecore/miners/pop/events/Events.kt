package nodecore.miners.pop.events

import nodecore.miners.pop.contracts.PoPMinerDependencies
import nodecore.miners.pop.contracts.PoPMiningOperationState
import nodecore.miners.pop.contracts.VeriBlockHeader
import org.bitcoinj.core.Coin
import org.bitcoinj.core.Transaction

class BitcoinServiceNotReadyEvent

class BitcoinServiceReadyEvent

class BlockchainDownloadedEvent

class CoinsReceivedEvent(
    val tx: Transaction?,
    val previousBalance: Coin?,
    val newBalance: Coin?
)

open class ConfigurationChangedEvent

class NodeCoreConfigurationChangedEvent : ConfigurationChangedEvent()

class FilteredBlockAvailableEvent(
    val state: PoPMiningOperationState
)

class FundsAddedEvent

class InsufficientFundsEvent

class NewVeriBlockFoundEvent(
    val block: VeriBlockHeader,
    val previousHead: VeriBlockHeader?
)

class NodeCoreDesynchronizedEvent

class NodeCoreHealthyEvent

class NodeCoreSynchronizedEvent

class NodeCoreUnhealthyEvent

class PoPMinerNotReadyEvent(
    val failedDependency: PoPMinerDependencies
)

class PoPMinerReadyEvent

class PoPMiningOperationCompletedEvent(
    val operationId: String
)

class PoPMiningOperationStateChangedEvent(
    val state: PoPMiningOperationState,
    val messages: List<String>
)

class ProgramQuitEvent(
    var reason: Int
)

class ShellCompletedEvent

class TransactionConfirmedEvent(
    val state: PoPMiningOperationState
)

class WalletSeedAgreementMissingEvent
