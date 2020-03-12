// VeriBlock PoP Miner
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.miners.pop.services

import com.google.protobuf.ByteString
import nodecore.miners.pop.contracts.PoPMiningInstruction
import nodecore.miners.pop.contracts.PoPMiningOperationState
import nodecore.miners.pop.events.EventBus.popMiningOperationStateChangedEvent
import nodecore.miners.pop.events.PoPMiningOperationStateChangedEventDto
import nodecore.miners.pop.storage.OperationStateData
import nodecore.miners.pop.storage.PopRepository
import nodecore.miners.pop.storage.ProofOfProof
import org.veriblock.core.utilities.Utility
import org.veriblock.core.utilities.createLogger
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.stream.Collectors

private val logger = createLogger {}

class PoPStateService(
    private val repository: PopRepository
) {

    init {
        popMiningOperationStateChangedEvent.register(this) { event ->
            onPoPMiningOperationStateChanged(event)
        }
    }

    fun getActiveOperations(): List<PoPMiningOperationState> {
        return repository.getActiveOperations()?.asSequence()?.mapNotNull {
            try {
                reconstitute(ProofOfProof.OperationState.parseFrom(it.state))
            } catch (e: Exception) {
                logger.error(e.message, e)
                null
            }
        }?.toList() ?: emptyList()
    }

    fun getOperation(id: String): PoPMiningOperationState? {
        val stateData = repository.getOperation(id)
            ?: return null
        try {
            return reconstitute(ProofOfProof.OperationState.parseFrom(stateData.state))
        } catch (e: Exception) {
            logger.error(e.message, e)
        }
        return null
    }

    fun onPoPMiningOperationStateChanged(event: PoPMiningOperationStateChangedEventDto) {
        try {
            val operationState = event.state
            val serializedState = serialize(event.state)
            val stateData = OperationStateData()
            stateData.id = operationState.operationId
            stateData.status = operationState.status.name
            stateData.action = operationState.currentActionAsString
            stateData.transactionStatus = if (operationState.transactionStatus != null) operationState.transactionStatus.name else ""
            stateData.message = operationState.message
            stateData.state = serializedState
            stateData.isDone = PoPMiningOperationState.Action.DONE == operationState.currentAction
            stateData.lastUpdated = Utility.getCurrentTimeSeconds()
            if (operationState.miningInstruction != null) {
                stateData.endorsedBlockHash = operationState.miningInstruction.blockHashAsString
                stateData.endorsedBlockNumber = operationState.blockNumber
            }
            logger.info("Operation [" + operationState.operationId + "] new status: " + stateData.status)
            repository.saveOperationState(stateData)
        } catch (e: Exception) {
            logger.error(e.message, e)
        }
        return
    }

    private fun serialize(operationState: PoPMiningOperationState): ByteArray? {
        val builder = ProofOfProof.OperationState.newBuilder()
        builder.id = operationState.operationId
        builder.status = operationState.status.name
        builder.action = operationState.currentAction.name
        if (operationState.blockNumber != null) {
            builder.endorsedBlockNumber = operationState.blockNumber
        } else {
            builder.endorsedBlockNumber = -1
        }
        if (operationState.miningInstruction != null) {
            builder.miningInstructions = ProofOfProof.MiningInstruction.newBuilder()
                .setPublicationData(ByteString.copyFrom(operationState.miningInstruction.publicationData))
                .setEndorsedBlockHeader(ByteString.copyFrom(operationState.miningInstruction.endorsedBlockHeader))
                .setLastBitcoinBlock(ByteString.copyFrom(operationState.miningInstruction.lastBitcoinBlock))
                .setMinerAddress(ByteString.copyFrom(operationState.miningInstruction.minerAddress))
                .addAllBitcoinContextAtEndorsed(
                    operationState.miningInstruction.endorsedBlockContextHeaders.stream()
                        .map { bytes: ByteArray? -> ByteString.copyFrom(bytes) }
                        .collect(Collectors.toList())
                )
                .build()
        }
        if (operationState.transactionBytes != null) {
            builder.transaction = ByteString.copyFrom(operationState.transactionBytes)
            builder.bitcoinTxId = operationState.submittedTransactionId
        }
        if (operationState.bitcoinBlockHeaderOfProofBytes != null) {
            builder.blockOfProof = ByteString.copyFrom(operationState.bitcoinBlockHeaderOfProofBytes)
        }
        if (operationState.bitcoinContextBlocksBytes != null) {
            builder.addAllBitcoinContext(
                operationState.bitcoinContextBlocksBytes
                    .stream()
                    .map { bytes: ByteArray? -> ByteString.copyFrom(bytes) }
                    .collect(Collectors.toList())
            )
        }
        if (operationState.merklePath != null) {
            builder.merklePath = operationState.merklePath
        }
        if (operationState.popTransactionId != null) {
            builder.popTxId = operationState.popTransactionId
        }
        if (operationState.message != null) {
            builder.message = operationState.message
        }
        if (operationState.transactionStatus != null) {
            builder.transactionStatus = operationState.transactionStatus.name
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

    private fun reconstitute(state: ProofOfProof.OperationState): PoPMiningOperationState {
        logger.info("Reconstituting operation {}", state.id)
        val builder = PoPMiningOperationState.newBuilder()
            .setOperationId(state.id)
            .parseStatus(state.status)
            .parseCurrentAction(state.action)
        if (state.endorsedBlockNumber >= 0) {
            builder.setBlockNumber(state.endorsedBlockNumber)
        }
        if (state.miningInstructions != null) {
            val miningInstruction = PoPMiningInstruction()
            miningInstruction.publicationData = state.miningInstructions.publicationData.toByteArray()
            miningInstruction.endorsedBlockHeader = state.miningInstructions.endorsedBlockHeader.toByteArray()
            miningInstruction.lastBitcoinBlock = state.miningInstructions.lastBitcoinBlock.toByteArray()
            miningInstruction.minerAddress = state.miningInstructions.minerAddress.toByteArray()
            miningInstruction.endorsedBlockContextHeaders = state.miningInstructions
                .bitcoinContextAtEndorsedList
                .stream()
                .map { obj: ByteString -> obj.toByteArray() }
                .collect(Collectors.toList())
            builder.setMiningInstruction(miningInstruction)
        }
        if (state.transaction != null && state.transaction.size() > 0) {
            builder.setTransaction(state.transaction.toByteArray())
            builder.setSubmittedTransactionId(state.bitcoinTxId)
        }
        if (state.blockOfProof != null && state.blockOfProof.size() > 0) {
            builder.setBitcoinBlockHeaderOfProof(state.blockOfProof.toByteArray())
        }
        if (state.bitcoinContextList != null && state.bitcoinContextCount > 0) {
            builder.setBitcoinContextBlocks(
                state.bitcoinContextList.stream().map { obj: ByteString -> obj.toByteArray() }.collect(Collectors.toList())
            )
        }
        if (state.merklePath != null) {
            builder.setMerklePath(state.merklePath)
        }
        if (state.popTxId != null) {
            builder.setPopTransactionId(state.popTxId)
        }
        if (state.message != null) {
            builder.setMessage(state.message)
        }
        if (state.transactionStatus != null) {
            builder.parseTransactionStatus(state.transactionStatus)
        }
        return builder.build()
    }
}
