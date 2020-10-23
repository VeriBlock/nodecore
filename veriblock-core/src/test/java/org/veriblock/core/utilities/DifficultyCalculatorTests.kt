package org.veriblock.core.utilities

import org.junit.Assert
import org.junit.Test
import org.veriblock.core.bitcoinj.BitcoinUtilities
import org.veriblock.core.crypto.PreviousBlockVbkHash
import org.veriblock.core.crypto.PreviousKeystoneVbkHash
import org.veriblock.core.crypto.Sha256Hash
import org.veriblock.core.params.defaultMainNetParameters
import org.veriblock.sdk.blockchain.VeriBlockDifficultyCalculator
import org.veriblock.sdk.models.VeriBlockBlock
import java.math.BigInteger
import java.time.Instant
import java.util.*

class VeriBlockDifficultyCalculatorTests {
    val mainnet = defaultMainNetParameters

    fun createBlockMock(time: Int, height: Int, difficulty: BigInteger? = null): VeriBlockBlock {
        val diff = difficulty ?: BigInteger.ONE
        return VeriBlockBlock(
            height = height,
            version = 2,
            previousBlock = PreviousBlockVbkHash(),
            previousKeystone = PreviousKeystoneVbkHash(),
            secondPreviousKeystone = PreviousKeystoneVbkHash(),
            merkleRoot = Sha256Hash.ZERO_HASH,
            timestamp = time,
            difficulty = BitcoinUtilities.encodeCompactBits(diff).toInt(),
            nonce = 0
        )
    }

    @Test
    fun calculateDifficultyWhenBlockLengthIsOne() {
        val blocks: MutableList<VeriBlockBlock> = ArrayList<VeriBlockBlock>()
        blocks.add(
            createBlockMock(
                Instant.now().epochSecond.toInt(), 0, BigInteger("000000FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF", 16)
            )
        )
        val result: BigInteger = VeriBlockDifficultyCalculator.calculate(mainnet, blocks[0], blocks)
        Assert.assertEquals(blocks[0].getDecodedDifficulty(), result)
    }

    @Test
    fun calculateDifficultyWhenBlockIsNormalBeforeRetarget() {
        val startTime = Instant.now().epochSecond.toInt()
        val blocks: MutableList<VeriBlockBlock> = ArrayList<VeriBlockBlock>()
        for (i in 0 until VeriBlockDifficultyCalculator.RETARGET_PERIOD - 1) {
            blocks.add(createBlockMock(startTime + 100, i, BigInteger("000000FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF", 16)))
        }
        blocks.reverse()
        val result: BigInteger = VeriBlockDifficultyCalculator.calculate(
            mainnet,
            blocks[0],
            blocks
        )
        Assert.assertEquals(blocks[0].getDecodedDifficulty(), result)
    }

    @Test
    fun calculateDifficultyWhenBlockIsARetargetBlockAveragingTarget() {
        var time = Instant.now().epochSecond.toInt()
        val blocks: MutableList<VeriBlockBlock> = ArrayList<VeriBlockBlock>()
        for (i in 0 until VeriBlockDifficultyCalculator.RETARGET_PERIOD + 1) {
            time += VeriBlockDifficultyCalculator.TARGET_BLOCKTIME
            blocks.add(createBlockMock(time, i, BigInteger("374138710165940323220619084031490655051373200015360")))
        }
        blocks.reverse()
        val result: BigInteger = VeriBlockDifficultyCalculator.calculate(
            mainnet,
            blocks[0],
            blocks
        )
        Assert.assertEquals(BigInteger("374138710165940323220619084031490655051373200015360"), result)
    }

    @Test
    fun calculateDifficultyWhenBlockIsARetargetBlockDoublingTarget() {
        var time = Instant.now().epochSecond.toInt()
        val blocks: MutableList<VeriBlockBlock> = ArrayList<VeriBlockBlock>()
        for (i in 0 until VeriBlockDifficultyCalculator.RETARGET_PERIOD + 1) {
            time += VeriBlockDifficultyCalculator.TARGET_BLOCKTIME * 2
            blocks.add(createBlockMock(time, i, BigInteger("374138710165940323220619084031490655051373200015360")))
        }
        blocks.reverse()
        val result: BigInteger = VeriBlockDifficultyCalculator.calculate(
            mainnet,
            blocks[0],
            blocks
        )
        Assert.assertEquals(BigInteger("187069355082970161610309542015745327525686600007680"), result)
    }

    @Test
    fun calculateDifficultyWhenBlockIsARetargetBlockHalvingTarget() {
        var time = Instant.now().epochSecond.toInt()
        val blocks: MutableList<VeriBlockBlock> = ArrayList<VeriBlockBlock>()
        for (i in 1000 until 1000 + VeriBlockDifficultyCalculator.RETARGET_PERIOD + 1) {
            time += VeriBlockDifficultyCalculator.TARGET_BLOCKTIME / 2
            blocks.add(createBlockMock(time, i, BigInteger("374138710165940323220619084031490655051373200015360")))
        }
        blocks.reverse()
        val result: BigInteger = VeriBlockDifficultyCalculator.calculate(
            mainnet,
            blocks[0],
            blocks
        )
        Assert.assertEquals(BigInteger("748277420331880646441238168062981310102746400030720"), result)
    }

