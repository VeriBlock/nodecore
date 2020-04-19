// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.core

import org.veriblock.core.contracts.WithDetailedInfo
import org.veriblock.core.utilities.extensions.toHex
import org.veriblock.lite.core.AsyncEvent
import org.veriblock.lite.transactionmonitor.WalletTransaction
import org.veriblock.lite.util.Threading
import org.veriblock.miners.pop.securityinheriting.SecurityInheritingMonitor
import org.veriblock.sdk.alt.ApmInstruction
import org.veriblock.sdk.alt.SecurityInheritingChain
import org.veriblock.sdk.models.VeriBlockBlock
import org.veriblock.sdk.models.VeriBlockMerklePath
import org.veriblock.sdk.models.VeriBlockPublication
import java.time.LocalDateTime
import java.util.UUID

class ApmOperation(
    id: String? = null,
    val chain: SecurityInheritingChain,
    val chainMonitor: SecurityInheritingMonitor,
    endorsedBlockHeight: Int? = null,
    createdAt: LocalDateTime = LocalDateTime.now(),
    logs: List<OperationLog> = emptyList(),
    reconstituting: Boolean = false
) : MiningOperation<ApmInstruction, ApmSpTransaction, ApmSpBlock, ApmMerklePath, ApmContext>(
    id ?: chain.key + UUID.randomUUID().toString().substring(0, 8), endorsedBlockHeight, createdAt, logs, reconstituting
) {
    val stateChangedEvent = AsyncEvent<ApmOperation>(Threading.MINER_THREAD)

    override fun onStateChanged() {
        stateChangedEvent.trigger(this)
    }
}

class ApmSpTransaction(
    val transaction: WalletTransaction
) : SpTransaction {
    override val txId: String get() = transaction.id.bytes.toHex()
    override val fee: Long get() = 0L // TODO
}

class ApmSpBlock(
    val block: VeriBlockBlock
) : SpBlock {
    override val hash: String get() = block.hash.bytes.toHex()
}

class ApmMerklePath(
    val merklePath: VeriBlockMerklePath
) : MerklePath {
    override val compactFormat: String get() = merklePath.toCompactString()
}

class ApmContext(
    val publications: List<VeriBlockPublication>
) : WithDetailedInfo {
    override val detailedInfo: Map<String, String>
        get() = mapOf(
            "vtbTransactions" to publications.joinToString { it.transaction.id.bytes.toHex() },
            "vtbBtcBlocks" to publications.joinToString { it.firstBitcoinBlock.hash.bytes.toHex() }
        )
}
