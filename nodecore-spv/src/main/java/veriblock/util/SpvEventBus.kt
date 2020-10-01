package veriblock.util

import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import nodecore.api.grpc.VeriBlockMessages
import org.veriblock.core.utilities.AsyncEvent
import org.veriblock.sdk.models.VeriBlockBlock
import veriblock.model.StandardTransaction
import veriblock.model.TransactionMeta
import veriblock.net.SpvPeer
import veriblock.util.Threading.EVENT_EXECUTOR

object SpvEventBus {

    val peerConnectedEvent = AsyncEvent<SpvPeer>("Peer Connected", EVENT_EXECUTOR)
    val peerDisconnectedEvent = AsyncEvent<SpvPeer>("Peer Disconnected", EVENT_EXECUTOR)
    val messageReceivedEvent = AsyncEvent<MessageReceivedEvent>("Message Received", EVENT_EXECUTOR)

    val pendingTransactionDownloadedEvent = AsyncEvent<StandardTransaction>("Pending Transaction Downloaded", EVENT_EXECUTOR)

    val transactionStateChangedEvent = AsyncEvent<TransactionMeta>("Transaction State Changed", EVENT_EXECUTOR)
    val transactionDepthChangedEvent = AsyncEvent<TransactionMeta>("Transaction Depth Changed", EVENT_EXECUTOR)

    // Block Events
    val newBestBlockEvent = AsyncEvent<VeriBlockBlock>("New Best Block", Threading.LISTENER_THREAD)
    val newBestBlockChannel = BroadcastChannel<VeriBlockBlock>(Channel.CONFLATED)
    //val blockChainReorganizedEvent = AsyncEvent<BlockChainReorganizedEventData>("Blockchain Reorganized", Threading.LISTENER_THREAD)
}

data class MessageReceivedEvent(
    val peer: SpvPeer,
    val message: VeriBlockMessages.Event
)
