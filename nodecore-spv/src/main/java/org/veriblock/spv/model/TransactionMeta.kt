// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.spv.model

import io.ktor.util.network.*
import org.veriblock.core.crypto.VbkHash
import org.veriblock.core.crypto.VbkTxId
import org.veriblock.spv.util.SpvEventBus
import java.util.ArrayList
import java.util.HashSet

class TransactionMeta(
    val txId: VbkTxId
) {
    private val appearsInBlock: MutableList<VbkHash> = ArrayList()
    var appearsAtChainHeight = -1
    var depth = 0
    private val seenByPeers: MutableSet<String> = HashSet()
    var state = MetaState.UNKNOWN
        private set
    var appearsInBestChainBlock: VbkHash? = null

    fun getAppearsInBlock(): List<VbkHash> {
        return appearsInBlock
    }

    fun addBlockAppearance(hash: VbkHash) {
        appearsInBlock.add(hash)
    }

    fun incrementDepth() {
        depth++
        informListenersDepthChanged()
    }

    val broadcastPeerCount: Int
        get() = seenByPeers.size

    fun recordBroadcast(peer: String): Boolean {
        val added = seenByPeers.add(peer)
        if (!added) {
            return false
        }
        if (state == MetaState.UNKNOWN) {
            state = MetaState.PENDING
        }
        return true
    }

    private fun informListenersStateChanged() {
        SpvEventBus.transactionStateChangedEvent.trigger(this)
    }

    private fun informListenersDepthChanged() {
        SpvEventBus.transactionDepthChangedEvent.trigger(this)
    }

    fun setState(state: MetaState) {
        if (this.state == state) {
            return
        }
        this.state = state
        if (state == MetaState.PENDING) {
            depth = 0
            appearsAtChainHeight = -1
        }
        informListenersStateChanged()
    }

    enum class MetaState(
        val value: Int
    ) {
        PENDING(1),
        CONFIRMED(2),
        DEAD(3),
        UNKNOWN(0);

        companion object {
            fun forNumber(value: Int): MetaState {
                return when (value) {
                    0 -> UNKNOWN
                    1 -> PENDING
                    2 -> CONFIRMED
                    3 -> DEAD
                    else -> throw IllegalArgumentException("There's no MetaState for value $value")
                }
            }
        }

    }
}
