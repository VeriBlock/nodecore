// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.veriblock.core.crypto.AnyVbkHash
import org.veriblock.core.crypto.VbkTxId
import java.util.ArrayList

class TransactionMeta(
    val txId: VbkTxId
) {
    private val _state = MutableStateFlow(MetaState.UNKNOWN)

    val stateFlow = _state.asStateFlow()

    val state: MetaState get() = _state.value

    var appearsInBestChainBlock: AnyVbkHash? = null

    private val appearsInBlock: MutableList<AnyVbkHash> = ArrayList()

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

    fun getAppearsInBlock(): List<AnyVbkHash> {
        return appearsInBlock
    }

    fun addBlockAppearance(hash: AnyVbkHash) {
        appearsInBlock.add(hash)
    }

    fun setState(state: MetaState) {
        if (_state.value == state) {
            return
        }

        if (state == MetaState.PENDING) {
            depth = 0
            appearsAtChainHeight = -1
        }
        _state.value = state
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
