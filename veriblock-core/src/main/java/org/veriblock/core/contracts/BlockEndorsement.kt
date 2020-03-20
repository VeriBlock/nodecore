// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.core.contracts

data class BlockEndorsement(
    val height: Int,
    val hash: BlockEndorsementHash,
    val previousHash: BlockEndorsementHash,
    val previousKeystone: BlockEndorsementHash,
    val secondKeystone: BlockEndorsementHash
) {
    constructor(
        height: Int,
        hash: String,
        previousHash: String,
        previousKeystone: String,
        secondKeystone: String
    ) : this(
        height,
        BlockEndorsementHash(hash.toUpperCase()),
        BlockEndorsementHash(previousHash.toUpperCase()),
        BlockEndorsementHash(previousKeystone.toUpperCase()),
        BlockEndorsementHash(secondKeystone.toUpperCase())
    )

    constructor(
        height: Int,
        hash: ByteArray,
        previousHash: ByteArray,
        previousKeystone: ByteArray,
        secondKeystone: ByteArray
    ) : this(
        height,
        BlockEndorsementHash(hash),
        BlockEndorsementHash(previousHash),
        BlockEndorsementHash(previousKeystone),
        BlockEndorsementHash(secondKeystone)
    )
}
