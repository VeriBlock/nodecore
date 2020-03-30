// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.


package org.veriblock.lite.core

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executor

typealias EmptyEventHandler = () -> Unit
typealias EmptyEventListenerPair = Pair<Any, EmptyEventHandler>

class EmptyEvent {

    private val listeners = CopyOnWriteArrayList<EmptyEventListenerPair>()

    fun register(listener: Any, handler: EmptyEventHandler) {
        listeners += listener to handler
    }

    fun remove(listener: Any) {
        listeners.removeIf { it.first == listener }
    }

    fun clear() = listeners.clear()

    fun trigger() {
        for (listener in listeners) {
            listener.second()
        }
    }
}

typealias EventHandler<T> = (T) -> Unit
typealias EventListenerPair<T> = Pair<Any, EventHandler<T>>

class Event<T> {

    private val listeners = CopyOnWriteArrayList<EventListenerPair<T>>()

    fun register(listener: Any, handler: EventHandler<T>) {
        listeners += listener to handler
    }

    fun remove(listener: Any) {
        listeners.removeIf { it.first == listener }
    }

    fun clear() = listeners.clear()

    fun trigger(data: T) {
        for (listener in listeners) {
            listener.second(data)
        }
    }
}

typealias AsyncEventHandler<T> = suspend (T) -> Unit
typealias AsyncEventListenerPair<T> = Pair<Any, AsyncEventHandler<T>>

class AsyncEvent<T>(
    dispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    constructor(executor: Executor) : this(executor.asCoroutineDispatcher())

    private val coroutineScope = CoroutineScope(dispatcher)

    private val listeners = CopyOnWriteArrayList<AsyncEventListenerPair<T>>()

    fun register(listener: Any, handler: AsyncEventHandler<T>) {
        listeners += listener to handler
    }

    fun remove(listener: Any) {
        listeners.removeIf { it.first == listener }
    }

    fun clear() = listeners.clear()

    fun trigger(data: T) {
        for (handler in listeners) {
            coroutineScope.launch {
                handler.second(data)
            }
        }
    }
}
