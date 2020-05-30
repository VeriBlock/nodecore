package org.veriblock.miners.pop.service

import org.veriblock.core.utilities.extensions.formatAtomicLongWithDecimal
import org.veriblock.lite.core.Context
import org.veriblock.miners.pop.core.ApmOperation
import org.veriblock.miners.pop.core.ApmOperationState
import org.veriblock.miners.pop.core.MiningOperationState
import org.veriblock.miners.pop.core.nextState
import org.veriblock.miners.pop.service.ApmOperationExplainer.OperationStatus.*

class ApmOperationExplainer(
    val context: Context
) {
    fun explainOperation(operation: ApmOperation): List<OperationStage> {
        // TODO: Explain failed ops down to the state at which they failed
        if (operation.isFailed()) {
            return listOf(OperationStage(MiningOperationState.FAILED, CURRENT, listOf(operation.failureReason ?: "mine")))
        }
        val currentState = operation.state
        return ApmOperationState.ALL.map { operationState ->
            val status = operationState.getOperationStatus(currentState)
            OperationStage(
                operationState = operationState,
                status = status,
                extraInformation = if (status == DONE) operationState.getExtraInformation(operation) else emptyList()
            )
        }
    }

    private fun MiningOperationState.getOperationStatus(currentState: MiningOperationState) = when {
        id <= currentState.id -> DONE
        id == currentState.id + 1 -> CURRENT
        else -> PENDING
    }

    private fun MiningOperationState.getExtraInformation(operation: ApmOperation) = when (this) {
        ApmOperationState.INITIAL -> {
            listOf("Assigned id: ${operation.id}")
        }
        ApmOperationState.INSTRUCTION -> {
            listOf("Endorsed ${operation.chain.name} block height: ${operation.endorsedBlockHeight}")
        }
        ApmOperationState.ENDORSEMENT_TRANSACTION -> {
            val transaction = operation.endorsementTransaction
            listOf(
                "${context.vbkTokenName} endorsement transaction id: ${transaction?.txId}",
                "${context.vbkTokenName} endorsement transaction fee: ${transaction?.fee?.formatAtomicLongWithDecimal()}"
            )
        }
        ApmOperationState.CONFIRMED -> {
            listOf("${context.vbkTokenName} endorsement transaction has been confirmed")
        }
        ApmOperationState.BLOCK_OF_PROOF -> {
            val blockOfProof = operation.blockOfProof
            listOf("${context.vbkTokenName} block of proof: ${blockOfProof?.hash} @ ${blockOfProof?.height}")
        }
        ApmOperationState.PROVEN -> {
            listOf(
                "Merkle path: ${operation.merklePath?.toCompactString()}",
                "Waiting for the keystone of proof"
            )
        }
        ApmOperationState.CONTEXT -> {
            listOf("")
        }
        ApmOperationState.SUBMITTED_POP_DATA -> {
            val payoutBlockHeight = (operation.miningInstruction?.endorsedBlockHeight ?: 0) + operation.chain.getPayoutInterval()
            listOf(
                "VTB submitted to ${operation.chain.name}! ${operation.chain.name} PoP TxId: ${operation.proofOfProofId}",
                "Waiting for reward to be paid in ${operation.chain.name} block @ $payoutBlockHeight to ${operation.chain.name} address ${operation.miningInstruction?.publicationData?.payoutInfo}"
            )
        }
        ApmOperationState.PAYOUT_DETECTED -> {
            listOf("Payout detected in ${operation.chain.name} block ${operation.payoutBlockHash}! Amount: ${operation.payoutAmount?.formatAtomicLongWithDecimal()}")
        }
        else -> listOf("FAILED")
    }

    data class OperationStage (
        val operationState: MiningOperationState,
        val status: OperationStatus,
        val extraInformation: List<String>
    )

    enum class OperationStatus {
        PENDING,
        CURRENT,
        DONE
    }
}
