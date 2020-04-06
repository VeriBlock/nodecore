package org.veriblock.miners.pop.core

import kotlinx.coroutines.Job
import mu.KLogger
import org.veriblock.core.contracts.MiningInstruction
import org.veriblock.core.contracts.WithDetailedInfo
import org.veriblock.core.utilities.Utility
import org.veriblock.core.utilities.createLogger
import java.util.Collections

private val logger = createLogger {}

abstract class MiningOperation<
    MI : MiningInstruction,
    SPT : SpTransaction,
    SPB : SpBlock,
    MP : MerklePath,
    CD : WithDetailedInfo
    >(
    val id: String,
    var endorsedBlockHeight: Int?,
    changeHistory: List<StateChangeEvent>,
    var reconstituting: Boolean = false
) {
    var state: OperationState = OperationState.INITIAL
        private set

    var job: Job? = null

    var miningInstruction: MI? = null
        private set
    var endorsementTransaction: SPT? = null
        private set
    var blockOfProof: SPB? = null
        private set
    var merklePath: MP? = null
        private set
    var context: CD? = null
        private set
    var proofOfProofId: String? = null
        private set
    var payoutBlockHash: String? = null
        private set
    var payoutAmount: String? = null
        private set
    var failureReason: String? = null
        private set

    private val changeHistory: MutableList<StateChangeEvent> = ArrayList(changeHistory)

    val timestamp = System.currentTimeMillis()

    protected fun setState(state: OperationState) {
        if (state.id > 0 && this.state.id != state.id - 1) {
            error("Trying to set state from ${this.state} directly to $state")
        }
        this.state = state
        if (!reconstituting) {
            logger.debug(this) { "New state: $state" }
            changeHistory.add(StateChangeEvent(state.toString(), Utility.getCurrentTimeSeconds()))
            onStateChanged()
        }
    }

    open fun onStateChanged() {
    }

    fun setMiningInstruction(miningInstruction: MI) {
        endorsedBlockHeight = miningInstruction.endorsedBlockHeight
        this.miningInstruction = miningInstruction
        setState(OperationState.INSTRUCTION)
    }

    fun setTransaction(transaction: SPT) {
        if (state != OperationState.INSTRUCTION) {
            error("Trying to set transaction without having the mining instruction")
        }
        endorsementTransaction = transaction
        setState(OperationState.ENDORSEMENT_TRANSACTION)
        onTransactionSet(transaction)
    }

    open fun onTransactionSet(transaction: SPT) {
    }

    fun setConfirmed() {
        if (state != OperationState.ENDORSEMENT_TRANSACTION) {
            error("Trying to set as transaction confirmed without such transaction")
        }
        setState(OperationState.CONFIRMED)
    }

    fun setBlockOfProof(blockOfProof: SPB) {
        if (state != OperationState.CONFIRMED) {
            error("Trying to set block of proof without having confirmed the transaction")
        }
        this.blockOfProof = blockOfProof
        setState(OperationState.BLOCK_OF_PROOF)
    }

    fun setMerklePath(merklePath: MP) {
        if (state != OperationState.BLOCK_OF_PROOF) {
            error("Trying to set merkle path without the block of proof")
        }
        this.merklePath = merklePath
        setState(OperationState.PROVEN)
    }

    fun setContext(context: CD) {
        if (state != OperationState.PROVEN) {
            error("Trying to set context without the merkle path")
        }
        this.context = context
        setState(OperationState.CONTEXT)
    }

    fun setProofOfProofId(proofOfProofId: String) {
        if (state != OperationState.CONTEXT) {
            error("Trying to set Proof of Proof id without having the context")
        }
        this.proofOfProofId = proofOfProofId
        setState(OperationState.SUBMITTED_POP_DATA)
    }

    fun complete(payoutBlockHash: String, payoutAmount: String) {
        if (state != OperationState.SUBMITTED_POP_DATA) {
            error("Trying to mark the process as complete without having submitted the PoP data")
        }
        this.payoutBlockHash = payoutBlockHash
        this.payoutAmount = payoutAmount
        setState(OperationState.COMPLETED)

        onCompleted()
        stopJob()
    }

    open fun onCompleted() {}

    fun fail(reason: String) {
        logger.warn { "Operation $id failed: $reason" }
        setState(OperationState.FAILED)

        onFailed()
        stopJob()
    }

    open fun onFailed() {}

    fun isFailed() = state == OperationState.FAILED

    fun stopJob() {
        if (state != OperationState.COMPLETED) {
            job?.cancel()
        }
        job = null
    }

    fun getChangeHistory(): List<StateChangeEvent> {
        return Collections.unmodifiableList(changeHistory)
    }

    fun getDetailedInfo(): Map<String, String> {
        val result = LinkedHashMap<String, String>()
        miningInstruction?.let {
            result.putAll(it.detailedInfo)
        }
        endorsementTransaction?.let {
            result["endorsementTransactionId"] = it.txId
            result["endorsementTransactionFee"] = it.fee
        }
        blockOfProof?.let {
            result["blockOfProof"] = it.hash
        }
        merklePath?.let {
            result["merklePath"] = it.compactFormat
        }
        context?.let {
            result.putAll(it.detailedInfo)
        }
        proofOfProofId?.let {
            result["proofOfProofId"] = it
        }
        payoutBlockHash?.let {
            result["payoutBlockHash"] = it
        }
        payoutAmount?.let {
            result["payoutAmount"] = it
        }
        failureReason?.let {
            result["failureReason"] = it
        }
        return result
    }

    override fun toString(): String {
        return "MiningOperation(id='$id', state='${state.description}')"
    }
}

