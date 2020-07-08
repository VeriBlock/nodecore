package org.veriblock.miners.pop

import org.bitcoinj.core.Coin
import org.bitcoinj.core.Transaction
import org.veriblock.core.contracts.Balance
import org.veriblock.core.utilities.EmptyEvent
import org.veriblock.core.utilities.Event
import org.veriblock.miners.pop.core.VpmOperation
import org.veriblock.miners.pop.model.VeriBlockHeader

object EventBus {
    // Bitcoin Events
    val bitcoinServiceReadyEvent = EmptyEvent("Bitcoin Service Ready")
    val bitcoinServiceNotReadyEvent = EmptyEvent("Bitcoin Service Not Ready")
    val blockchainDownloadedEvent = EmptyEvent("Bitcoin blockchain downloaded")
    val blockchainNotDownloadedEvent = EmptyEvent("Bitcoin blockchain not downloaded")

    // Balance Events
    val sufficientFundsEvent = Event<Coin>("Sufficient Funds")
    val insufficientFundsEvent = Event<Coin>("Insufficient Funds")
    val balanceChangedEvent = Event<Coin>("Balance changed")

    // Block Events
    val newVeriBlockFoundEvent = Event<NewVeriBlockFoundEventDto>("New VeriBlock Found")

    // NodeCore Events
    val nodeCoreAccessibleEvent = EmptyEvent("NodeCore is accessible")
    val nodeCoreNotAccessibleEvent = EmptyEvent("NodeCore is not accessible")
    val nodeCoreSynchronizedEvent = EmptyEvent("NodeCore is synchronized")
    val nodeCoreNotSynchronizedEvent = EmptyEvent("NodeCore is not synchronized")
    val nodeCoreSameNetworkEvent = EmptyEvent("NodeCore is at the same configured network")
    val nodeCoreNotSameNetworkEvent = EmptyEvent("NodeCore is not at the same configured network")
    val nodeCoreReadyEvent = EmptyEvent("NodeCore is ready")
    val nodeCoreNotReadyEvent = EmptyEvent("NodeCore is not ready")

    // Operation Events
    val popMiningOperationCompletedEvent = Event<String>("PoP Mining Operation Completed")
    val popMiningOperationFinishedEvent = Event<VpmOperation>("PoP Mining Operation Finished")
    val popMiningOperationStateChangedEvent = Event<VpmOperation>("PoP Mining Operation State Changed")

    // Miner Events
    val minerReadyEvent = EmptyEvent("Miner is ready")
    val minerNotReadyEvent = EmptyEvent("Miner is not ready")

    // Others
    val programQuitEvent = Event<Int>("Program Quit")
    val shellCompletedEvent = EmptyEvent("Shell Completed")
    val walletSeedAgreementMissingEvent = EmptyEvent("Wallet Seed Agreement Missing")
    val transactionSufferedReorgEvent = Event<VpmOperation>("Bitcoin transaction suffered a reorg")
}

class CoinsReceivedEventDto(
    val tx: Transaction,
    val previousBalance: Coin,
    val newBalance: Coin
)

class NewVeriBlockFoundEventDto(
    val block: VeriBlockHeader,
    val previousHead: VeriBlockHeader?
)
