package org.veriblock.core

import com.google.common.util.concurrent.ThreadFactoryBuilder
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

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

fun createSingleThreadExecutor(name: String): ExecutorService = Executors.newSingleThreadExecutor(
    ThreadFactoryBuilder().setNameFormat(name).build()
)

fun createMultiThreadExecutor(name: String, count: Int): ExecutorService = Executors.newFixedThreadPool(
    count,
    ThreadFactoryBuilder().setNameFormat(name).build()
)

