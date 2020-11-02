package org.veriblock.spv.util

import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import nodecore.api.grpc.VeriBlockMessages
import org.veriblock.core.utilities.AsyncEvent
import org.veriblock.sdk.models.VeriBlockBlock
import org.veriblock.spv.model.LedgerContext
import org.veriblock.spv.model.StandardTransaction
import org.veriblock.spv.model.TransactionMeta
import org.veriblock.spv.net.SpvPeer
import org.veriblock.spv.util.Threading.EVENT_EXECUTOR

object SpvEventBus {

    val peerConnectedEvent = AsyncEvent<SpvPeer>("Peer Connected", EVENT_EXECUTOR)
    val peerDisconnectedEvent = AsyncEvent<SpvPeer>("Peer Disconnected", EVENT_EXECUTOR)
    val messageReceivedEvent = AsyncEvent<MessageReceivedEvent>("Message Received", EVENT_EXECUTOR)

    val addressStateUpdatedEvent = AsyncEvent<LedgerContext>("Address State Updated", EVENT_EXECUTOR)

    val pendingTransactionDownloadedEvent = AsyncEvent<StandardTransaction>("Pending Transaction Downloaded", EVENT_EXECUTOR)

    val transactionStateChangedEvent = AsyncEvent<TransactionMeta>("Transaction State Changed", EVENT_EXECUTOR)
    val transactionDepthChangedEvent = AsyncEvent<TransactionMeta>("Transaction Depth Changed", EVENT_EXECUTOR)

    // Block Events
    val newBestBlockEvent = AsyncEvent<VeriBlockBlock>("New Best Block", Threading.LISTENER_THREAD)
    val newBlockChannel = BroadcastChannel<VeriBlockBlock>(Channel.CONFLATED)
    //val blockChainReorganizedEvent = AsyncEvent<BlockChainReorganizedEventData>("Blockchain Reorganized", Threading.LISTENER_THREAD)
}

data class MessageReceivedEvent(
    val peer: SpvPeer,
    val message: VeriBlockMessages.Event
)
