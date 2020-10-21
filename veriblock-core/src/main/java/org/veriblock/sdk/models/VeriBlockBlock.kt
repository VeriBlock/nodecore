// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.sdk.models

import org.veriblock.core.bitcoinj.BitcoinUtilities
import org.veriblock.core.crypto.AnyVbkHash
import org.veriblock.core.crypto.PreviousBlockVbkHash
import org.veriblock.core.crypto.PreviousKeystoneVbkHash
import org.veriblock.core.crypto.Sha256Hash
import org.veriblock.core.crypto.VBlake
import org.veriblock.core.crypto.VbkHash
import org.veriblock.core.crypto.asVbkHash
import org.veriblock.core.utilities.BlockUtility
import org.veriblock.sdk.services.SerializeDeserializeService
import java.math.BigInteger
import java.util.Arrays

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

    val raw: ByteArray
        get() = SerializeDeserializeService.serializeHeaders(this)

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
    }

    fun getRoundIndex(): Int =
        height % Constants.KEYSTONE_INTERVAL

    fun isKeystone(): Boolean =
        height % Constants.KEYSTONE_INTERVAL == 0

    fun getEffectivePreviousKeystone(): AnyVbkHash =
        if (height % Constants.KEYSTONE_INTERVAL == 1) previousBlock else previousKeystone

    fun getDecodedDifficulty(): BigInteger = BitcoinUtilities.decodeCompactBits(this.difficulty.toLong())

    override fun equals(other: Any?): Boolean {
        return this === other || other != null && javaClass == other.javaClass && Arrays.equals(
            SerializeDeserializeService.serialize(this), SerializeDeserializeService
            .serialize(other as VeriBlockBlock)
        )
    }

    override fun toString(): String {
        return "VeriBlockBlock($hash @ $height)"
    }
}
