// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.sdk.models

import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.nio.ByteBuffer

class Output(
    val address: Address,
    val amount: Coin
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val obj = other as Output
        return address == obj.address && amount == obj.amount
    }

    override fun hashCode(): Int {
        var result = address.hashCode()
        result = 31 * result + amount.hashCode()
        return result
    }

    companion object {
        @JvmStatic
        fun of(address: String, amount: Long): Output {
            return Output(Address(address), amount.asCoin())
        }

        @JvmStatic
        fun parse(buffer: ByteBuffer): Output {
            val address = Address.parse(buffer)
            val amount = Coin.parse(buffer)
            return Output(address, amount)
        }
    }

    fun serialize(): ByteArray {
        ByteArrayOutputStream().use { stream ->
            serialize(stream)
            return stream.toByteArray()
        }
    }

    fun serialize(stream: OutputStream) {
        address.serialize(stream)
        amount.serialize(stream)
    }
}
