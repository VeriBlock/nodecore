package veriblock.model

import nodecore.api.grpc.VeriBlockMessages
import java.util.concurrent.atomic.AtomicBoolean

class FutureEventReply {
    private val isDone = AtomicBoolean(false)

    lateinit var response: VeriBlockMessages.Event
        private set

    @Synchronized
    fun response(event: VeriBlockMessages.Event) {
        response = event
        isDone.set(true)
    }

    fun isDone(): Boolean {
        return isDone.get()
    }
}
