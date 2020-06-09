package org.veriblock.miners.pop

import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import org.veriblock.core.utilities.EmptyEvent
import org.veriblock.core.utilities.Event
import org.veriblock.lite.core.AsyncEvent
import org.veriblock.lite.core.Balance
import org.veriblock.lite.core.BlockChainReorganizedEventData
import org.veriblock.lite.core.FullBlock
import org.veriblock.lite.util.Threading
import org.veriblock.miners.pop.core.ApmOperation

object EventBus {

    val nodeCoreHealthyEvent = EmptyEvent("NodeCore Healthy")
    val nodeCoreUnhealthyEvent = EmptyEvent("NodeCore Unhealthy")
    val nodeCoreHealthySyncEvent = EmptyEvent("NodeCore Healthy Sync")
    val nodeCoreUnhealthySyncEvent = EmptyEvent("NodeCore Unhealthy Sync")
    val balanceChangedEvent = Event<Balance>("Balance Changed")

    val newBestBlockEvent = AsyncEvent<FullBlock>(Threading.LISTENER_THREAD)
    val newBestBlockChannel = BroadcastChannel<FullBlock>(Channel.CONFLATED)

    val blockChainReorganizedEvent = AsyncEvent<BlockChainReorganizedEventData>(Threading.LISTENER_THREAD)

    val operationStateChangedEvent = AsyncEvent<ApmOperation>(Threading.MINER_THREAD)
}
