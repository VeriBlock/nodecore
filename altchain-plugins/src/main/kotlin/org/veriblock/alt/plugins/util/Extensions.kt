package org.veriblock.alt.plugins.util

import java.nio.ByteBuffer

internal fun ByteBuffer.getBytes(count: Int): ByteArray {
    val result = ByteArray(count)
    get(result)
    return result
}

internal fun String.asEthHexInt() = drop(2).toInt(16)
internal fun String.asEthHexLong(): Long = drop(2).toLong(16)
internal fun Int.asEthHex(): String = "0x${Integer.toHexString(this)}"

internal fun String.toEthHash(): String = if (startsWith("0x", true)) {
    this
} else {
    "0x$this"
}

internal fun String.asEthHash() = if (startsWith("0x", true)) {
    replaceFirst("0x", "", true)
} else {
    this
}
