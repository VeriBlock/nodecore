package veriblock.model

import nodecore.api.grpc.VeriBlockMessages
import java.util.concurrent.atomic.AtomicBoolean

class FutureEventReply {
    private val isDone = AtomicBoolean(false)
    var response: VeriBlockMessages.Event? = null
        private set

    @Synchronized
    fun response(event: VeriBlockMessages.Event?) {
        response = event
        isDone.set(true)
    }

    fun isDone(): Boolean {
        return isDone.get()
    }
}
