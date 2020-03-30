// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.lite.core

import org.veriblock.lite.params.NetworkParameters
import org.veriblock.sdk.models.VeriBlockBlock
import org.veriblock.sdk.util.BitcoinUtils

import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

object VeriBlockDifficultyCalculator {
    const val RETARGET_PERIOD = 100 // In blocks

    private const val T = 30 // 30 seconds
    private const val N = RETARGET_PERIOD
    private val K = BigInteger.valueOf(N.toLong())
        .multiply(BigInteger.valueOf(N.toLong()).add(BigInteger.valueOf(-1)))
        .multiply(BigInteger.valueOf(T.toLong()))
        .divide(BigInteger.valueOf(2))

    @Strictfp
    fun calculate(params: NetworkParameters, lastBlock: VeriBlockBlock, context: List<VeriBlockBlock>): BigInteger {
        var currentContext = context
        if (lastBlock.height < N) {
            return BitcoinUtils.decodeCompactBits(lastBlock.difficulty.toLong())
        }

        var sumTarget = BigDecimal.valueOf(0)
        var t: Long = 0
        var j: Long = 0

        if (currentContext.size > N) {
            currentContext = currentContext.subList(0, N)
        }

        for (i in currentContext.size - 1 downTo 1) {
            var solveTime = currentContext[i - 1].timestamp - currentContext[i].timestamp
            if (solveTime > T * 6) {
                solveTime = T * 6
            } else if (solveTime < -6 * T) {
                solveTime = -6 * T
            }

            j++
            t += solveTime * j
            sumTarget = sumTarget.add(BigDecimal(BitcoinUtils.decodeCompactBits(currentContext[i].difficulty.toLong())))
        }

        sumTarget = sumTarget.divide(BigDecimal.valueOf((N - 1).toLong()), 8, RoundingMode.HALF_UP)

        if (t < K.divide(BigInteger.valueOf(10)).toInt()) {
            t = K.divide(BigInteger.valueOf(10)).toInt().toLong()
        }

        val nextTarget = sumTarget.multiply(BigDecimal(K).divide(BigDecimal.valueOf(t), 8, RoundingMode.HALF_UP)).toBigInteger()

        return if (nextTarget < params.minimumDifficulty) {
            params.minimumDifficulty
        } else nextTarget
    }
}
