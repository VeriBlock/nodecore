// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package veriblock.util;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Threading {
    public static final ExecutorService LISTENER_THREAD;
    public static final ScheduledExecutorService NODECORE_POLL_THREAD;
    public static final ExecutorService PEER_OUTPUT_POOL;
    public static final ExecutorService PEER_INPUT_POOL;

    static {
        LISTENER_THREAD = Executors.newSingleThreadExecutor(
                new ThreadFactoryBuilder()
                        .setNameFormat("event-listener")
                        .build());
        NODECORE_POLL_THREAD = Executors.newSingleThreadScheduledExecutor(
                new ThreadFactoryBuilder()
                        .setNameFormat("nc-poll")
                        .build());

        PEER_OUTPUT_POOL = Executors.newFixedThreadPool(25,
                new ThreadFactoryBuilder()
                        .setNameFormat("peer-output-%d")
                        .build());

        PEER_INPUT_POOL = Executors.newFixedThreadPool(25,
                new ThreadFactoryBuilder()
                        .setNameFormat("peer-input-%d")
                        .build());
    }

    public static void shutdown() throws ExecutionException, InterruptedException {
        CompletableFuture<Void> shutdownTasks = CompletableFuture.allOf(
                CompletableFuture.runAsync(() -> shutdown(LISTENER_THREAD)),
                CompletableFuture.runAsync(() -> shutdown(NODECORE_POLL_THREAD)),
                CompletableFuture.runAsync(() -> shutdown(PEER_OUTPUT_POOL)),
                CompletableFuture.runAsync(() -> shutdown(PEER_INPUT_POOL)));

        shutdownTasks.get();
    }

    public static void shutdown(ExecutorService executorService) {
        executorService.shutdown();

        try {
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException ex) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
