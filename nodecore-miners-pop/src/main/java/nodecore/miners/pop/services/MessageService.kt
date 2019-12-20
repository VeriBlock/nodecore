// VeriBlock PoP Miner
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.miners.pop.services

import com.google.common.eventbus.Subscribe
import nodecore.miners.pop.InternalEventBus
import nodecore.miners.pop.contracts.MessageEvent
import org.veriblock.core.utilities.createLogger
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch

private val logger = createLogger {}

class MessageService {
    private val queue: ConcurrentLinkedQueue<MessageEvent> = ConcurrentLinkedQueue()

    private var messageGate: CountDownLatch = CountDownLatch(1)
    private var running: Boolean = true

    init {
        InternalEventBus.getInstance().register(this)
    }

    fun getMessages(): List<MessageEvent> {
        if (!running) {
            return emptyList()
        }
        try {
            messageGate.await()
        } catch (e: InterruptedException) {
            logger.error(e.message, e)
        }
        val messages: MutableList<MessageEvent> = ArrayList()
        var message: MessageEvent? = queue.poll()
        while (message != null) {
            messages.add(message)
            message = queue.poll()
        }
        messageGate = CountDownLatch(1)
        return messages
    }

    fun shutdown() {
        running = false
        messageGate.countDown()
    }

    @Subscribe
    fun onMessage(event: MessageEvent) {
        try {
            queue.add(event)
            messageGate.countDown()
        } catch (e: Exception) {
            logger.error(e.message, e)
        }
    }
}
