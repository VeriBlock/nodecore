package nodecore.p2p

import com.google.common.util.concurrent.ThreadFactoryBuilder
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object Threading {
    val PEER_TABLE_POOL = Executors.newScheduledThreadPool(
        3,
        ThreadFactoryBuilder().setNameFormat("nc-peer-table-%d").build()
    )
    val PEER_WARDEN_THREAD = Executors.newSingleThreadScheduledExecutor(
        ThreadFactoryBuilder().setNameFormat("nc-peer-warden").build()
    )
    val TRAFFIC_MANAGER_THREAD = Executors.newSingleThreadScheduledExecutor(
        ThreadFactoryBuilder().setNameFormat("nc-traffic-manager").build()
    )
    val PEER_SERVER_THREAD = Executors.newSingleThreadExecutor(
        ThreadFactoryBuilder().setNameFormat("nc-peer-listener").build()
    )
    val PEER_IO_POOL = Executors.newFixedThreadPool(
        10,
        ThreadFactoryBuilder().setNameFormat("nc-peer-io-%d").build()
    )
    val PEER_READ_THREAD = Executors.newSingleThreadExecutor(
        ThreadFactoryBuilder().setNameFormat("nc-peer-read").build()
    )
    val P2P_EVENT_BUS_POOL = Executors.newFixedThreadPool(
        50,
        ThreadFactoryBuilder().setNameFormat("nc-p2p-thread-%d").build()
    )
}

fun ExecutorService.safeShutdown() {
    shutdown()
    try {
        if (!awaitTermination(10, TimeUnit.SECONDS)) {
            shutdownNow()
        }
    } catch (ex: InterruptedException) {
        shutdownNow()
        Thread.currentThread().interrupt()
    }
}
