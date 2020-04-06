// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.miners.pop.common

import org.bitcoinj.core.Block
import org.bitcoinj.core.Coin
import org.bitcoinj.utils.MonetaryFormat
import org.quartz.CronScheduleBuilder
import java.math.BigDecimal
import java.util.Arrays
import java.util.UUID

fun generateOperationId(): String {
    val id = UUID.randomUUID()
    return id.toString().substring(0, 8)
}

fun Block.serializeHeader(): ByteArray {
    return Arrays.copyOfRange(bitcoinSerialize(), 0, 80)
}

fun BigDecimal.amountToCoin(): Coin {
    val satoshis: Long = movePointRight(8).toLong()
    return Coin.valueOf(satoshis)
}

fun String.isValidCronExpression(): Boolean {
    return try {
        CronScheduleBuilder.cronSchedule(this)
        true
    } catch (e: RuntimeException) {
        false
    }
}

private val BTC_FORMAT = MonetaryFormat.BTC
    .minDecimals(8)
    .repeatOptionalDecimals(8, 0)
    .postfixCode()

/**
 * Returns the value as a 0.12 type string. More digits after the decimal place will be used
 * if necessary, but two will always be present.
 */
fun Coin.formatBTCFriendlyString(): String {
    return BTC_FORMAT.format(this).toString()
}
