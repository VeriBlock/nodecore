package org.veriblock.core

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
