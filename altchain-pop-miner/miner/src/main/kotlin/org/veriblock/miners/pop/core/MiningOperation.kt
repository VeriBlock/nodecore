// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.core

import mu.KLogger
import org.veriblock.core.utilities.Utility
import org.veriblock.core.utilities.createLogger
import org.veriblock.lite.core.AsyncEvent
import org.veriblock.lite.core.TransactionMeta
import org.veriblock.lite.transactionmonitor.WalletTransaction
import org.veriblock.lite.util.Threading
import org.veriblock.miners.pop.TX_DEPTH_COMPLETE
import org.veriblock.sdk.alt.PublicationDataWithContext
import org.veriblock.sdk.models.VeriBlockBlock
import org.veriblock.sdk.models.VeriBlockMerklePath
import org.veriblock.sdk.models.VeriBlockPublication
import java.util.*

private val logger = createLogger {}

class MiningOperation(
    val id: String = UUID.randomUUID().toString().substring(0, 8),
    val chainId: String,
    changeHistory: List<StateChangeEvent> = emptyList(),
    status: OperationStatus = OperationStatus.UNKNOWN,
    var blockHeight: Int? = null
) {
    val stateChangedEvent = AsyncEvent<OperationState>(Threading.MINER_THREAD)

    private val changeHistory: MutableList<StateChangeEvent>

    var status = status
        private set

    var state: OperationState = OperationState.Initial
        private set

    val timestamp = System.currentTimeMillis()

    fun getChangeHistory(): List<StateChangeEvent> {
        return Collections.unmodifiableList(changeHistory)
    }

    private fun setState(state: OperationState) {
        this.state = state
        logger.debug(this) { "New state: $state" }
        informStateChangedListeners(state)
    }

    fun setPublicationDataWithContext(publicationDataWithContext: PublicationDataWithContext) {
        blockHeight = publicationDataWithContext.endorsedBlockHeight
        setState(OperationState.PublicationData(publicationDataWithContext))
    }

    fun setTransaction(transaction: WalletTransaction) {
        val currentState = state
        if (currentState !is OperationState.PublicationData) {
            fail("Trying to set transaction without having the publication data")
            return
        }
        attachTransactionListeners(transaction)
        setState(OperationState.EndorsementTransaction(currentState, transaction))
    }

    fun setConfirmed() {
        val currentState = state
        if (currentState !is OperationState.EndorsementTransaction) {
            fail("Trying to set as transaction confirmed without such transaction")
            return
        }
        setState(OperationState.Confirmed(currentState))
    }

    fun setBlockOfProof(blockOfProof: VeriBlockBlock) {
        val currentState = state
        if (currentState !is OperationState.Confirmed) {
            fail("Trying to set block of proof without having confirmed the transaction")
            return
        }
        setState(OperationState.BlockOfProof(currentState, blockOfProof))
    }

    fun setMerklePath(merklePath: VeriBlockMerklePath) {
        val currentState = state
        if (currentState !is OperationState.BlockOfProof) {
            fail("Trying to set merkle path without the block of proof")
            return
        }
        setState(OperationState.TransactionProved(currentState, merklePath))
    }

    fun setKeystoneOfProof(block: VeriBlockBlock) {
        val currentState = state
        if (currentState !is OperationState.TransactionProved) {
            fail("Trying to set keystone of proof without having proved the transaction")
            return
        }
        setState(OperationState.KeystoneOfProof(currentState, block))
    }

    fun setVeriBlockPublications(veriBlockPublications: List<VeriBlockPublication>) {
        val currentState = state
        if (currentState !is OperationState.KeystoneOfProof) {
            fail("Trying to set VeriBlock publications without the keystone of proof")
            return
        }
        setState(OperationState.VeriBlockPublications(currentState, veriBlockPublications))
    }

    fun setProofOfProofId(proofOfProofId: String) {
        val currentState = state
        if (currentState !is OperationState.VeriBlockPublications) {
            fail("Trying to set VeriBlock proof of proof id without having the publication data")
            return
        }
        setState(OperationState.SubmittedPopData(currentState, proofOfProofId))
    }

    init {
        this.changeHistory = ArrayList(changeHistory)
    }

    fun begin() {
        status = OperationStatus.RUNNING
    }

    fun fail(reason: String) {
        logger.warn { "Operation $id failed for reason: $reason" }
        status = OperationStatus.FAILED
        detachTransactionListeners(state.transaction)
        setState(OperationState.Failed(state, reason))
    }

    fun complete() {
        val currentState = state
        if (currentState !is OperationState.SubmittedPopData) {
            fail("Trying to mark the process as complete without having submitted the PoP data")
            return
        }
        logger.info { "Operation $id has completed!" }
        status = OperationStatus.COMPLETED
        detachTransactionListeners(state.transaction)
        setState(OperationState.Completed(currentState))
    }

    private fun onReorganized() {
        fail("Reorganized") // FIXME
        setState(OperationState.Reorganized(state))
    }

    private fun attachTransactionListeners(transaction: WalletTransaction) {
        transaction.transactionMeta.stateChangedEvent.register(this) { metaState ->
            if (metaState === TransactionMeta.MetaState.PENDING) {
                onReorganized()
            } else if (metaState === TransactionMeta.MetaState.CONFIRMED) {
                setConfirmed()
            }
        }
        transaction.transactionMeta.depthChangedEvent.register(this) { depth ->
            if (depth >= TX_DEPTH_COMPLETE && state is OperationState.SubmittedPopData) {
                complete()
            }
        }
    }

    private fun detachTransactionListeners(transaction: WalletTransaction?) {
        if (transaction == null) {
            return
        }

        transaction.transactionMeta.depthChangedEvent.remove(this)
        transaction.transactionMeta.stateChangedEvent.remove(this)
    }

    private fun informStateChangedListeners(reason: OperationState) {
        changeHistory.add(StateChangeEvent(reason.toString(), Utility.getCurrentTimeSeconds()))

        stateChangedEvent.trigger(reason)
    }

    override fun toString(): String {
        return "MiningOperation(id='$id', chainId='$chainId', state='$state')"
    }
}

class StateChangeEvent(
    val state: String,
    val timestamp: Int
)

// Utility functions for logging on an operation
inline fun KLogger.debug(operation: MiningOperation, crossinline msg: () -> Any?) = debug { "[${operation.id}] ${msg()}" }
inline fun KLogger.info(operation: MiningOperation, crossinline msg: () -> Any?) = info { "[${operation.id}] ${msg()}" }
inline fun KLogger.warn(operation: MiningOperation, crossinline msg: () -> Any?) = warn { "[${operation.id}] ${msg()}" }
inline fun KLogger.error(operation: MiningOperation, crossinline msg: () -> Any?) = error { "[${operation.id}] ${msg()}" }
inline fun KLogger.error(operation: MiningOperation, e: Throwable, crossinline msg: () -> Any?) = error(e) { "[${operation.id}] ${msg()}" }
