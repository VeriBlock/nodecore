package nodecore.p2p;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Threading {
    public static final ExecutorService PLUGIN_THREAD;
    public static final ScheduledExecutorService BLOCK_THREAD;
    public static final ScheduledExecutorService MEMPOOL_THREAD;
    public static final ScheduledExecutorService MONITOR_THREAD;
    public static final ScheduledExecutorService PEER_HEARTBEAT_THREAD;
    public static final ScheduledExecutorService PEER_TABLE_POOL;
    public static final ScheduledExecutorService PEER_WARDEN_THREAD;
    public static final ScheduledExecutorService UCP_WARDEN_THREAD;
    public static final ScheduledExecutorService TRAFFIC_MANAGER_THREAD;
    public static final ExecutorService PEER_SERVER_THREAD;
    public static final ExecutorService PEER_IO_POOL;
    public static final ExecutorService PEER_READ_THREAD;
    public static final ExecutorService EVENT_BUS_POOL;
    public static final ExecutorService P2P_EVENT_BUS_POOL;
    public static final ExecutorService POOL_WEB_POOL;
    public static final ExecutorService POOL_READ_THREAD;
    public static final ExecutorService WALLET_THREAD;

    static {
        PLUGIN_THREAD = Executors.newSingleThreadExecutor(
            new ThreadFactoryBuilder().setNameFormat("nc-plugin-thread").build());
        BLOCK_THREAD = Executors.newSingleThreadScheduledExecutor(
            new ThreadFactoryBuilder().setNameFormat("nc-block-thread").build());
        MEMPOOL_THREAD = Executors.newSingleThreadScheduledExecutor(
            new ThreadFactoryBuilder().setNameFormat("nc-tx-thread").build());
        MONITOR_THREAD = Executors.newSingleThreadScheduledExecutor(
            new ThreadFactoryBuilder().setNameFormat("nc-monitor").build());
        PEER_HEARTBEAT_THREAD = Executors.newSingleThreadScheduledExecutor(
            new ThreadFactoryBuilder().setNameFormat("nc-peer-heartbeat").build());
        PEER_TABLE_POOL = Executors.newScheduledThreadPool(3,
            new ThreadFactoryBuilder().setNameFormat("nc-peer-table-%d").build());
        PEER_WARDEN_THREAD = Executors.newSingleThreadScheduledExecutor(
            new ThreadFactoryBuilder().setNameFormat("nc-peer-warden").build());
        UCP_WARDEN_THREAD = Executors.newSingleThreadScheduledExecutor(
            new ThreadFactoryBuilder().setNameFormat("nc-ucp-warden").build());
        TRAFFIC_MANAGER_THREAD = Executors.newSingleThreadScheduledExecutor(
            new ThreadFactoryBuilder().setNameFormat("nc-traffic-manager").build());
        PEER_SERVER_THREAD = Executors.newSingleThreadExecutor(
            new ThreadFactoryBuilder().setNameFormat("nc-peer-listener").build());
        PEER_IO_POOL = Executors.newFixedThreadPool(10,
            new ThreadFactoryBuilder().setNameFormat("nc-peer-io-%d").build());
        PEER_READ_THREAD = Executors.newSingleThreadExecutor(
            new ThreadFactoryBuilder().setNameFormat("nc-peer-read").build());
        EVENT_BUS_POOL = Executors.newFixedThreadPool(50,
            new ThreadFactoryBuilder().setNameFormat("nc-bus-thread-%d").build());
        P2P_EVENT_BUS_POOL = Executors.newFixedThreadPool(50,
            new ThreadFactoryBuilder().setNameFormat("nc-p2p-thread-%d").build());
        POOL_WEB_POOL = Executors.newFixedThreadPool(20,
            new ThreadFactoryBuilder().setNameFormat("nc-pool-web-%d").build());
        POOL_READ_THREAD = Executors.newSingleThreadExecutor(
            new ThreadFactoryBuilder().setNameFormat("nc-pool-read").build());
        WALLET_THREAD = Executors.newSingleThreadExecutor(
            new ThreadFactoryBuilder().setNameFormat("nc-wallet").build());
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
