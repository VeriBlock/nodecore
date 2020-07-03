package org.veriblock.miners.pop

import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import org.veriblock.core.contracts.Balance
import org.veriblock.core.utilities.AsyncEvent
import org.veriblock.core.utilities.EmptyEvent
import org.veriblock.core.utilities.Event
import org.veriblock.lite.core.BlockChainReorganizedEventData
import org.veriblock.lite.core.FullBlock
import org.veriblock.lite.util.Threading
import org.veriblock.miners.pop.core.ApmOperation

object EventBus {

    // NodeCore Events
    val nodeCoreAccessibleEvent = EmptyEvent("NodeCore is accessible")
    val nodeCoreNotAccessibleEvent = EmptyEvent("NodeCore is not accessible")
    val nodeCoreSynchronizedEvent = EmptyEvent("NodeCore is synchronized")
    val nodeCoreNotSynchronizedEvent = EmptyEvent("NodeCore is not synchronized")
    val nodeCoreSameNetworkEvent = EmptyEvent("NodeCore is at the same configured network")
    val nodeCoreNotSameNetworkEvent = EmptyEvent("NodeCore is not at the same configured network")
    val nodeCoreReadyEvent = EmptyEvent("NodeCore is ready")
    val nodeCoreNotReadyEvent = EmptyEvent("NodeCore is not ready")

    // Altchain Events
    val altChainAccessibleEvent = Event<String>("Altchain is accessible")
    val altChainNotAccessibleEvent = Event<String>("Altchain is not accessible")
    val altChainSynchronizedEvent = Event<String>("Altchain is synchronized")
    val altChainNotSynchronizedEvent = Event<String>("Altchain is not synchronized")
    val altChainSameNetworkEvent = Event<String>("Altchain is at the same configured network")
    val altChainNotSameNetworkEvent = Event<String>("Altchain is not at the same configured network")
    val altChainReadyEvent = Event<String>("Altchain is ready")
    val altChainNotReadyEvent = Event<String>("Altchain is not ready")

    // Balance Events
    val sufficientBalanceEvent = Event<Balance>("Sufficient balance")
    val insufficientBalanceEvent = EmptyEvent("Insufficient balance")
    val balanceChangeEvent = Event<Balance>("Balance changed")

    // Block Events
    val newBestBlockEvent = AsyncEvent<FullBlock>("New Best Block", Threading.LISTENER_THREAD)
    val newBestBlockChannel = BroadcastChannel<FullBlock>(Channel.CONFLATED)
    val blockChainReorganizedEvent = AsyncEvent<BlockChainReorganizedEventData>("Blockchain Reorganized", Threading.LISTENER_THREAD)

    // Operation Events
    val operationStateChangedEvent = AsyncEvent<ApmOperation>("Operation State Changed", Threading.MINER_THREAD)
    val operationFinishedEvent = AsyncEvent<ApmOperation>("Operation Finished", Threading.MINER_THREAD)

    // Others
    val programQuitEvent = Event<Int>("Program Quit")
    val shellCompletedEvent = EmptyEvent("Shell Completed")
}
