// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package veriblock.util

import com.google.common.util.concurrent.ThreadFactoryBuilder
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

object Threading {
    @JvmField
    var LISTENER_THREAD: ExecutorService? = null
    var NODECORE_POLL_THREAD: ScheduledExecutorService? = null
    @JvmField
    var PEER_OUTPUT_POOL: ExecutorService? = null
    @JvmField
    var PEER_INPUT_POOL: ExecutorService? = null

    @Throws(ExecutionException::class, InterruptedException::class)
    fun shutdown() {
        val shutdownTasks = CompletableFuture.allOf(
            CompletableFuture.runAsync { shutdown(LISTENER_THREAD) },
            CompletableFuture.runAsync { shutdown(NODECORE_POLL_THREAD) },
            CompletableFuture.runAsync { shutdown(PEER_OUTPUT_POOL) },
            CompletableFuture.runAsync { shutdown(PEER_INPUT_POOL) }
        )
        shutdownTasks.get()
    }

    @JvmStatic
    fun shutdown(executorService: ExecutorService?) {
        executorService!!.shutdown()
        try {
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                executorService.shutdownNow()
            }
        } catch (ex: InterruptedException) {
            executorService.shutdownNow()
            Thread.currentThread().interrupt()
        }
    }

    init {
        LISTENER_THREAD = Executors.newSingleThreadExecutor(
            ThreadFactoryBuilder()
                .setNameFormat("event-listener")
                .build()
        )
        NODECORE_POLL_THREAD = Executors.newSingleThreadScheduledExecutor(
            ThreadFactoryBuilder()
                .setNameFormat("nc-poll")
                .build()
        )
        PEER_OUTPUT_POOL = Executors.newFixedThreadPool(
            25,
            ThreadFactoryBuilder()
                .setNameFormat("peer-output-%d")
                .build()
        )
        PEER_INPUT_POOL = Executors.newFixedThreadPool(
            25,
            ThreadFactoryBuilder()
                .setNameFormat("peer-input-%d")
                .build()
        )
    }
}
