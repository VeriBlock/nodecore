// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.miners.pop.services

import com.google.protobuf.ByteString
import org.bitcoinj.core.Transaction
import org.veriblock.core.utilities.Utility
import org.veriblock.core.utilities.createLogger
import org.veriblock.miners.pop.EventBus
import org.veriblock.miners.pop.core.OperationState
import org.veriblock.miners.pop.core.OperationStatus
import org.veriblock.miners.pop.core.VpmOperation
import org.veriblock.miners.pop.core.debug
import org.veriblock.miners.pop.model.PopMiningInstruction
import org.veriblock.miners.pop.storage.OperationStateData
import org.veriblock.miners.pop.storage.PopRepository
import org.veriblock.miners.pop.storage.ProofOfProof
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.ArrayList

private val logger = createLogger {}

class PopStateService(
    private val repository: PopRepository,
    private val bitcoinService: BitcoinService
) {
    init {
        EventBus.popMiningOperationStateChangedEvent.register(this, ::onMiningOperationStateChanged)
    }

    fun getActiveOperations(): List<VpmOperation> {
        return repository.getActiveOperations()?.asSequence()?.mapNotNull {
            try {
                reconstitute(ProofOfProof.OperationState.parseFrom(it.state))
            } catch (e: Exception) {
                logger.error(e.message, e)
                null
            }
        }?.toList() ?: emptyList()
    }

    fun getOperation(id: String): VpmOperation? {
        val stateData = repository.getOperation(id)
            ?: return null
        try {
            return reconstitute(ProofOfProof.OperationState.parseFrom(stateData.state))
        } catch (e: Exception) {
            logger.error(e.message, e)
        }
        return null
    }

    private fun onMiningOperationStateChanged(operation: VpmOperation) {
        try {
            val operationState = operation.state
            val serializedState = serialize(operation)
            val stateData = OperationStateData()
            stateData.id = operation.id
            stateData.status = operation.status.name
            stateData.action = operationState.type.name
            stateData.message = operationState.toString()
            stateData.state = serializedState
            stateData.isDone = operation.status == OperationStatus.COMPLETED || operation.status == OperationStatus.FAILED
            stateData.lastUpdated = Utility.getCurrentTimeSeconds()
            if (operationState is OperationState.Instruction) {
                stateData.endorsedBlockHash = operationState.miningInstruction.endorsedBlockHash
                stateData.endorsedBlockNumber = operationState.miningInstruction.endorsedBlockHeight
            }
            repository.saveOperationState(stateData)
        } catch (e: Exception) {
            logger.error(e.message, e)
        }
        return
    }

    private fun serialize(operation: VpmOperation): ByteArray? {
        val protoState = ProofOfProof.OperationState.newBuilder().apply {
            id = operation.id
            status = operation.status.name
            action = operation.state.toString()
            endorsedBlockNumber = operation.endorsedBlockHeight ?: -1
            val state = operation.state.let {
                if (it !is OperationState.Failed) it else it.previous
            }
            if (state is OperationState.Instruction) {
                miningInstructions = ProofOfProof.MiningInstruction.newBuilder().apply {
                    publicationData = ByteString.copyFrom(state.miningInstruction.publicationData)
                    endorsedBlockHeader = ByteString.copyFrom(state.miningInstruction.endorsedBlockHeader)
                    lastBitcoinBlock = ByteString.copyFrom(state.miningInstruction.lastBitcoinBlock)
                    minerAddress = ByteString.copyFrom(state.miningInstruction.minerAddressBytes)
                    addAllBitcoinContextAtEndorsed(state.miningInstruction.endorsedBlockContextHeaders.map { ByteString.copyFrom(it) })
                }.build()
            }
            if (state is OperationState.EndorsementTransaction) {
                transaction = ByteString.copyFrom(state.endorsementTransactionBytes)
                bitcoinTxId = state.endorsementTransaction.txId.toString()
            }
            if (state is OperationState.BlockOfProof) {
                blockOfProof = ByteString.copyFrom(state.blockOfProof.bitcoinSerialize())
            }
            if (state is OperationState.Proven) {
                merklePath = state.merklePath
            }
            if (state is OperationState.Context) {
                addAllBitcoinContext(state.bitcoinContextBlocks.map { ByteString.copyFrom(it.bitcoinSerialize()) })
            }
            if (state is OperationState.SubmittedPopData) {
                popTxId = state.proofOfProofId
            }
            if (state is OperationState.Completed) {
                payoutBlockHash = state.payoutBlockHash
                payoutAmount = state.payoutAmount
            }
        }.build()
        return serialize(protoState)
    }

    private fun serialize(state: ProofOfProof.OperationState): ByteArray? {
        try {
            ByteArrayOutputStream().use { stream ->
                state.writeTo(stream)
                return stream.toByteArray()
            }
        } catch (ignored: IOException) {
        }
        return null
    }

    private fun reconstitute(state: ProofOfProof.OperationState): VpmOperation {
        logger.debug("Reconstituting operation {}", state.id)
        val status = OperationStatus.valueOf(state.status)
        val miningOperation = VpmOperation(
            id = state.id,
            status = status,
            reconstituting = true
        )
        if (state.endorsedBlockNumber >= 0) {
            miningOperation.endorsedBlockHeight = state.endorsedBlockNumber
        }
        if (state.miningInstructions != null && !state.miningInstructions.publicationData.isEmpty) {
            val miningInstruction = PopMiningInstruction(
                publicationData = state.miningInstructions.publicationData.toByteArray(),
                endorsedBlockHeader = state.miningInstructions.endorsedBlockHeader.toByteArray(),
                lastBitcoinBlock = state.miningInstructions.lastBitcoinBlock.toByteArray(),
                minerAddressBytes = state.miningInstructions.minerAddress.toByteArray(),
                endorsedBlockContextHeaders = state.miningInstructions.bitcoinContextAtEndorsedList.map { it.toByteArray() }
            )
            miningOperation.setMiningInstruction(miningInstruction)
        }
        if (state.transaction != null && state.transaction.size() > 0) {
            logger.debug(miningOperation) { "Rebuilding transaction" }
            val transaction: Transaction = bitcoinService.makeTransaction(state.transaction.toByteArray())
            miningOperation.setTransaction(transaction, state.transaction.toByteArray())
            logger.debug(miningOperation) { "Rebuilt transaction ${transaction.txId}" }
        }
        if (state.blockOfProof != null && state.blockOfProof.size() > 0) {
            miningOperation.setConfirmed()
            logger.debug(miningOperation) { "Rebuilding block of proof" }
            val block = bitcoinService.makeBlock(state.blockOfProof.toByteArray())
            miningOperation.setBlockOfProof(block)
            logger.debug(miningOperation) { "Reattached block of proof ${block.hashAsString}" }
        }
        if (!state.merklePath.isNullOrEmpty()) {
            miningOperation.setMerklePath(state.merklePath)
        }
        if (state.bitcoinContextList != null && state.bitcoinContextCount > 0) {
            logger.debug(miningOperation) { "Rebuilding context blocks" }
            val contextBytes = state.bitcoinContextList.map { it.toByteArray() }
            val blocks = bitcoinService.makeBlocks(contextBytes)
            miningOperation.setContext(ArrayList(blocks))
        } else if (!state.popTxId.isNullOrEmpty()) {
            miningOperation.setContext(emptyList())
        }
        if (!state.popTxId.isNullOrEmpty()) {
            miningOperation.setProofOfProofId(state.popTxId)
        }
        if (!state.payoutBlockHash.isNullOrEmpty()) {
            miningOperation.complete(state.payoutBlockHash, state.payoutAmount)
        }
        if (status == OperationStatus.FAILED) {
            miningOperation.fail("Loaded as failed")
        }
        miningOperation.reconstituting = false
        return miningOperation
    }
}
