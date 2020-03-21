// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.core

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import mu.KLogger
import nodecore.miners.pop.EventBus
import nodecore.miners.pop.model.PopMiningInstruction
import org.bitcoinj.core.Block
import org.bitcoinj.core.Transaction
import org.bitcoinj.core.TransactionConfidence
import org.veriblock.core.utilities.Utility
import org.veriblock.core.utilities.createLogger
import java.util.ArrayList
import java.util.Collections
import java.util.UUID

private val logger = createLogger {}

class MiningOperation(
    val id: String = UUID.randomUUID().toString().substring(0, 8),
    changeHistory: List<StateChangeEvent> = emptyList(),
    status: OperationStatus = OperationStatus.UNKNOWN,
    var endorsedBlockHeight: Int? = null,
    var reconstituting: Boolean = false
) {
    private val changeHistory: MutableList<StateChangeEvent>

    var status = status
        private set

    var state: OperationState = OperationState.Initial
        private set

    val timestamp = System.currentTimeMillis()

    var job: Job? = null

    val transactionConfidenceEventChannel = BroadcastChannel<TransactionConfidence.ConfidenceType>(Channel.CONFLATED)

    private val transactionListener = { confidence: TransactionConfidence, reason: TransactionConfidence.Listener.ChangeReason ->
        if (
            reason == TransactionConfidence.Listener.ChangeReason.TYPE && (
                confidence.confidenceType == TransactionConfidence.ConfidenceType.PENDING ||
                    confidence.confidenceType == TransactionConfidence.ConfidenceType.BUILDING
                )
        ) {
            transactionConfidenceEventChannel.offer(confidence.confidenceType)
        }
    }

    init {
        this.changeHistory = ArrayList(changeHistory)
    }

    fun begin() {
        status = OperationStatus.RUNNING
    }

    fun getChangeHistory(): List<StateChangeEvent> {
        return Collections.unmodifiableList(changeHistory)
    }

    private fun setState(state: OperationState) {
        this.state = state
        if (!reconstituting) {
            logger.debug(this) { "New state: $state" }
            informStateChangedListeners(state)
        }
    }

    fun setMiningInstruction(miningInstruction: PopMiningInstruction) {
        endorsedBlockHeight = miningInstruction.endorsedBlockHeight
        setState(OperationState.Instruction(miningInstruction))
    }

    fun setTransaction(transaction: Transaction, transactionBytes: ByteArray) {
        val currentState = state as? OperationState.Instruction
            ?: error("Trying to set transaction without having the mining instruction")

        val transactionState = OperationState.EndorsementTransaction(currentState, transaction, transactionBytes)
        setState(transactionState)

        transaction.confidence.addEventListener(transactionListener)
        GlobalScope.launch {
            for (confidenceType in transactionConfidenceEventChannel.openSubscription()) {
                if (confidenceType == TransactionConfidence.ConfidenceType.PENDING) {
                    EventBus.transactionSufferedReorgEvent.trigger(this@MiningOperation)
                    // Reset the state to the endorsement transaction pending for confirmation
                    setState(transactionState)
                }
            }
        }
    }

    fun setConfirmed() {
        val currentState = state as? OperationState.EndorsementTransaction
            ?: error("Trying to set as transaction confirmed without such transaction")
        setState(OperationState.Confirmed(currentState))
    }

    fun setBlockOfProof(blockOfProof: Block) {
        val currentState = state as? OperationState.Confirmed
            ?: error("Trying to set block of proof without having confirmed the transaction")
        setState(OperationState.BlockOfProof(currentState, blockOfProof))
    }

    fun setMerklePath(merklePath: String) {
        val currentState = state as? OperationState.BlockOfProof
            ?: error("Trying to set merkle path without the block of proof")
        setState(OperationState.Proven(currentState, merklePath))
    }

    fun setContext(context: List<Block>) {
        val currentState = state as? OperationState.Proven
            ?: error("Trying to set Bitcoin context without the merkle path")
        setState(OperationState.Context(currentState, context))
    }

    fun setProofOfProofId(proofOfProofId: String) {
        val currentState = state as? OperationState.Context
            ?: error("Trying to set Proof of Proof id without having the context")
        setState(OperationState.SubmittedPopData(currentState, proofOfProofId))
    }

    fun setVbkEndorsementTransactionConfirmed() {
        val currentState = state as? OperationState.SubmittedPopData
            ?: error("Trying to confirm VBK Endorsement Transaction without having its id")
        setState(OperationState.VbkEndorsementTransactionConfirmed(currentState))
    }

    fun complete(payoutBlockHash: String, payoutAmount: String) {
        val currentState = state
        if (currentState !is OperationState.VbkEndorsementTransactionConfirmed) {
            fail("Trying to mark the process as complete without having submitted the PoP data")
            return
        }
        logger.info { "Operation $id has completed! Payout: $payoutAmount VBK" }
        status = OperationStatus.COMPLETED
        setState(OperationState.Completed(currentState, payoutBlockHash, payoutAmount))

        stopJob()
    }

    fun fail(reason: String) {
        logger.warn { "Operation $id failed: $reason" }
        status = OperationStatus.FAILED
        setState(OperationState.Failed(state, reason))

        stopJob()
    }

    fun isFailed() = status == OperationStatus.FAILED || state is OperationState.Failed

    fun stopJob() {
        job?.cancel()
        job = null
        state.endorsementTransaction?.confidence?.removeEventListener(transactionListener)
    }

    private fun informStateChangedListeners(reason: OperationState) {
        changeHistory.add(StateChangeEvent(reason.toString(), Utility.getCurrentTimeSeconds()))

        EventBus.popMiningOperationStateChangedEvent.trigger(this)
    }

    override fun toString(): String {
        return "MiningOperation(id='$id', state='$state')"
    }
}

class StateChangeEvent(
    val state: String,
    val timestamp: Int
)

// Utility functions for logging on an operation
inline fun KLogger.trace(operation: MiningOperation, crossinline msg: () -> Any?) = trace { "[${operation.id}] ${msg()}" }
inline fun KLogger.debug(operation: MiningOperation, crossinline msg: () -> Any?) = debug { "[${operation.id}] ${msg()}" }
inline fun KLogger.info(operation: MiningOperation, crossinline msg: () -> Any?) = info { "[${operation.id}] ${msg()}" }
inline fun KLogger.warn(operation: MiningOperation, crossinline msg: () -> Any?) = warn { "[${operation.id}] ${msg()}" }
inline fun KLogger.error(operation: MiningOperation, crossinline msg: () -> Any?) = error { "[${operation.id}] ${msg()}" }
inline fun KLogger.error(operation: MiningOperation, e: Throwable, crossinline msg: () -> Any?) = error(e) { "[${operation.id}] ${msg()}" }
