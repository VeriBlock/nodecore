// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.sdk.models

import org.veriblock.core.bitcoinj.BitcoinUtilities
import org.veriblock.core.crypto.AnyVbkHash
import org.veriblock.core.crypto.PreviousBlockVbkHash
import org.veriblock.core.crypto.PreviousKeystoneVbkHash
import org.veriblock.core.crypto.VbkHash
import org.veriblock.core.crypto.MerkleRoot
import org.veriblock.core.crypto.TruncatedMerkleRoot
import org.veriblock.core.crypto.asVbkHash
import org.veriblock.core.utilities.BlockUtility
import org.veriblock.sdk.services.SerializeDeserializeService
import java.math.BigInteger
import java.util.Arrays

open class VeriBlockBlock(
    val height: Int,
    val version: Short,
    val previousBlock: PreviousBlockVbkHash,
    val previousKeystone: PreviousKeystoneVbkHash,
    val secondPreviousKeystone: PreviousKeystoneVbkHash,
    val merkleRoot: TruncatedMerkleRoot,
    val timestamp: Int,
    val difficulty: Int,
    var nonce: Long,
    private val precomputedHash: VbkHash? = null
) {
    val raw: ByteArray
        get() = SerializeDeserializeService.serializeHeaders(this)

    val hash: VbkHash by lazy {
        precomputedHash ?: BlockUtility.hashBlock(raw).asVbkHash()
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
        return "$hash @ $height"
    }

    override fun hashCode(): Int {
        var result = precomputedHash?.hashCode() ?: 0
        result = 31 * result + height
        result = 31 * result + version
        result = 31 * result + previousBlock.hashCode()
        result = 31 * result + previousKeystone.hashCode()
        result = 31 * result + secondPreviousKeystone.hashCode()
        result = 31 * result + merkleRoot.hashCode()
        result = 31 * result + timestamp
        result = 31 * result + difficulty
        result = 31 * result + nonce.hashCode()
        return result
    }
}
