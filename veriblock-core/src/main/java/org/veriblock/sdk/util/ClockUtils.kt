package org.veriblock.sdk.util

import com.lyft.kronos.Clock
import com.lyft.kronos.ClockFactory
import com.lyft.kronos.SyncResponseCache
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.time.delay
import kotlinx.coroutines.time.withTimeout
import org.veriblock.core.utilities.createLogger
import org.veriblock.sdk.models.Constants
import java.text.SimpleDateFormat
import java.time.Duration
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.system.exitProcess


private val logger = createLogger {}

fun getDate(dateInMilliseconds: Long, dateFormat: String): String? {
    val formatter = SimpleDateFormat(dateFormat);
    return formatter.format(Date(dateInMilliseconds))
}

suspend fun checkSystemClock() = try {
    withTimeout(Duration.ofSeconds(30)) {
        logger.debug { "Checking the system clock..." }
        val kronosClock = ClockFactory.createKronosClock(LocalClock(), LocalSyncResponseCache())
        var ntpCurrentTime = kronosClock.getCurrentNtpTimeMs()
        while (ntpCurrentTime  == null) {
            delay(Duration.ofSeconds(1))
            ntpCurrentTime = kronosClock.getCurrentNtpTimeMs()
        }
        val systemTime = System.currentTimeMillis()
        if (abs(ntpCurrentTime - System.currentTimeMillis()) > TimeUnit.SECONDS.toMillis(Constants.ALLOWED_TIME_DRIFT.toLong())) {
            val format = "dd/MM/yyyy hh:mm:ss"
            val st = getDate(systemTime, format)
            val nt = getDate(ntpCurrentTime, format)
            logger.error { "The system clock is out of sync. System time=${st}, NTP time=${nt}, local time exceeded allowed time drift=${Constants.ALLOWED_TIME_DRIFT}s" }
            exitProcess(1)
        } else {
            logger.debug { "The system clock is synchronized" }
        }
        kronosClock.shutdown()
    }
} catch (e: TimeoutCancellationException) {
    logger.warn { "Unable to connect to NTP service. Skipping system clock check..." }
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
