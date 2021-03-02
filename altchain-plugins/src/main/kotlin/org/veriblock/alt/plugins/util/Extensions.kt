package org.veriblock.alt.plugins.util

import java.nio.ByteBuffer

internal fun ByteBuffer.getBytes(count: Int): ByteArray {
    val result = ByteArray(count)
    get(result)
    return result
}

internal  fun String.asEthHexInt() = drop(2).toInt(16)
internal  fun String.asEthHexLong(): Long = drop(2).toLong(16)
internal  fun Int.asEthHex(): String = "0x${Integer.toHexString(this)}"
