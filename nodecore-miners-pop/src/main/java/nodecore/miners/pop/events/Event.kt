// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.


package nodecore.miners.pop.events

import org.veriblock.core.utilities.createLogger
import java.util.concurrent.CopyOnWriteArrayList

private val logger = createLogger {}

typealias EmptyEventHandler = () -> Unit
typealias EmptyEventListenerPair = Pair<Any, EmptyEventHandler>

class EmptyEvent(
    private val name: String
) {
    private val listeners = CopyOnWriteArrayList<EmptyEventListenerPair>()

    fun register(listener: Any, handler: EmptyEventHandler) {
        logger.debug { "$listener registered to event event $name" }
        listeners += listener to handler
    }

    fun remove(listener: Any) {
        logger.debug { "$listener unregistered from event event $name" }
        listeners.removeIf { it.first == listener }
    }

    fun clear() = listeners.clear()

    fun trigger() {
        logger.debug { "Triggered event: $name" }
        for (listener in listeners) {
            listener.second()
        }
    }
}

typealias EventHandler<T> = (T) -> Unit
typealias EventListenerPair<T> = Pair<Any, EventHandler<T>>

class Event<T>(
    private val name: String
) {
    private val listeners = CopyOnWriteArrayList<EventListenerPair<T>>()

    fun register(listener: Any, handler: EventHandler<T>) {
        logger.debug { "$listener registered to event event $name" }
        listeners += listener to handler
    }

    fun unregister(listener: Any) {
        logger.debug { "$listener unregistered from event event $name" }
        listeners.removeIf { it.first == listener }
    }

    fun clear() = listeners.clear()

    fun trigger(data: T) {
        logger.debug { "Triggered event: $name" }
        for (listener in listeners) {
            listener.second(data)
        }
    }
}
