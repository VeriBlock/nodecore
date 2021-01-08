// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.core.contracts

data class BlockEvidence(
    val height: Int,
    val hash: EgBlockHash,
    val previousHash: EgBlockHash,
    val previousKeystone: EgBlockHash,
    val secondKeystone: EgBlockHash? = null
) {
    constructor(
        height: Int,
        hash: String,
        previousHash: String,
        previousKeystone: String,
        secondKeystone: String? = null
    ) : this(
        height,
        hash.asEgBlockHash(),
        previousHash.asEgBlockHash(),
        previousKeystone.asEgBlockHash(),
        secondKeystone?.asEgBlockHash()
    )

    constructor(
        height: Int,
        hash: ByteArray,
        previousHash: ByteArray,
        previousKeystone: ByteArray,
        secondKeystone: ByteArray? = null
    ) : this(
        height,
        hash.asEgBlockHash(),
        previousHash.asEgBlockHash(),
        previousKeystone.asEgBlockHash(),
        secondKeystone?.asEgBlockHash()
    )
}
