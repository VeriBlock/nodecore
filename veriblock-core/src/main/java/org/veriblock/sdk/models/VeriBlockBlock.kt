// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.sdk.models

import org.veriblock.core.crypto.Sha256Hash
import org.veriblock.core.crypto.VBlakeHash
import org.veriblock.core.utilities.BlockUtility
import org.veriblock.sdk.services.SerializeDeserializeService
import java.util.Arrays

open class VeriBlockBlock(
    height: Int,
    version: Short,
    previousBlock: VBlakeHash,
    previousKeystone: VBlakeHash,
    secondPreviousKeystone: VBlakeHash,
    merkleRoot: Sha256Hash,
    timestamp: Int,
    difficulty: Int,
    nonce: Long
) {
    val height: Int
    val version: Short
    val previousBlock: VBlakeHash
    val previousKeystone: VBlakeHash
    val secondPreviousKeystone: VBlakeHash
    val merkleRoot: Sha256Hash
    val timestamp: Int
    val difficulty: Int
    val nonce: Long

    val raw: ByteArray by lazy {
        SerializeDeserializeService.serializeHeaders(this)
    }

    val hash: VBlakeHash by lazy {
        VBlakeHash.wrap(
            if (height == 0) {
                BlockUtility.hashVBlakeBlock(raw)
            } else {
                BlockUtility.hashBlock(raw)
            }
        )
    }

    init {
        require(previousBlock.length >= VBlakeHash.PREVIOUS_BLOCK_LENGTH) {
            "Invalid previous block: $previousBlock"
        }
        require(previousKeystone.length >= VBlakeHash.PREVIOUS_KEYSTONE_LENGTH) {
            "Invalid previous keystone: $previousKeystone"
        }
        require(secondPreviousKeystone.length >= VBlakeHash.PREVIOUS_KEYSTONE_LENGTH) {
            "Invalid second previous keystone: $secondPreviousKeystone"
        }
        require(merkleRoot.length >= Sha256Hash.VERIBLOCK_MERKLE_ROOT_LENGTH) {
            "Invalid merkle root: $merkleRoot"
        }
        this.height = height
        this.version = version
        this.previousBlock = previousBlock.trimToPreviousBlockSize()
        this.previousKeystone = previousKeystone.trimToPreviousKeystoneSize()
        this.secondPreviousKeystone = secondPreviousKeystone.trimToPreviousKeystoneSize()
        this.merkleRoot = merkleRoot.trim(Sha256Hash.VERIBLOCK_MERKLE_ROOT_LENGTH)
        this.timestamp = timestamp
        this.difficulty = difficulty
        this.nonce = nonce
    }

    fun getRoundIndex(): Int =
        height % Constants.KEYSTONE_INTERVAL

    fun isKeystone(): Boolean =
        height % Constants.KEYSTONE_INTERVAL == 0

    fun getEffectivePreviousKeystone(): VBlakeHash =
        if (height % Constants.KEYSTONE_INTERVAL == 1) previousBlock else previousKeystone

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
