package org.veriblock.miners.pop.core

import ch.qos.logback.classic.Level
import kotlinx.coroutines.Job
import org.veriblock.core.contracts.MiningInstruction
import org.veriblock.core.contracts.WithDetailedInfo
import org.veriblock.core.utilities.createLogger
import org.veriblock.core.utilities.extensions.formatAtomicLongWithDecimal
import org.veriblock.miners.pop.service.Metrics
import java.time.LocalDateTime
import java.util.concurrent.CopyOnWriteArrayList

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
    val createdAt: LocalDateTime,
    logs: List<OperationLog>,
    var reconstituting: Boolean
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
    var payoutAmount: Long? = null
        private set
    var failureReason: String? = null
        private set

    private val logs: MutableList<OperationLog> = CopyOnWriteArrayList(logs)

    init {
        if (!reconstituting) {
            Metrics.startedOperationsCounter.increment()
        }
    }

    protected fun setState(state: OperationState) {
        if (state.id > 0 && this.state.id != state.id - 1) {
            error("Trying to set state from ${this.state} directly to $state")
        }
        this.state = state
        if (!reconstituting) {
            logger.debug(this, "New state: $state")
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

        endorsementTransaction?.let {
            Metrics.spentFeesCounter.increment(it.fee.toDouble())
        }
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

    fun complete(payoutBlockHash: String, payoutAmount: Long) {
        if (state != OperationState.SUBMITTED_POP_DATA) {
            error("Trying to mark the process as complete without having submitted the PoP data")
        }
        this.payoutBlockHash = payoutBlockHash
        this.payoutAmount = payoutAmount
        setState(OperationState.COMPLETED)

        onCompleted()
        Metrics.completedOperationsCounter.increment()
        Metrics.miningRewardCounter.increment(payoutAmount.toDouble())
    }

    open fun onCompleted() {}

    fun fail(reason: String) {
        logger.warn(this, "Failed: $reason")
        failureReason = reason
        setState(OperationState.FAILED)

        onFailed()
        cancelJob()
        Metrics.failedOperationsCounter.increment()
    }

    open fun onFailed() {}

    fun isFailed() = state == OperationState.FAILED

    fun cancelJob() {
        job?.cancel()
        job = null
    }

    fun getStateDescription() = if (isFailed()) {
        "Failed: $failureReason"
    } else {
        state.description
    }

    open fun isLoggingEnabled(level: Level): Boolean = true

    fun addLog(log: OperationLog) {
        logs.add(log)
    }

    fun getLogs(level: Level = Level.TRACE): List<OperationLog> {
        return logs.filter {
            Level.toLevel(it.level).isGreaterOrEqual(level)
        }
    }

    fun getDetailedInfo(): Map<String, String> {
        val result = LinkedHashMap<String, String>()
        miningInstruction?.let {
            result.putAll(it.detailedInfo)
        }
        endorsementTransaction?.let {
            result["endorsementTransactionId"] = it.txId
            result["endorsementTransactionFee"] = it.fee.formatAtomicLongWithDecimal()
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
            result["payoutAmount"] = it.toString()
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
    val fee: Long
}

interface MerklePath {
    val compactFormat: String
}

interface SpBlock {
    val hash: String
}
