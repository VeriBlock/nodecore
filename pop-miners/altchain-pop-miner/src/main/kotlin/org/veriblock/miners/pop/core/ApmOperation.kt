// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.core

import org.veriblock.core.utilities.Utility
import org.veriblock.core.utilities.createLogger
import org.veriblock.lite.core.AsyncEvent
import org.veriblock.lite.transactionmonitor.WalletTransaction
import org.veriblock.lite.util.Threading
import org.veriblock.sdk.alt.MiningInstruction
import org.veriblock.sdk.models.VeriBlockBlock
import org.veriblock.sdk.models.VeriBlockMerklePath
import org.veriblock.sdk.models.VeriBlockPublication
import java.util.ArrayList
import java.util.Collections
import java.util.UUID

private val logger = createLogger {}

class ApmOperation(
    id: String = UUID.randomUUID().toString().substring(0, 8),
    val chainId: String,
    changeHistory: List<StateChangeEvent> = emptyList(),
    status: OperationStatus = OperationStatus.UNKNOWN,
    var blockHeight: Int? = null
) : MiningOperation(id, status) {
    val stateChangedEvent = AsyncEvent<OperationState>(Threading.MINER_THREAD)

    private val changeHistory: MutableList<StateChangeEvent>

    var state: OperationState = OperationState.Initial
        private set

    override val stateType: OperationStateType
        get() = state.type

    val timestamp = System.currentTimeMillis()

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
        logger.debug(this) { "New state: $state" }
        informStateChangedListeners(state)
    }

    fun setMiningInstruction(miningInstruction: MiningInstruction) {
        blockHeight = miningInstruction.endorsedBlockHeight
        setState(OperationState.Instruction(miningInstruction))
    }

    fun setTransaction(transaction: WalletTransaction) {
        val currentState = state as? OperationState.Instruction
            ?: error("Trying to set transaction without having the mining instruction")
        setState(OperationState.EndorsementTransaction(currentState, transaction))
    }

    fun setConfirmed() {
        val currentState = state as? OperationState.EndorsementTransaction
            ?: error("Trying to set as transaction confirmed without such transaction")
        setState(OperationState.Confirmed(currentState))
    }

    fun setBlockOfProof(blockOfProof: VeriBlockBlock) {
        val currentState = state as? OperationState.Confirmed
            ?: error("Trying to set block of proof without having confirmed the transaction")
        setState(OperationState.BlockOfProof(currentState, blockOfProof))
    }

    fun setMerklePath(merklePath: VeriBlockMerklePath) {
        val currentState = state as? OperationState.BlockOfProof
            ?: error("Trying to set merkle path without the block of proof")
        setState(OperationState.Proven(currentState, merklePath))
    }

    fun setVeriBlockPublications(veriBlockPublications: List<VeriBlockPublication>) {
        val currentState = state as? OperationState.Proven
            ?: error("Trying to set VeriBlock context without the merkle path")
        setState(OperationState.VeriBlockPublications(currentState, veriBlockPublications))
    }

    fun setProofOfProofId(proofOfProofId: String) {
        val currentState = state as? OperationState.VeriBlockPublications
            ?: error("Trying to set VeriBlock Proof of Proof id without having the publication data")
        setState(OperationState.SubmittedPopData(currentState, proofOfProofId))
    }

    fun complete(payoutBlockHash: String, payoutAmount: String) {
        val currentState = state as? OperationState.SubmittedPopData
            ?: error("Trying to mark the process as complete without having submitted the PoP data")

        status = OperationStatus.COMPLETED
        setState(OperationState.Completed(currentState, payoutBlockHash, payoutAmount))
    }

    fun fail(reason: String) {
        logger.warn { "Operation $id failed for reason: $reason" }
        status = OperationStatus.FAILED
        setState(OperationState.Failed(state, reason))
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
