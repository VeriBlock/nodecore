// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.sdk.util

import org.veriblock.core.utilities.Utility
import java.io.OutputStream
import java.nio.ByteBuffer

fun OutputStream.writeSingleByteLengthValue(value: Int) {
    val trimmed = Utility.trimmedByteArrayFromInteger(value)
    write(trimmed.size)
    write(trimmed)
}

fun OutputStream.writeSingleByteLengthValue(value: Long) {
    val trimmed = Utility.trimmedByteArrayFromLong(value)
    write(trimmed.size)
    write(trimmed)
}

fun OutputStream.writeSingleByteLengthValue(value: ByteArray) {
    write(value.size)
    write(value)
}

fun OutputStream.writeSingleIntLengthValue(value: Int) {
    val valueBytes = Utility.toByteArray(value)
    writeSingleByteLengthValue(valueBytes)
}

fun OutputStream.writeVariableLengthValue(value: ByteArray) {
    val dataSize = Utility.trimmedByteArrayFromInteger(value.size)
    write(dataSize.size)
    write(dataSize)
    write(value)
}

fun ByteBuffer.getSingleByteLengthValue(minLength: Int, maxLength: Int): ByteArray {
    val length = get().toInt()
    length.checkLength(minLength, maxLength)
    val value = ByteArray(length)
    this[value]
    return value
}

fun ByteBuffer.getVariableLengthValue(minLength: Int, maxLength: Int): ByteArray {
    val lengthLength = get()
    lengthLength.toInt().checkLength(0, 4)
    val lengthBytes = ByteArray(4)
    this[lengthBytes, 4 - lengthLength, lengthLength.toInt()]
    val length = ByteBuffer.wrap(lengthBytes).int
    length.checkLength(minLength, maxLength)
    val value = ByteArray(length)
    this[value]
    return value
}

private fun Int.checkLength(min: Int, max: Int) {
    require(this in min..max) {
        "Unexpected length: $this (expected a value between $min and $max)"
    }
}
