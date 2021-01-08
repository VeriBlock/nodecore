// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.core

import org.bitcoinj.core.Block
import org.bitcoinj.core.Transaction

class VpmSpTransaction(
    val transaction: Transaction,
    val transactionBytes: ByteArray
) {
    val txId: String get() = transaction.txId.toString()
    val fee: Long get() = transaction.fee?.value ?: 0L
}

class VpmSpBlock(
    val block: Block
) {
    val hash: String get() = block.hashAsString
}

class VpmMerklePath(
    val compactFormat: String
)

class VpmContext(
    val blocks: List<Block> = emptyList()
)
