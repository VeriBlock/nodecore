// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.sdk.services

import org.veriblock.core.bitcoinj.BitcoinUtilities
import org.veriblock.core.utilities.Utility
import org.veriblock.sdk.models.BitcoinBlock
import org.veriblock.sdk.models.Constants
import org.veriblock.sdk.models.VeriBlockBlock
import org.veriblock.sdk.models.VerificationException
import java.math.BigInteger
import java.util.Locale

object ValidationService {
    fun checkBlock(block: VeriBlockBlock): Boolean {
        return isProofOfWorkValid(block) && isBlockTimeValid(block)
    }

    fun checkBlock(block: BitcoinBlock): Boolean = try {
        verify(block)
        true
    } catch (e: Exception) {
        false
    }

    // VeriBlockBlock
    @Throws(VerificationException::class)
    fun verify(veriBlockBlock: VeriBlockBlock) {
        checkProofOfWork(veriBlockBlock)
        checkMaximumDrift(veriBlockBlock)
    }

    fun getEmbeddedTarget(block: VeriBlockBlock): BigInteger {
        val embeddedDifficulty = BitcoinUtilities.decodeCompactBits(block.difficulty.toLong())
        return Constants.MAXIMUM_DIFFICULTY.divide(embeddedDifficulty)
    }

    fun isProofOfWorkValid(block: VeriBlockBlock): Boolean {
        val hash = block.hash.toBigInteger()
        return hash <= getEmbeddedTarget(block)
    }

    fun isBlockTimeValid(block: VeriBlockBlock): Boolean {
        val currentTime = Utility.getCurrentTimestamp()
        return block.timestamp <= currentTime + Constants.ALLOWED_TIME_DRIFT
    }


    fun checkProofOfWork(block: VeriBlockBlock) {
        if (!isProofOfWorkValid(block)) {
            throw VerificationException(
                String.format(
                    Locale.US, "Block hash is higher than target: %s vs %s",
                    block.hash.toString(),
                    getEmbeddedTarget(block).toString(16)
                )
            )
        }
    }

    fun checkMaximumDrift(veriBlockBlock: VeriBlockBlock) {
        if (!isBlockTimeValid(veriBlockBlock)) {
            throw VerificationException("Block is too far in the future")
        }
    }

    // BitcoinBlock
    @Throws(VerificationException::class)
    fun verify(bitcoinBlock: BitcoinBlock) {
        checkProofOfWork(bitcoinBlock)
        checkMaximumDrift(bitcoinBlock)
    }

    @Throws(VerificationException::class)
    fun isProofOfWorkValid(block: BitcoinBlock): Boolean {
        val embeddedTarget = BitcoinUtilities.decodeCompactBits(block.difficulty.toLong())
        val hash = block.hash.toBigInteger()
        return hash.compareTo(embeddedTarget) <= 0
    }

    @Throws(VerificationException::class)
    fun checkProofOfWork(block: BitcoinBlock) {
        if (!isProofOfWorkValid(block)) {
            val embeddedTarget = BitcoinUtilities.decodeCompactBits(block.difficulty.toLong())
            throw VerificationException(
                String.format(
                    Locale.US, "Block hash is higher than target: %s vs %s",
                    block.hash.toString(),
                    embeddedTarget.toString(16)
                )
            )
        }
    }

    @Throws(VerificationException::class)
    fun checkMaximumDrift(bitcoinBlock: BitcoinBlock) {
        val currentTime = Utility.getCurrentTimestamp()
        if (bitcoinBlock.timestamp > currentTime + Constants.ALLOWED_TIME_DRIFT) {
            throw VerificationException("Block is too far in the future")
        }
    }
}
