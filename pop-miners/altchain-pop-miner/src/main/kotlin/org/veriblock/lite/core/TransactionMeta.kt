// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.lite.core

import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel.Factory.CONFLATED
import org.veriblock.sdk.models.Sha256Hash
import org.veriblock.sdk.models.VBlakeHash
import java.util.ArrayList

class TransactionMeta(
    val txId: Sha256Hash
) {
    val stateChangedBroadcastChannel = BroadcastChannel<MetaState>(CONFLATED)

    var state = MetaState.UNKNOWN
        private set

    var appearsInBestChainBlock: VBlakeHash? = null

    private val appearsInBlock: MutableList<VBlakeHash> = ArrayList()

    var appearsAtChainHeight = -1

    var depth: Int = 0

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
                    else -> error("Invalid MetaState value: $value")
                }
            }
        }
    }

    fun getAppearsInBlock(): List<VBlakeHash> {
        return appearsInBlock
    }

    fun addBlockAppearance(hash: VBlakeHash) {
        appearsInBlock.add(hash)
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
        stateChangedBroadcastChannel.offer(state)
    }

    fun incrementDepth() {
        depth++
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TransactionMeta

        if (txId != other.txId) return false
        if (state != other.state) return false
        if (appearsInBestChainBlock != other.appearsInBestChainBlock) return false
        if (appearsInBlock != other.appearsInBlock) return false
        if (appearsAtChainHeight != other.appearsAtChainHeight) return false
        if (depth != other.depth) return false

        return true
    }

    override fun hashCode(): Int {
        var result = txId.hashCode()
        result = 31 * result + state.hashCode()
        result = 31 * result + (appearsInBestChainBlock?.hashCode() ?: 0)
        result = 31 * result + appearsInBlock.hashCode()
        result = 31 * result + appearsAtChainHeight
        result = 31 * result + depth
        return result
    }

    override fun toString(): String {
        return "TransactionMeta(txId=$txId, state=$state, appearsInBestChainBlock=$appearsInBestChainBlock, appearsInBlock=$appearsInBlock, appearsAtChainHeight=$appearsAtChainHeight, depth=$depth)"
    }
}
