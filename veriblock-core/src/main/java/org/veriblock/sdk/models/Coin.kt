// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.sdk.models

import org.veriblock.sdk.util.StreamUtils
import org.veriblock.sdk.util.Utils
import java.nio.ByteBuffer
import kotlin.math.roundToLong

class Coin(
    val atomicUnits: Long
) : Comparable<Coin> {

    @Strictfp
    fun add(addend: Coin): Coin {
        return Coin(atomicUnits + addend.atomicUnits)
    }

    @Strictfp
    fun subtract(subtrahend: Coin): Coin {
        return Coin(atomicUnits - subtrahend.atomicUnits)
    }

    @Strictfp
    fun negate(): Coin {
        return ZERO.subtract(this)
    }

    @Strictfp
    fun toDecimal(): Double {
        return atomicUnits.toDouble() / COIN_VALUE.toDouble()
    }

    override fun hashCode(): Int {
        return atomicUnits.toInt()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return if (other == null || other !is Coin) false else atomicUnits == other.atomicUnits
    }

    override fun toString(): String {
        return atomicUnits.toString()
    }

    override fun compareTo(other: Coin): Int {
        return atomicUnits.compareTo(other.atomicUnits)
    }

    companion object {
        const val COIN_VALUE: Long = 100000000L
        val ZERO = 0.asCoin()
        val ONE = COIN_VALUE.asCoin()

        @JvmStatic
        fun parse(txBuffer: ByteBuffer?): Coin {
            val atomicUnits = Utils.toLong(StreamUtils.getSingleByteLengthValue(txBuffer, 0, 8))
            return atomicUnits.asCoin()
        }
    }
}

operator fun Coin.plus(other: Coin): Coin = add(other)
operator fun Coin.minus(other: Coin): Coin = subtract(other)
operator fun Coin.unaryMinus(): Coin = negate()

fun Coin.abs(): Coin = if (this > Coin.ZERO) {
    this
} else {
    negate()
}

fun Coin.formatted(): String = toString()
fun Int.asCoin(): Coin = Coin(this.toLong())
fun Long.asCoin(): Coin = Coin(this)

fun Int.asCoinDecimal(): Coin = Coin(this * Coin.COIN_VALUE)
fun Long.asCoinDecimal(): Coin = Coin(this * Coin.COIN_VALUE)
fun Double.asCoinDecimal(): Coin = Coin((this * Coin.COIN_VALUE).roundToLong())
