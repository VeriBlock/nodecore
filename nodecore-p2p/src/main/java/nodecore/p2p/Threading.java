// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.p2p;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Threading {
    static final ScheduledExecutorService PEER_TABLE_POOL;
    static final ScheduledExecutorService PEER_WARDEN_THREAD;
    static final ExecutorService PEER_SERVER_THREAD;
    static final ExecutorService PEER_READ_THREAD;
    static final ExecutorService EVENT_BUS_POOL;

    static {
        PEER_TABLE_POOL = Executors.newScheduledThreadPool(3,
                new ThreadFactoryBuilder().setNameFormat("nc-peer-table-%d").build());
        PEER_WARDEN_THREAD = Executors.newSingleThreadScheduledExecutor(
                new ThreadFactoryBuilder().setNameFormat("nc-peer-warden").build());
        PEER_SERVER_THREAD = Executors.newSingleThreadExecutor(
                new ThreadFactoryBuilder().setNameFormat("nc-peer-listener").build());
        PEER_READ_THREAD = Executors.newSingleThreadExecutor(
                new ThreadFactoryBuilder().setNameFormat("nc-peer-read").build());
        EVENT_BUS_POOL = Executors.newFixedThreadPool(50,
                new ThreadFactoryBuilder().setNameFormat("nc-bus-thread-%d").build());
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
