package org.veriblock.sdk.util

import org.veriblock.core.utilities.Utility
import java.nio.ByteBuffer

fun ByteBuffer.readBEInt16(): Short {
    return short
}

fun ByteBuffer.readBEInt32(): Int {
    return int
}

fun ByteBuffer.readLEInt32(): Int {
    return Integer.reverseBytes(int)
}

fun ByteBuffer.putBEInt16(value: Short) {
    putShort(value)
}

fun ByteBuffer.putBEInt32(value: Int) {
    putInt(value)
}

fun ByteBuffer.putBEBytes(value: ByteArray?) {
    put(value)
}

fun ByteBuffer.putLEBytes(value: ByteArray?) {
    put(Utility.flip(value))
}

fun ByteBuffer.putLEInt32(value: Int) {
    putInt(Integer.reverseBytes(value))
}
