// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package veriblock.util

import com.google.common.util.concurrent.ThreadFactoryBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

object Threading {
    val LISTENER_THREAD: ExecutorService = Executors.newSingleThreadExecutor(
        ThreadFactoryBuilder()
            .setNameFormat("event-listener")
            .build()
    )
    val PEER_TABLE_THREAD: ExecutorService = Executors.newSingleThreadExecutor(
        ThreadFactoryBuilder()
            .setNameFormat("peer-table-thread")
            .build()
    )
    val MESSAGE_HANDLER_THREAD: ExecutorService = Executors.newSingleThreadExecutor(
        ThreadFactoryBuilder()
            .setNameFormat("message-handler-thread")
            .build()
    )
    val PEER_OUTPUT_POOL: ExecutorService = Executors.newFixedThreadPool(
        25,
        ThreadFactoryBuilder()
            .setNameFormat("peer-output-%d")
            .build()
    )
    val PEER_INPUT_POOL: ExecutorService = Executors.newFixedThreadPool(
        25,
        ThreadFactoryBuilder()
            .setNameFormat("peer-input-%d")
            .build()
    )
    val EVENT_EXECUTOR: ExecutorService = Executors.newSingleThreadExecutor(
        ThreadFactoryBuilder()
            .setNameFormat("event-listener")
            .build()
    )

    @Throws(ExecutionException::class, InterruptedException::class)
    fun shutdown() {
        val shutdownTasks = CompletableFuture.allOf(
            CompletableFuture.runAsync { shutdown(LISTENER_THREAD) },
            CompletableFuture.runAsync { shutdown(PEER_TABLE_THREAD) },
            CompletableFuture.runAsync { shutdown(MESSAGE_HANDLER_THREAD) },
            CompletableFuture.runAsync { shutdown(PEER_OUTPUT_POOL) },
            CompletableFuture.runAsync { shutdown(PEER_INPUT_POOL) },
            CompletableFuture.runAsync { shutdown(EVENT_EXECUTOR) }
        )
        shutdownTasks.get()
    }

    @JvmStatic
    fun shutdown(executorService: ExecutorService) {
        executorService.shutdown()
        try {
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                executorService.shutdownNow()
            }
        } catch (ex: InterruptedException) {
            executorService.shutdownNow()
            Thread.currentThread().interrupt()
        }
    }
}

inline fun CoroutineScope.launchWithFixedDelay(
    initialDelayMillis: Long = 0,
    periodMillis: Long = 1000L,
    crossinline block: suspend CoroutineScope.() -> Unit
) = launch {
    delay(initialDelayMillis)
    while (isActive) {
        block()
        delay(periodMillis)
    }
}