    @Test
    fun calculateDifficultyWhenBlockIsARetargetBlockAndTimesAreZero() {
        val time = Instant.now().epochSecond.toInt()
        val blocks: MutableList<VeriBlockBlock> = ArrayList<VeriBlockBlock>()
        for (i in 0 until VeriBlockDifficultyCalculator.RETARGET_PERIOD + 1) {
            blocks.add(createBlockMock(time, i, BigInteger("374138710165940323220619084031490655051373200015360")))
        }
        blocks.reverse()
        val result: BigInteger = VeriBlockDifficultyCalculator.calculate(
            mainnet,
            blocks[0],
            blocks
        )
        Assert.assertEquals(BigInteger("3741387101659403232206190840314906550513732000153600"), result)
    }

    @Test
    fun calculateDifficultyWhenBlockIsARetargetBlockAndTimesAreAPercentage() {
        var time = Instant.now().epochSecond.toInt()
        val blocks: MutableList<VeriBlockBlock> = ArrayList<VeriBlockBlock>()
        for (i in 0 until VeriBlockDifficultyCalculator.RETARGET_PERIOD + 1) {
            time += VeriBlockDifficultyCalculator.TARGET_BLOCKTIME / 3
            blocks.add(createBlockMock(time, i, BigInteger("374138710165940323220619084031490655051373200015360")))
        }
        blocks.reverse()
        val result: BigInteger = VeriBlockDifficultyCalculator.calculate(
            mainnet,
            blocks[0],
            blocks
        )
        Assert.assertEquals(BigInteger("1122416130497820969661857252094471965154119600046080"), result)
    }

    @Test
    fun calculateDifficultyWhenBlockTimeIncreasing() {
        /*
         * Blocks   1-33: 20 second intervals
         * Blocks  34-66: 30 second intervals
         * Blocks 67-100: 40 second intervals
         * Averages to roughly a 30 second block time but because more recent are weighted higher,
         * the difficulty will adjust downward.
         */
        var time = Instant.now().epochSecond.toInt()
        val blocks: MutableList<VeriBlockBlock> = ArrayList<VeriBlockBlock>()
        var interval: Int = VeriBlockDifficultyCalculator.TARGET_BLOCKTIME - 10
        for (i in 0 until VeriBlockDifficultyCalculator.RETARGET_PERIOD) {
            if (i > 0 && i % 40 == 0) {
                interval += 10
            }
            time += interval
            blocks.add(createBlockMock(time, i + 1, BigInteger("10000000000000")))
        }
        blocks.reverse()
        val result: BigInteger = VeriBlockDifficultyCalculator.calculate(
            mainnet,
            blocks[0],
            blocks
        )
        Assert.assertEquals(BigInteger("9363170766320"), result)
    }

    @Test
    fun calculateDifficultyWhenBlockTimeDecreasing() {
        /*
         * Blocks   1-40: 40 second intervals
         * Blocks  41-80: 30 second intervals
         * Blocks 81-120: 20 second intervals
         * Averages to a 30 second block time but because more recent are weighted higher,
         * the difficulty will adjust upward.
         */
        var time = Instant.now().epochSecond.toInt()
        val blocks: MutableList<VeriBlockBlock> = ArrayList<VeriBlockBlock>()
        var interval: Int = VeriBlockDifficultyCalculator.TARGET_BLOCKTIME + 10
        for (i in 0 until VeriBlockDifficultyCalculator.RETARGET_PERIOD) {
            if (i > 0 && i % 40 == 0) {
                interval -= 10
            }
            time += interval
            blocks.add(createBlockMock(time, i + 1, BigInteger("10000000000000")))
        }
        blocks.reverse()
        val result: BigInteger = VeriBlockDifficultyCalculator.calculate(
            mainnet,
            blocks[0],
            blocks
        )
        Assert.assertEquals(BigInteger("10729760739729"), result)
    }

    @Test
    fun calculateDifficultyWhenBlockTimesNegative() {
        var time = Instant.now().epochSecond.toInt()
        val blocks: MutableList<VeriBlockBlock> = ArrayList<VeriBlockBlock>()
        for (i in 0 until VeriBlockDifficultyCalculator.RETARGET_PERIOD + 1) {
            time -= 10
            blocks.add(createBlockMock(time, i, BigInteger("374138710165940323220619084031490655051373200015360")))
        }
        blocks.reverse()
        val result: BigInteger = VeriBlockDifficultyCalculator.calculate(
            mainnet,
            blocks[0],
            blocks
        )
        Assert.assertEquals(BigInteger("3741387101659403232206190840314906550513732000153600"), result)
    }

    @Test
    fun calculateDifficultyWhenBlockTimesAboveUpperBound() {
        /*
         * Blocks    1-60:  30 second intervals
         * Blocks  61-120: 300 second intervals (will cap at 180)
         */
        var time = Instant.now().epochSecond.toInt()
        val blocks: MutableList<VeriBlockBlock> = ArrayList<VeriBlockBlock>()
        var interval: Int = VeriBlockDifficultyCalculator.TARGET_BLOCKTIME
        for (i in 0 until VeriBlockDifficultyCalculator.RETARGET_PERIOD) {
            if (i >= 60) {
                interval = 300
            }
            time += interval
            blocks.add(createBlockMock(time, i + 1, BigInteger("10000000000000")))
        }
        blocks.reverse()
        val result: BigInteger = VeriBlockDifficultyCalculator.calculate(
            mainnet,
            blocks[0],
            blocks
        )
        Assert.assertEquals(BigInteger("2374098916560"), result)
    }
}
