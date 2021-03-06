// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.sdk.blockchain

import org.veriblock.core.bitcoinj.BitcoinUtilities
import org.veriblock.core.params.NetworkParameters
import org.veriblock.sdk.models.VeriBlockBlock
import java.lang.IllegalArgumentException
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

object VeriBlockDifficultyCalculator {
    const val PROGPOW_INITIAL_DIFFICULTY = 100_000_000_000L
    const val RETARGET_PERIOD = 100 // In blocks
    const val TARGET_BLOCKTIME = 30 // seconds
    private const val T = TARGET_BLOCKTIME
    private const val N = RETARGET_PERIOD
    private val K = BigInteger.valueOf(N.toLong())
        .multiply(BigInteger.valueOf(N.toLong()).add(BigInteger.valueOf(-1)))
        .multiply(BigInteger.valueOf(T.toLong()))
        .divide(BigInteger.valueOf(2))

    fun calculate(
        networkParameters: NetworkParameters,
        lastBlock: VeriBlockBlock,
        context: List<VeriBlockBlock>
    ): BigInteger {
        if (lastBlock.height < N || networkParameters.powNoRetargeting) {
            return BitcoinUtilities.decodeCompactBits(lastBlock.difficulty.toLong())
        }

        if (lastBlock.height >= networkParameters.progPowForkHeight - 1 &&
            lastBlock.height < networkParameters.progPowForkHeight + N - 1
        ) {
            return BigInteger.valueOf(PROGPOW_INITIAL_DIFFICULTY)
        }

        var sumTarget = BigDecimal.valueOf(0)
        var t: Long = 0
        var j: Long = 0

        val contextToCheck = if (context.size > N) {
            // take last N elements
            context.subList(0, N)
        } else {
            context
        }

        if(lastBlock.height > N && contextToCheck.size != N) {
            throw IllegalArgumentException("Invariant failed! Block height is=${lastBlock.height} but context size is ${contextToCheck.size} != $N")
        }

        (contextToCheck.size - 1 downTo 1).forEach { i ->
            var solveTime = contextToCheck[i - 1].timestamp - contextToCheck[i].timestamp
            if (solveTime > T * 6) {
                solveTime = T * 6
            } else if (solveTime < -6 * T) {
                solveTime = -6 * T
            }
            j++
            t += solveTime * j
            sumTarget = sumTarget.add(BigDecimal(BitcoinUtilities.decodeCompactBits(contextToCheck[i].difficulty.toLong())))
        }
        sumTarget = sumTarget.divide(BigDecimal.valueOf(N - 1.toLong()), 8, RoundingMode.HALF_UP)
        if (t < K.toLong() / 10) {
            t = K.toLong() / 10
        }
        val nextTarget = (
            sumTarget * (K.toBigDecimal().divide(t.toBigDecimal(), 8, RoundingMode.HALF_UP))
            ).toBigInteger()
        return if (nextTarget < networkParameters.minimumDifficulty) {
            if(lastBlock.height < 906439) {
                networkParameters.minimumDifficulty
            } else {
                networkParameters.minimumDifficulty.add(BigInteger.valueOf(500000))
            }
        } else {
            nextTarget
        }
    }
}
