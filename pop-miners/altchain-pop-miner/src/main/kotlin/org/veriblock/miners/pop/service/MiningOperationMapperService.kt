package org.veriblock.miners.pop.service

import org.veriblock.core.utilities.extensions.formatAtomicLongWithDecimal
import org.veriblock.lite.core.Context
import org.veriblock.miners.pop.core.ApmOperation
import org.veriblock.miners.pop.core.OperationState
import org.veriblock.miners.pop.service.MiningOperationMapperService.OperationStatus.*

class MiningOperationMapperService(
    val context: Context
) {
    fun getOperationData(operation: ApmOperation): List<OperationStage> {
        val currentState = operation.state
        return OperationState.values().filter {
            it != OperationState.FAILED
        }.map { operationState ->
            val status = operationState.getOperationStatus(currentState)
            OperationStage(
                operationState = operationState,
                status = status,
                extraInformation = if (status == DONE) operationState.getExtraInformation(operation) else emptyList()
            )
        }
    }

    private fun OperationState.getOperationStatus(currentState: OperationState) = when {
        id < currentState.id -> DONE
        id == currentState.id -> CURRENT
        else -> PENDING
    }

    fun OperationState.getExtraInformation(operation: ApmOperation) = when (this) {
        OperationState.INITIAL -> {
            listOf("Assigned id: ${operation.id}")
        }
        OperationState.INSTRUCTION -> {
            listOf("Endorsed ${operation.chain.name} block height: ${operation.endorsedBlockHeight}")
        }
        OperationState.ENDORSEMENT_TRANSACTION -> {
            operation.endorsementTransaction?.let { transaction ->
                listOf("Endorsed ${operation.chain.name} block: ${operation.endorsedBlockHeight} at ${transaction.txId}. Fee: ${transaction.fee.formatAtomicLongWithDecimal()}")
            } ?: listOf("Unable to acquire the endorsement transaction from the operation")
        }
        OperationState.CONFIRMED -> {
            operation.blockOfProof?.let {blockOfProof ->
                listOf("Endorsement transaction has been confirmed in ${context.vbkTokenName} block ${blockOfProof.block.hash} @ ${blockOfProof.block.height}")
            } ?: listOf("Unable to acquire the block of proof from the operation")
        }
        OperationState.BLOCK_OF_PROOF -> {
            listOf("")
        }
        OperationState.PROVEN -> {
            operation.merklePath?.let { merklePath ->
                listOf("Merkle path: ${merklePath.compactFormat}. Waiting for the keystone of proof.")
            } ?: listOf("Unable to acquire the merkle path from the operation")
        }
        OperationState.CONTEXT -> {
            listOf("")
        }
        OperationState.SUBMITTED_POP_DATA -> {
            listOf("VTB submitted to ${operation.chain.name}! ${operation.chain.name} PoP TxId: ${operation.proofOfProofId}")
        }
        OperationState.COMPLETED -> {
            listOf("To be paid in ${operation.chain.name} block $operation to ${operation.chain.name} address ${operation.payoutAmount}")
        }
        else -> listOf("FAILED")
    }

    data class OperationStage (
        val operationState: OperationState,
        val status: OperationStatus,
        val extraInformation: List<String>
    )

    enum class OperationStatus {
        PENDING,
        CURRENT,
        DONE
    }
}
