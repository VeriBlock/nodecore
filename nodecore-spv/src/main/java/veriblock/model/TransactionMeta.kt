// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package veriblock.model

import org.veriblock.sdk.models.Sha256Hash
import org.veriblock.sdk.models.VBlakeHash
import veriblock.listeners.TransactionStateChangedListener
import java.util.ArrayList
import java.util.HashSet
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executor

class TransactionMeta(
    val txId: Sha256Hash
) {
    private val appearsInBlock: MutableList<VBlakeHash> = ArrayList()
    var appearsAtChainHeight = -1
    var depth = 0
    private val seenByPeers: MutableSet<String> = HashSet()
    private val stateChangedListeners: CopyOnWriteArrayList<ListenerRegistration<TransactionStateChangedListener>> = CopyOnWriteArrayList()
    var state = MetaState.UNKNOWN
        private set
    var appearsInBestChainBlock: VBlakeHash? = null

    fun getAppearsInBlock(): List<VBlakeHash> {
        return appearsInBlock
    }

    fun addBlockAppearance(hash: VBlakeHash) {
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

    fun addTransactionStateChangedListener(listener: TransactionStateChangedListener, executor: Executor) {
        stateChangedListeners.add(ListenerRegistration(listener, executor))
    }

    fun removeTransactionStateChangedListener(listener: TransactionStateChangedListener) {
        ListenerRegistration.Companion.removeFromList<TransactionStateChangedListener>(listener, stateChangedListeners)
    }

    private fun informListenersStateChanged() {
        val self = this
        for (registration in stateChangedListeners) {
            registration.executor.execute {
                registration.listener.onTransactionStateChanged(
                    self,
                    TransactionStateChangedListener.ChangeReason.STATE
                )
            }
        }
    }

    private fun informListenersDepthChanged() {
        val self = this
        for (registration in stateChangedListeners) {
            registration.executor.execute {
                registration.listener.onTransactionStateChanged(
                    self,
                    TransactionStateChangedListener.ChangeReason.DEPTH
                )
            }
        }
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
