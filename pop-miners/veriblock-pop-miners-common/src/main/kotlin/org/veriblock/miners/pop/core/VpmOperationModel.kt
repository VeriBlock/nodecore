// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.core

import org.bitcoinj.core.Block
import org.bitcoinj.core.Transaction
import org.veriblock.core.contracts.WithDetailedInfo
import org.veriblock.miners.pop.common.formatBTCFriendlyString

class VpmSpTransaction(
    val transaction: Transaction,
    val transactionBytes: ByteArray
) : SpTransaction {
    override val txId: String get() = transaction.txId.toString()
    override val fee: Long get() = transaction.fee?.value ?: 0L
}

class VpmSpBlock(
    val block: Block
) : SpBlock {
    override val hash: String get() = block.hashAsString
}

class VpmMerklePath(
    override val compactFormat: String
) : MerklePath

class VpmContext(
    val blocks: List<Block> = emptyList()
) : WithDetailedInfo {
    override val detailedInfo: Map<String, String>
        get() = mapOf(
            "btcContextBlocks" to blocks.joinToString { it.hashAsString }
        )
}
