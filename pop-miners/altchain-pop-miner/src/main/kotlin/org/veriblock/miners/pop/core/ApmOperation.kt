// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.core

import org.veriblock.core.contracts.WithDetailedInfo
import org.veriblock.core.utilities.createLogger
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
import java.util.UUID

private val logger = createLogger {}

class ApmOperation(
    id: String = UUID.randomUUID().toString().substring(0, 8),
    val chainId: String,
    val chain: SecurityInheritingChain,
    val chainMonitor: SecurityInheritingMonitor,
    changeHistory: List<StateChangeEvent> = emptyList(),
    endorsedBlockHeight: Int? = null,
    reconstituting: Boolean = false
) : MiningOperation<ApmInstruction, ApmSpTransaction, ApmSpBlock, ApmMerklePath, ApmContext>(id, endorsedBlockHeight, changeHistory, reconstituting) {

    val stateChangedEvent = AsyncEvent<ApmOperation>(Threading.MINER_THREAD)

    override fun onStateChanged() {
        stateChangedEvent.trigger(this)
    }
}

class ApmSpTransaction(
    val transaction: WalletTransaction
) : SpTransaction {
    override val txId: String get() = transaction.id.bytes.toHex()
    override val fee: String get() = "Unknown" // TODO
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
