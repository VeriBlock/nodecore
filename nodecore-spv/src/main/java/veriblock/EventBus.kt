package veriblock

import com.google.common.util.concurrent.ThreadFactoryBuilder
import nodecore.api.grpc.VeriBlockMessages
import org.veriblock.core.utilities.AsyncEvent
import org.veriblock.core.utilities.EmptyEvent
import org.veriblock.core.utilities.Event
import veriblock.model.StandardTransaction
import veriblock.model.TransactionMeta
import veriblock.net.Peer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

object EventBus {
    private val executor: ExecutorService = Executors.newSingleThreadExecutor(
        ThreadFactoryBuilder()
            .setNameFormat("event-listener")
            .build()
    )

    val peerConnectedEvent = AsyncEvent<Peer>("Peer Connected", executor)
    val peerDisconnectedEvent = AsyncEvent<Peer>("Peer Disconnected", executor)
    val messageReceivedEvent = AsyncEvent<MessageReceivedEvent>("Message Received", executor)

    val pendingTransactionDownloadedEvent = AsyncEvent<StandardTransaction>("Pending Transaction Downloaded", executor)

    val transactionStateChangedEvent = AsyncEvent<TransactionMeta>("Transaction State Changed", executor)
    val transactionDepthChangedEvent = AsyncEvent<TransactionMeta>("Transaction Depth Changed", executor)
}

data class MessageReceivedEvent(
    val peer: Peer,
    val message: VeriBlockMessages.Event
)
