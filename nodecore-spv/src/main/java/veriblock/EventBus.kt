package veriblock

import com.google.common.util.concurrent.ThreadFactoryBuilder
import org.veriblock.core.utilities.AsyncEvent
import org.veriblock.core.utilities.EmptyEvent
import org.veriblock.core.utilities.Event
import veriblock.model.StandardTransaction
import java.util.concurrent.Executors

object EventBus {

    private val executor = Executors.newSingleThreadExecutor(
        ThreadFactoryBuilder()
            .setNameFormat("event-listener")
            .build()
    )

    val pendingTransactionDownloadedEvent = AsyncEvent<StandardTransaction>("Pending Transaction Downloaded", executor)
}
