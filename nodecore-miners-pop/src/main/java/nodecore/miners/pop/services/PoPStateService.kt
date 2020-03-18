// VeriBlock PoP Miner
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.miners.pop.services

import com.google.protobuf.ByteString
import nodecore.miners.pop.core.MiningOperation
import nodecore.miners.pop.core.OperationState
import nodecore.miners.pop.core.OperationStateType
import nodecore.miners.pop.core.OperationStatus
import nodecore.miners.pop.core.info
import nodecore.miners.pop.events.EventBus
import nodecore.miners.pop.model.PopMiningInstruction
import nodecore.miners.pop.storage.OperationStateData
import nodecore.miners.pop.storage.PopRepository
import nodecore.miners.pop.storage.ProofOfProof
import org.bitcoinj.core.Transaction
import org.veriblock.core.utilities.Utility
import org.veriblock.core.utilities.createLogger
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.ArrayList
import java.util.stream.Collectors

private val logger = createLogger {}

class PoPStateService(
    private val repository: PopRepository,
    private val bitcoinService: BitcoinService
) {

    init {
        EventBus.popMiningOperationStateChangedEvent.register(this, ::onMiningOperationStateChanged)
    }

    fun getActiveOperations(): List<MiningOperation> {
        return repository.getActiveOperations()?.asSequence()?.mapNotNull {
            try {
                reconstitute(ProofOfProof.OperationState.parseFrom(it.state))
            } catch (e: Exception) {
                logger.error(e.message, e)
                null
            }
        }?.toList() ?: emptyList()
    }

    fun getOperation(id: String): MiningOperation? {
        val stateData = repository.getOperation(id)
            ?: return null
        try {
            return reconstitute(ProofOfProof.OperationState.parseFrom(stateData.state))
        } catch (e: Exception) {
            logger.error(e.message, e)
        }
        return null
    }

    private fun onMiningOperationStateChanged(operation: MiningOperation) {
        try {
            val operationState = operation.state
            val serializedState = serialize(operation)
            val stateData = OperationStateData()
            stateData.id = operation.id
            stateData.status = operation.status.name
            stateData.action = operationState.type.name
            stateData.message = operationState.toString()
            stateData.state = serializedState
            stateData.isDone = operationState.type == OperationStateType.COMPLETE
            stateData.lastUpdated = Utility.getCurrentTimeSeconds()
            if (operationState is OperationState.Instruction) {
                stateData.endorsedBlockHash = operationState.miningInstruction.endorsedBlockHash
                stateData.endorsedBlockNumber = operationState.miningInstruction.endorsedBlockHeight
            }
            repository.saveOperationState(stateData)
            logger.info("Operation [${operation.id}] new state: ${operationState}")
        } catch (e: Exception) {
            logger.error(e.message, e)
        }
        return
    }

    private fun serialize(operation: MiningOperation): ByteArray? {
        val builder = ProofOfProof.OperationState.newBuilder()
        builder.id = operation.id
        builder.status = operation.status.name
        builder.action = operation.state.toString()
        builder.endorsedBlockNumber = operation.blockHeight ?: -1
        val state = operation.state
        if (state is OperationState.Instruction) {
            builder.miningInstructions = ProofOfProof.MiningInstruction.newBuilder()
                .setPublicationData(ByteString.copyFrom(state.miningInstruction.publicationData))
                .setEndorsedBlockHeader(ByteString.copyFrom(state.miningInstruction.endorsedBlockHeader))
                .setLastBitcoinBlock(ByteString.copyFrom(state.miningInstruction.lastBitcoinBlock))
                .setMinerAddress(ByteString.copyFrom(state.miningInstruction.minerAddress))
                .addAllBitcoinContextAtEndorsed(
                    state.miningInstruction.endorsedBlockContextHeaders.stream()
                        .map { bytes: ByteArray? -> ByteString.copyFrom(bytes) }
                        .collect(Collectors.toList())
                )
                .build()
        }
        if (state is OperationState.EndorsementTransaction) {
            builder.transaction = ByteString.copyFrom(state.endorsementTransactionBytes)
            builder.bitcoinTxId = state.endorsementTransaction.txId.toString()
        }
        if (state is OperationState.BlockOfProof) {
            builder.blockOfProof = ByteString.copyFrom(state.blockOfProof.bitcoinSerialize())
        }
        if (state is OperationState.Proven) {
            builder.merklePath = state.merklePath
        }
        if (state is OperationState.Context) {
            builder.addAllBitcoinContext(
                state.bitcoinContextBlocks.map { ByteString.copyFrom(it.bitcoinSerialize()) }
            )
        }
        if (state is OperationState.SubmittedPopData) {
            builder.popTxId = state.proofOfProofId
        }
        if (state is OperationState.VbkEndorsementTransactionConfirmed) {
            TODO()
        }
        if (state is OperationState.Completed) {
            TODO()
        }
        return serialize(builder.build())
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

    private fun reconstitute(state: ProofOfProof.OperationState): MiningOperation {
        logger.info("Reconstituting operation {}", state.id)
        val miningOperation = MiningOperation(
            id = state.id,
            status = OperationStatus.valueOf(state.status)
        )
        if (state.endorsedBlockNumber >= 0) {
            miningOperation.blockHeight = state.endorsedBlockNumber
        }
        if (state.miningInstructions != null) {
            val miningInstruction = PopMiningInstruction()
            miningInstruction.publicationData = state.miningInstructions.publicationData.toByteArray()
            miningInstruction.endorsedBlockHeader = state.miningInstructions.endorsedBlockHeader.toByteArray()
            miningInstruction.lastBitcoinBlock = state.miningInstructions.lastBitcoinBlock.toByteArray()
            miningInstruction.minerAddress = state.miningInstructions.minerAddress.toByteArray()
            miningInstruction.endorsedBlockContextHeaders = state.miningInstructions.bitcoinContextAtEndorsedList.map { it.toByteArray() }
            miningOperation.setMiningInstruction(miningInstruction)
        }
        if (state.transaction != null && state.transaction.size() > 0) {
            logger.info(miningOperation) { "Rebuilding transaction" }
            val transaction: Transaction = bitcoinService.makeTransaction(state.transaction.toByteArray())
            miningOperation.setTransaction(transaction, state.transaction.toByteArray())
            logger.info(miningOperation) { "Rebuilt transaction ${transaction.txId}" }
        }
        if (state.blockOfProof != null && state.blockOfProof.size() > 0) {
            miningOperation.setConfirmed()
            logger.info(miningOperation) { "Rebuilding block of proof" }
            val block = bitcoinService.makeBlock(state.blockOfProof.toByteArray())
            miningOperation.setBlockOfProof(block)
            logger.info(miningOperation) { "Reattached block of proof ${block.hashAsString}" }
        }
        if (state.merklePath != null) {
            miningOperation.setMerklePath(state.merklePath)
        }
        if (state.bitcoinContextList != null && state.bitcoinContextCount > 0) {
            logger.info(miningOperation) { "Rebuilding context blocks" }
            val contextBytes = state.bitcoinContextList.map { it.toByteArray() }
            val blocks = bitcoinService.makeBlocks(contextBytes)
            miningOperation.setContext(ArrayList(blocks))
        }
        if (state.popTxId != null) {
            miningOperation.setProofOfProofId(state.popTxId)
        }
        TODO("Serialize vbk endorsement confirmation and payout")
        return miningOperation
    }
}
