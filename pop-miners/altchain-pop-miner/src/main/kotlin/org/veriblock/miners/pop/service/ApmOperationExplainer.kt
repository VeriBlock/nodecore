package org.veriblock.miners.pop.service

import org.veriblock.core.utilities.extensions.formatAtomicLongWithDecimal
import org.veriblock.miners.pop.core.ApmContext
import org.veriblock.miners.pop.api.dto.OperationWorkflow
import org.veriblock.miners.pop.api.dto.OperationWorkflowStage
import org.veriblock.miners.pop.core.ApmOperation
import org.veriblock.miners.pop.core.ApmOperationState
import org.veriblock.miners.pop.core.MiningOperationState
import org.veriblock.miners.pop.service.ApmOperationExplainer.OperationStatus.*
import org.veriblock.sdk.alt.PayoutDetectionType
import java.util.*

class ApmOperationExplainer(
    val context: ApmContext
) {
    fun explainOperation(operation: ApmOperation): OperationWorkflow {
        val currentState = if (!operation.isFailed()) {
            operation.state
        } else {
            getStateFromFailedOperation(operation)
        }
        return OperationWorkflow(operation.id, ApmOperationState.ALL.map { operationState ->
            val status = operationState.getOperationStatus(currentState)
            val currentFailed = operation.isFailed() && status == CURRENT
            val statusDisplay = if (status != PENDING) {
                if (!currentFailed) status.toString() else "FAILED"
            } else {
                ""
            }
            val taskName =
                (operationState.previousState?.taskName ?: "Start").uppercase(Locale.getDefault()).replace(' ', '_')
            val taskId = operationState.id + 1
            val taskIdString = if (taskId / 10 > 0) "$taskId." else " $taskId."
            OperationWorkflowStage(
                status = statusDisplay,
                taskName = "$taskIdString $taskName",
                extraInformation = when (status) {
                    DONE -> operationState.getExtraInformation(operation)
                    CURRENT -> operationState.getCurrentHint(operation)
                    PENDING -> operationState.getPendingHint(operation)
                }
            )
        } + OperationWorkflowStage(
            if (operation.state == MiningOperationState.COMPLETED) "DONE" else "",
            "9. COMPLETED",
            if (operation.state == MiningOperationState.COMPLETED) {
                when (operation.chain.config.payoutDetectionType) {
                    PayoutDetectionType.COINBASE -> "Paid amount: ${operation.payoutAmount?.formatAtomicLongWithDecimal()}"
                    PayoutDetectionType.BALANCE_DELTA -> "Paid amount: ${operation.payoutAmount?.formatAtomicLongWithDecimal()} (estimation based on wallet's incoming transactions)"
                    PayoutDetectionType.DISABLED -> "Paid amount: Unknown (payout detection is disabled)"
                }
            } else {
                ""
            }
        ))
    }

    private fun getStateFromFailedOperation(operation: ApmOperation): MiningOperationState {
        return when {
            operation.atvId != null -> ApmOperationState.SUBMITTED_POP_DATA
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
        ApmOperationState.INITIAL ->
            "Created APM operation id: ${operation.id}"
        ApmOperationState.INSTRUCTION -> {
            "Endorsed ${operation.chain.name} block height: ${operation.endorsedBlockHeight}"
        }
        ApmOperationState.ENDORSEMENT_TRANSACTION -> {
            val transaction = operation.endorsementTransaction
            "${context.vbkTokenName} endorsement transaction id: ${transaction?.txId} (fee: ${transaction?.fee?.formatAtomicLongWithDecimal()}, fee per byte: ${transaction?.feePerByte})"
        }
        ApmOperationState.ENDORSEMENT_TX_CONFIRMED ->
            ""
        ApmOperationState.BLOCK_OF_PROOF -> {
            val blockOfProof = operation.blockOfProof
            "${context.vbkTokenName} Block of Proof: ${blockOfProof?.hash} @ ${blockOfProof?.height}"
        }
        ApmOperationState.PROVEN ->
            "Merkle path has been verified"
        ApmOperationState.SUBMITTED_POP_DATA ->
            "ATV: ${operation.atvId} submitted to ${operation.chain.name}!"
        ApmOperationState.PAYOUT_DETECTED -> {
            when (operation.chain.config.payoutDetectionType) {
                PayoutDetectionType.COINBASE ->
                    operation.miningInstruction?.let { miningInstruction ->
                        val payoutBlockHeight = miningInstruction.endorsedBlockHeight + operation.chain.getPayoutDelay()
                        val address = operation.chain.extractAddressDisplay(miningInstruction.publicationData.payoutInfo)
                        "Payout detected in ${operation.chain.name} block $payoutBlockHeight to ${operation.chain.name} address $address"
                    } ?: "Payout detected in ${operation.chain.name}"
                PayoutDetectionType.BALANCE_DELTA ->
                    "TODO"
                PayoutDetectionType.DISABLED ->
                    "Skipped"
            }
        }
        else ->
            ""
    }

    private fun MiningOperationState.getCurrentHint(operation: ApmOperation) = operation.failureReason ?: when (this) {
        ApmOperationState.ENDORSEMENT_TX_CONFIRMED ->
            "Waiting for ${context.vbkTokenName} endorsement transaction to appear in a block"
        ApmOperationState.SUBMITTED_POP_DATA ->
            "Submitting PoP Data to ${operation.chain.name}"
        ApmOperationState.PAYOUT_DETECTED -> {
            when (operation.chain.config.payoutDetectionType) {
                PayoutDetectionType.COINBASE ->
                    operation.miningInstruction?.let { miningInstruction ->
                        val payoutBlockHeight = miningInstruction.endorsedBlockHeight + operation.chain.getPayoutDelay()
                        val address = operation.chain.extractAddressDisplay(miningInstruction.publicationData.payoutInfo)
                        return if(operation.requiredConfirmations == null) {
                            "Waiting for ATV to be mined in a block..."
                        } else {
                            "Got ${operation.currentConfirmations ?: "0"}/${operation.requiredConfirmations} confirmations for ATV: ${operation.atvId} to be paid in ${operation.chain.name} block @ $payoutBlockHeight to ${operation.chain.name} address $address"
                        }
                    } ?: "Waiting for reward to be paid"
                PayoutDetectionType.BALANCE_DELTA ->
                    "TODO"
                PayoutDetectionType.DISABLED ->
                    "Skipping..."
            }
        }
        else ->
            ""
    }

    private fun MiningOperationState.getPendingHint(operation: ApmOperation) = if (!operation.isFailed()) {
        when (this) {
            ApmOperationState.ENDORSEMENT_TX_CONFIRMED ->
                "Will wait for ${context.vbkTokenName} endorsement transaction to appear in a block"
            ApmOperationState.SUBMITTED_POP_DATA ->
                "Will submit PoP Data to ${operation.chain.name}"
            ApmOperationState.PAYOUT_DETECTED -> {
                when (operation.chain.config.payoutDetectionType) {
                    PayoutDetectionType.COINBASE ->
                        operation.miningInstruction?.let { miningInstruction ->
                            val payoutBlockHeight = miningInstruction.endorsedBlockHeight + operation.chain.getPayoutDelay()
                            val address = operation.chain.extractAddressDisplay(miningInstruction.publicationData.payoutInfo)
                            "Will wait for reward to be paid in ${operation.chain.name} block @ $payoutBlockHeight to ${operation.chain.name} address $address"
                        } ?: "Will wait for reward to be paid"
                    PayoutDetectionType.BALANCE_DELTA ->
                        "TODO"
                    PayoutDetectionType.DISABLED ->
                        "Disabled"
                }
            }
            else ->
                ""
        }
    } else {
        ""
    }

    enum class OperationStatus {
        PENDING,
        CURRENT,
        DONE
    }
}
