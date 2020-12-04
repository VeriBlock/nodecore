package org.veriblock.sdk.util

import com.lyft.kronos.Clock
import com.lyft.kronos.ClockFactory
import com.lyft.kronos.SyncResponseCache
import kotlinx.coroutines.time.delay
import kotlinx.coroutines.time.withTimeout
import org.veriblock.core.utilities.createLogger
import org.veriblock.sdk.models.Constants
import java.time.Duration
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.system.exitProcess

private val logger = createLogger {}

suspend fun checkSystemClock() = withTimeout(Duration.ofSeconds(30)) {
    logger.info { "Checking the system clock..." }
    val kronosClock = ClockFactory.createKronosClock(LocalClock(), LocalSyncResponseCache())
    var ntpCurrentTime = kronosClock.getCurrentNtpTimeMs()
    while (ntpCurrentTime  == null) {
        delay(Duration.ofSeconds(1))
        ntpCurrentTime = kronosClock.getCurrentNtpTimeMs()
    }
    if (abs(ntpCurrentTime - System.currentTimeMillis()) > TimeUnit.SECONDS.toMillis(Constants.ALLOWED_TIME_DRIFT.toLong())) {
        logger.error { "The system clock is out of synchronization, please synchronize it" }
        exitProcess(1)
    } else {
        logger.info { "The system clock is synchronized" }
    }
    kronosClock.shutdown()
}

class LocalClock : Clock {
    override fun getCurrentTimeMs(): Long = System.currentTimeMillis()
    override fun getElapsedTimeMs(): Long = 60001 // Instant NTP sync
}

class LocalSyncResponseCache(
    override var currentOffset: Long = 0,
    override var currentTime: Long = 0,
    override var elapsedTime: Long = 0
) : SyncResponseCache {
    override fun clear() {
        currentOffset = 0
        currentTime = 0
        elapsedTime = 0
    }
}
