// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.sdk.models

import org.veriblock.core.bitcoinj.BitcoinUtilities
import org.veriblock.core.crypto.*
import org.veriblock.core.utilities.BlockUtility
import org.veriblock.sdk.services.serializeHeaders
import java.math.BigInteger

open class VeriBlockBlock(
    height: Int,
    version: Short,
    previousBlock: PreviousBlockVbkHash,
    previousKeystone: PreviousKeystoneVbkHash,
    secondPreviousKeystone: PreviousKeystoneVbkHash,
    merkleRoot: Sha256Hash,
    timestamp: Int,
    difficulty: Int,
    nonce: Long,
    private val precomputedHash: VbkHash? = null
) {
    val height: Int
    val version: Short
    val previousBlock: PreviousBlockVbkHash
    val previousKeystone: PreviousKeystoneVbkHash
    val secondPreviousKeystone: PreviousKeystoneVbkHash
    val merkleRoot: Sha256Hash
    val timestamp: Int
    val difficulty: Int
    var nonce: Long
    val isProgPow: Boolean

    @Deprecated("See VeriBlockBlock.serializeRaw()", ReplaceWith("serializeRaw"))
    val raw: ByteArray
        get() = this.serializeHeaders()

    // TODO: avoid using BlockUtility.hashBlock
    val hash: VbkHash by lazy {
        precomputedHash ?: if (height == 0) {
            BlockUtility.hashVBlakeBlock(raw)
        } else {
            BlockUtility.hashBlock(raw)
        }.asVbkHash()
    }

    init {
        require(merkleRoot.length >= Sha256Hash.VERIBLOCK_MERKLE_ROOT_LENGTH) {
            "Invalid merkle root: $merkleRoot"
        }
        this.height = height
        this.version = version
        this.previousBlock = previousBlock
        this.previousKeystone = previousKeystone
        this.secondPreviousKeystone = secondPreviousKeystone
        this.merkleRoot = merkleRoot.trim(Sha256Hash.VERIBLOCK_MERKLE_ROOT_LENGTH)
        this.timestamp = timestamp
        this.difficulty = difficulty
        this.nonce = nonce
        // TODO: avoid using BlockUtility.isProgPow
        isProgPow = if (height == 0) false else BlockUtility.isProgPow(height)
    }

    fun getRoundIndex(): Int =
        height % Constants.KEYSTONE_INTERVAL

    fun isKeystone(): Boolean =
        height % Constants.KEYSTONE_INTERVAL == 0

    fun getEffectivePreviousKeystone(): AnyVbkHash =
        if (height % Constants.KEYSTONE_INTERVAL == 1) previousBlock else previousKeystone

    fun getDecodedDifficulty(): BigInteger = BitcoinUtilities.decodeCompactBits(this.difficulty.toLong())

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return if (other == null || other !is VeriBlockBlock) false else {
            height == other.height &&
                version == other.version &&
                previousBlock == other.previousBlock &&
                previousKeystone == other.previousKeystone &&
                secondPreviousKeystone == other.secondPreviousKeystone &&
                merkleRoot == other.merkleRoot &&
                timestamp == other.timestamp &&
                difficulty == other.difficulty &&
                nonce == other.nonce
        }
    }

    override fun hashCode(): Int {
        var result = height.hashCode()
        result = 31 * result + version.hashCode()
        result = 31 * result + previousBlock.hashCode()
        result = 31 * result + previousKeystone.hashCode()
        result = 31 * result + secondPreviousKeystone.hashCode()
        result = 31 * result + merkleRoot.hashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + difficulty.hashCode()
        result = 31 * result + nonce.hashCode()
        return result
    }

    override fun toString(): String {
        return "VeriBlockBlock($hash @ $height)"
    }
}
