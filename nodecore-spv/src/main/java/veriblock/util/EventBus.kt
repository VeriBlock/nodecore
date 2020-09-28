package veriblock.util

import nodecore.api.grpc.VeriBlockMessages
import org.veriblock.core.utilities.AsyncEvent
import veriblock.model.StandardTransaction
import veriblock.model.TransactionMeta
import veriblock.net.SpvPeer
import veriblock.util.Threading.EVENT_EXECUTOR

object EventBus {

    val peerConnectedEvent = AsyncEvent<SpvPeer>("Peer Connected", EVENT_EXECUTOR)
    val peerDisconnectedEvent = AsyncEvent<SpvPeer>("Peer Disconnected", EVENT_EXECUTOR)
    val messageReceivedEvent = AsyncEvent<MessageReceivedEvent>("Message Received", EVENT_EXECUTOR)

    val pendingTransactionDownloadedEvent = AsyncEvent<StandardTransaction>("Pending Transaction Downloaded", EVENT_EXECUTOR)

    val transactionStateChangedEvent = AsyncEvent<TransactionMeta>("Transaction State Changed", EVENT_EXECUTOR)
    val transactionDepthChangedEvent = AsyncEvent<TransactionMeta>("Transaction Depth Changed", EVENT_EXECUTOR)
}

data class MessageReceivedEvent(
    val peer: SpvPeer,
    val message: VeriBlockMessages.Event
)