interface SpTransaction {
    val txId: String
    val fee: String
}

interface MerklePath {
    val compactFormat: String
}

interface SpBlock {
    val hash: String
}

class StateChangeEvent(
    val state: String,
    val timestamp: Int
)

// Utility functions for logging on an operation
inline fun <MI : MiningInstruction, SPT : SpTransaction, SPB : SpBlock, MP : MerklePath, CD : WithDetailedInfo>
    KLogger.trace(operation: MiningOperation<MI, SPT, SPB, MP, CD>, crossinline msg: () -> Any?) = trace { "[${operation.id}] ${msg()}" }

inline fun <MI : MiningInstruction, SPT : SpTransaction, SPB : SpBlock, MP : MerklePath, CD : WithDetailedInfo>
    KLogger.debug(operation: MiningOperation<MI, SPT, SPB, MP, CD>, crossinline msg: () -> Any?) = debug { "[${operation.id}] ${msg()}" }

inline fun <MI : MiningInstruction, SPT : SpTransaction, SPB : SpBlock, MP : MerklePath, CD : WithDetailedInfo>
    KLogger.info(operation: MiningOperation<MI, SPT, SPB, MP, CD>, crossinline msg: () -> Any?) = info { "[${operation.id}] ${msg()}" }

inline fun <MI : MiningInstruction, SPT : SpTransaction, SPB : SpBlock, MP : MerklePath, CD : WithDetailedInfo>
    KLogger.warn(operation: MiningOperation<MI, SPT, SPB, MP, CD>, crossinline msg: () -> Any?) = warn { "[${operation.id}] ${msg()}" }

inline fun <MI : MiningInstruction, SPT : SpTransaction, SPB : SpBlock, MP : MerklePath, CD : WithDetailedInfo>
    KLogger.error(operation: MiningOperation<MI, SPT, SPB, MP, CD>, crossinline msg: () -> Any?) = error { "[${operation.id}] ${msg()}" }

inline fun <MI : MiningInstruction, SPT : SpTransaction, SPB : SpBlock, MP : MerklePath, CD : WithDetailedInfo>
    KLogger.error(operation: MiningOperation<MI, SPT, SPB, MP, CD>, e: Throwable, crossinline msg: () -> Any?) = error(
    e
) { "[${operation.id}] ${msg()}" }
