package veriblock.util

import com.google.common.util.concurrent.ThreadFactoryBuilder
import nodecore.api.grpc.VeriBlockMessages
import org.veriblock.core.utilities.AsyncEvent
import org.veriblock.core.utilities.EmptyEvent
import org.veriblock.core.utilities.Event
import veriblock.model.StandardTransaction
import veriblock.model.TransactionMeta
import veriblock.net.Peer
import veriblock.util.Threading.EVENT_EXECUTOR
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

object EventBus {

    val peerConnectedEvent = AsyncEvent<Peer>("Peer Connected", EVENT_EXECUTOR)
    val peerDisconnectedEvent = AsyncEvent<Peer>("Peer Disconnected", EVENT_EXECUTOR)
    val messageReceivedEvent = AsyncEvent<MessageReceivedEvent>("Message Received", EVENT_EXECUTOR)

    val pendingTransactionDownloadedEvent = AsyncEvent<StandardTransaction>("Pending Transaction Downloaded", EVENT_EXECUTOR)

    val transactionStateChangedEvent = AsyncEvent<TransactionMeta>("Transaction State Changed", EVENT_EXECUTOR)
    val transactionDepthChangedEvent = AsyncEvent<TransactionMeta>("Transaction Depth Changed", EVENT_EXECUTOR)
}

data class MessageReceivedEvent(
    val peer: Peer,
    val message: VeriBlockMessages.Event
)
