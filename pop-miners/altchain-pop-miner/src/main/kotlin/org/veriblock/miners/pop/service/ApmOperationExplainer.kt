package org.veriblock.miners.pop.service

import org.veriblock.core.utilities.extensions.formatAtomicLongWithDecimal
import org.veriblock.core.utilities.extensions.toHex
import org.veriblock.lite.core.Context
import org.veriblock.miners.pop.core.ApmOperation
import org.veriblock.miners.pop.core.ApmOperationState
import org.veriblock.miners.pop.core.MiningOperationState
import org.veriblock.miners.pop.service.ApmOperationExplainer.OperationStatus.*

class ApmOperationExplainer(
    val context: Context
) {
    fun explainOperation(operation: ApmOperation): List<OperationStage> {
        val currentState = if (!operation.isFailed()) {
            operation.state
        } else {
            getStateFromFailedOperation(operation)
        }
        return ApmOperationState.ALL.map { operationState ->
            val status = operationState.getOperationStatus(currentState)
            val currentFailed = operation.isFailed() && status == CURRENT
            val statusDisplay = if (status != PENDING) {
                if (!currentFailed) status.toString() else "FAILED"
            } else {
                ""
            }
            val taskName = (operationState.previousState?.taskName ?: "Start").toUpperCase().replace(' ', '_')
            val taskId = operationState.id + 1
            val taskIdString = if (taskId / 10 > 0) "$taskId." else " $taskId."
            OperationStage(
                status = statusDisplay,
                taskName = "$taskIdString ${taskName}",
                extraInformation = when (status) {
                    DONE -> operationState.getExtraInformation(operation)
                    CURRENT -> operationState.getCurrentHint(operation)
                    PENDING -> operationState.getPendingHint(operation)
                }
            )
        } + OperationStage(
            if (operation.state == MiningOperationState.COMPLETED) "DONE" else "",
            "11. COMPLETED",
            listOf("")
        )
    }

    private fun getStateFromFailedOperation(operation: ApmOperation): MiningOperationState {
        return when {
            operation.proofOfProofId != null -> ApmOperationState.SUBMITTED_POP_DATA
            operation.publicationData != null -> ApmOperationState.CONTEXT
            operation.merklePath != null -> ApmOperationState.PROVEN
            operation.blockOfProof != null -> ApmOperationState.BLOCK_OF_PROOF
            operation.endorsementTransaction != null -> ApmOperationState.ENDORSEMENT_TRANSACTION
            operation.miningInstruction != null -> ApmOperationState.INSTRUCTION
            else -> ApmOperationState.INITIAL
        }
    }

    private fun MiningOperationState.getOperationStatus(currentState: MiningOperationState) = when {
        id <= currentState.id -> DONE
        id == currentState.id + 1 -> CURRENT
        else -> PENDING
    }

    private fun MiningOperationState.getExtraInformation(operation: ApmOperation) = when (this) {
        ApmOperationState.INITIAL -> {
            listOf("Created APM operation Id: ${operation.id}")
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
            listOf("")
        }
        ApmOperationState.BLOCK_OF_PROOF -> {
            val blockOfProof = operation.blockOfProof
            listOf("${context.vbkTokenName} block of proof: ${blockOfProof?.hash} @ ${blockOfProof?.height}")
        }
        ApmOperationState.PROVEN -> {
            listOf("Merkle path has been verified")
        }
        ApmOperationState.KEYSTONE_OF_PROOF -> {
            listOf("Keystone of Proof: ${operation.keystoneOfProof?.hash}")
        }
        ApmOperationState.CONTEXT -> {
            listOf("Retrieved ${operation.publicationData?.size} VTBs")
        }
        ApmOperationState.SUBMITTED_POP_DATA -> {
            listOf("VTBs submitted to ${operation.chain.name}! ${operation.chain.name} PoP TxId: ${operation.proofOfProofId}")
        }
        ApmOperationState.PAYOUT_DETECTED -> {
            listOf("Payout detected in ${operation.chain.name} block ${operation.payoutBlockHash}! Amount: ${operation.payoutAmount?.formatAtomicLongWithDecimal()}")
        }
        else ->
            emptyList()
    }

    private fun MiningOperationState.getCurrentHint(operation: ApmOperation) = operation.failureReason?.let { failureReason ->
        listOf(failureReason)
    } ?: when (this) {
        ApmOperationState.CONFIRMED -> {
            listOf("Waiting for ${context.vbkTokenName} endorsement transaction to appear in a block")
        }
        ApmOperationState.KEYSTONE_OF_PROOF -> {
            listOf("Waiting for the next ${context.vbkTokenName} keystone")
        }
        ApmOperationState.CONTEXT -> {
            listOf("Waiting for the VeriBlock network to build the VTBs to be submitted to ${operation.chain.name}")
        }
        ApmOperationState.SUBMITTED_POP_DATA -> {
            listOf("Submitting PoP Data to ${operation.chain.name}")
        }
        ApmOperationState.PAYOUT_DETECTED -> {
            operation.miningInstruction?.let { miningInstruction ->
                val payoutBlockHeight = miningInstruction.endorsedBlockHeight + operation.chain.getPayoutInterval()
                val address = operation.chain.extractAddressDisplay(miningInstruction.publicationData.payoutInfo)
                listOf("Waiting for reward to be paid in ${operation.chain.name} block @ $payoutBlockHeight to ${operation.chain.name} address $address")
            } ?: listOf("Waiting for reward to be paid")
        }
        else ->
            emptyList()
    }

    private fun MiningOperationState.getPendingHint(operation: ApmOperation) = if (!operation.isFailed()) {
        when (this) {
            ApmOperationState.CONFIRMED -> {
                listOf("Will wait for ${context.vbkTokenName} endorsement transaction to appear in a block")
            }
            ApmOperationState.KEYSTONE_OF_PROOF -> {
                listOf("Will wait for the next ${context.vbkTokenName} keystone after the endorsement transaction's block")
            }
            ApmOperationState.CONTEXT -> {
                listOf("Will wait for the VeriBlock network to build the VTBs to be submitted to ${operation.chain.name}")
            }
            ApmOperationState.SUBMITTED_POP_DATA -> {
                listOf("Will submit PoP Data to ${operation.chain.name}")
            }
            ApmOperationState.PAYOUT_DETECTED -> {
                operation.miningInstruction?.let { miningInstruction ->
                    val payoutBlockHeight = miningInstruction.endorsedBlockHeight + operation.chain.getPayoutInterval()
                    val address = operation.chain.extractAddressDisplay(miningInstruction.publicationData.payoutInfo)
                    listOf("Will wait for reward to be paid in ${operation.chain.name} block @ $payoutBlockHeight to ${operation.chain.name} address $address")
                } ?: listOf("Will wait for reward to be paid")
            }
            else ->
                emptyList()
        }
    } else {
        emptyList()
    }

    data class OperationStage (
        val status: String,
        val taskName: String,
        val extraInformation: List<String>
    )

    enum class OperationStatus {
        PENDING,
        CURRENT,
        DONE
    }
}
