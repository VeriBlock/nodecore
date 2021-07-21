// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.util

import com.google.common.util.concurrent.ThreadFactoryBuilder
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import org.veriblock.lite.util.Threading

object Threading {
    val LISTENER_THREAD: ExecutorService = Executors.newSingleThreadExecutor(
        ThreadFactoryBuilder()
            .setNameFormat("event-listener")
            .build()
    )
    val NODECORE_POLL_THREAD: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor(
        ThreadFactoryBuilder()
            .setNameFormat("nc-poll")
            .build()
    )
    val SI_POLL_THREAD: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor(
        ThreadFactoryBuilder()
            .setNameFormat("si-poll")
            .build()
    )
    val MINER_THREAD: ExecutorService = Executors.newSingleThreadExecutor(
        ThreadFactoryBuilder()
            .setNameFormat("miner-thread")
            .build()
    )
    val TASK_POOL: ExecutorService = Executors.newFixedThreadPool(
        50,
        ThreadFactoryBuilder()
            .setNameFormat("pop-tasks-%d")
            .build()
    )

    init {
        Runtime.getRuntime().addShutdownHook(Thread {
            shutdown()
        })
    }

    @Throws(ExecutionException::class, InterruptedException::class)
    fun shutdown() {
        val shutdownTasks = CompletableFuture.allOf(
            CompletableFuture.runAsync { shutdown(Threading.LISTENER_THREAD) },
            CompletableFuture.runAsync { shutdown(Threading.NODECORE_POLL_THREAD) },
            CompletableFuture.runAsync { shutdown(Threading.MINER_THREAD) },
            CompletableFuture.runAsync { shutdown(Threading.TASK_POOL) }
        )
        shutdownTasks.get(20, TimeUnit.SECONDS)
    }

    private fun shutdown(executorService: ExecutorService) {
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
