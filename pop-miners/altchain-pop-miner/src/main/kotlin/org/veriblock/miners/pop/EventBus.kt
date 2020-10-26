package org.veriblock.miners.pop

import org.veriblock.core.contracts.Balance
import org.veriblock.core.utilities.AsyncEvent
import org.veriblock.core.utilities.EmptyEvent
import org.veriblock.core.utilities.Event
import org.veriblock.miners.pop.util.Threading
import org.veriblock.miners.pop.core.ApmOperation

object EventBus {

    // SPV Events
    val spvReadyEvent = EmptyEvent("SPV is ready")
    val spvNotReadyEvent = EmptyEvent("SPV is not ready")

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

    // Operation Events
    val operationStateChangedEvent = AsyncEvent<ApmOperation>("Operation State Changed", Threading.MINER_THREAD)
    val operationFinishedEvent = AsyncEvent<ApmOperation>("Operation Finished", Threading.MINER_THREAD)

    // Others
    val programQuitEvent = Event<Int>("Program Quit")
    val shellCompletedEvent = EmptyEvent("Shell Completed")
}
