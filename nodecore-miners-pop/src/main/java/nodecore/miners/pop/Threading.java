// VeriBlock PoP Miner
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.bitcoinj.utils.ContextPropagatingThreadFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Threading {
    public static final ExecutorService TASK_POOL;

    static {
        TASK_POOL = Executors.newFixedThreadPool(50,
                new ThreadFactoryBuilder()
                        .setNameFormat("pop-tasks-%d")
                        .setThreadFactory(new ContextPropagatingThreadFactory("pop-tasks"))
                        .build());
    }

    public static void shutdown() {
        TASK_POOL.shutdown();

        try {
            if (!TASK_POOL.awaitTermination(10, TimeUnit.SECONDS)) {
                TASK_POOL.shutdownNow();
            }
        } catch (InterruptedException ex) {
            TASK_POOL.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
