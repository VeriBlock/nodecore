// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package veriblock.model

import java.io.IOException
import java.io.OutputStream

abstract class AddressLight protected constructor(
    private val address: String
) {
    fun get(): String {
        return address
    }

    abstract val type: Byte
    abstract fun toByteArray(): ByteArray

    @Throws(IOException::class)
    fun serializeToStream(stream: OutputStream) {
        val bytes = toByteArray()
        stream.write(type.toInt())
        stream.write(bytes.size)
        stream.write(bytes)
    }

    override fun toString(): String {
        return address
    }

    override fun hashCode(): Int {
        return address.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return address == other
    }
}
