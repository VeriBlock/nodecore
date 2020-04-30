// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.miners.pop.service

import kotlinx.serialization.protobuf.ProtoBuf
import org.bitcoinj.core.Transaction
import org.veriblock.core.utilities.createLogger
import org.veriblock.miners.pop.EventBus
import org.veriblock.miners.pop.core.OperationState
import org.veriblock.miners.pop.core.VpmContext
import org.veriblock.miners.pop.core.VpmMerklePath
import org.veriblock.miners.pop.core.VpmOperation
import org.veriblock.miners.pop.core.VpmSpBlock
import org.veriblock.miners.pop.core.VpmSpTransaction
import org.veriblock.miners.pop.core.debug
import org.veriblock.miners.pop.core.parseOperationLogs
import org.veriblock.miners.pop.core.toJson
import org.veriblock.miners.pop.model.PopMiningInstruction
import org.veriblock.miners.pop.model.proto.OperationProto
import org.veriblock.miners.pop.storage.OperationRepository
import org.veriblock.miners.pop.storage.OperationStateRecord
import java.util.ArrayList

private val logger = createLogger {}

class PopStateService(
    private val repository: OperationRepository,
    private val bitcoinService: BitcoinService
) {
    init {
        EventBus.popMiningOperationStateChangedEvent.register(this, ::onMiningOperationStateChanged)
    }

    fun getActiveOperations(): List<VpmOperation> {
        return repository.getActiveOperations().map {
            reconstitute(it)
        }
    }

    fun getOperation(id: String): VpmOperation? {
        val stateData = repository.getOperation(id)
            ?: return null
        try {
            return reconstitute(stateData)
        } catch (e: Exception) {
            logger.error(e.message, e)
        }
        return null
    }

    private fun onMiningOperationStateChanged(operation: VpmOperation) {
        try {
            val serializedState = serialize(operation)
            val stateData = OperationStateRecord(
                id = operation.id,
                status = operation.state.id,
                state = serializedState,
                createdAt = operation.createdAt,
                logs = operation.getLogs().toJson()
            )
            repository.saveOperationState(stateData)
        } catch (e: Exception) {
            logger.error(e.message, e)
        }
    }

    private fun serialize(operation: VpmOperation): ByteArray {
        val protoData = OperationProto.Operation(
            id = operation.id,
            state = operation.state.name,
            action = operation.state.description,
            endorsedBlockNumber = operation.endorsedBlockHeight ?: -1,
            miningInstruction = operation.miningInstruction?.let {
                OperationProto.MiningInstruction(
                    it.publicationData,
                    it.endorsedBlockHeader,
                    it.lastBitcoinBlock,
                    it.minerAddressBytes,
                    it.endorsedBlockContextHeaders
                )
            } ?: OperationProto.MiningInstruction(),
            transaction = operation.endorsementTransaction?.transactionBytes ?: ByteArray(0),
            bitcoinTxId = operation.endorsementTransaction?.txId ?: "",
            blockOfProof = operation.blockOfProof?.block?.bitcoinSerialize() ?: ByteArray(0),
            merklePath = operation.merklePath?.compactFormat ?: "",
            bitcoinContext = operation.context?.blocks?.map { it.bitcoinSerialize() } ?: emptyList(),
            popTxId = operation.proofOfProofId ?: "",
            payoutBlockHash = operation.payoutBlockHash ?: "",
            payoutAmount = operation.payoutAmount ?: 0L
        )
        return ProtoBuf.dump(OperationProto.Operation.serializer(), protoData)
    }

    private fun reconstitute(record: OperationStateRecord): VpmOperation {
        val operation = VpmOperation(
            id = record.id,
            createdAt = record.createdAt,
            logs = record.logs.parseOperationLogs(),
            reconstituting = true
        )
        val protoData = ProtoBuf.load(OperationProto.Operation.serializer(), record.state)
        logger.debug("Reconstituting operation {}", protoData.id)
        val state = OperationState.valueOf(protoData.state)
        if (protoData.endorsedBlockNumber >= 0) {
            operation.endorsedBlockHeight = protoData.endorsedBlockNumber
        }
        if (protoData.miningInstruction.publicationData.isNotEmpty()) {
            val miningInstruction = PopMiningInstruction(
                publicationData = protoData.miningInstruction.publicationData,
                endorsedBlockHeader = protoData.miningInstruction.endorsedBlockHeader,
                lastBitcoinBlock = protoData.miningInstruction.lastBitcoinBlock,
                minerAddressBytes = protoData.miningInstruction.minerAddress,
                endorsedBlockContextHeaders = protoData.miningInstruction.bitcoinContextAtEndorsed
            )
            operation.setMiningInstruction(miningInstruction)
        }
        if (protoData.transaction.isNotEmpty()) {
            logger.debug(operation, "Rebuilding transaction")
            val transaction: Transaction = bitcoinService.makeTransaction(protoData.transaction)
            operation.setTransaction(VpmSpTransaction(transaction, protoData.transaction))
            logger.debug(operation, "Rebuilt transaction ${transaction.txId}")
        }
        if (protoData.blockOfProof.isNotEmpty()) {
            operation.setConfirmed()
            logger.debug(operation, "Rebuilding block of proof")
            val block = bitcoinService.makeBlock(protoData.blockOfProof)
            operation.setBlockOfProof(VpmSpBlock(block))
            logger.debug(operation, "Reattached block of proof ${block.hashAsString}")
        }
        if (protoData.merklePath.isNotEmpty()) {
            operation.setMerklePath(VpmMerklePath(protoData.merklePath))
        }
        if (protoData.bitcoinContext.isNotEmpty()) {
            logger.debug(operation, "Rebuilding context blocks")
            val contextBytes = protoData.bitcoinContext
            val blocks = bitcoinService.makeBlocks(contextBytes)
            operation.setContext(VpmContext(ArrayList(blocks)))
        } else if (protoData.popTxId.isNotEmpty()) {
            operation.setContext(VpmContext())
        }
        if (protoData.popTxId.isNotEmpty()) {
            operation.setProofOfProofId(protoData.popTxId)
        }
        if (protoData.payoutBlockHash.isNotEmpty()) {
            operation.complete(protoData.payoutBlockHash, protoData.payoutAmount)
        }
        if (state == OperationState.FAILED) {
            operation.fail("Loaded as failed")
        }
        operation.reconstituting = false
        return operation
    }
}
