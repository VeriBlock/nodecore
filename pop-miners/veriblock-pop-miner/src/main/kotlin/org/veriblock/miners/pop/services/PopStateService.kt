// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.miners.pop.services

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
import org.veriblock.miners.pop.model.PopMiningInstruction
import org.veriblock.miners.pop.proto.OperationProto
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
            reconstitute(ProtoBuf.load(OperationProto.Operation.serializer(), it.state))
        }
    }

    fun getOperation(id: String): VpmOperation? {
        val stateData = repository.getOperation(id)
            ?: return null
        try {
            return reconstitute(ProtoBuf.load(OperationProto.Operation.serializer(), stateData.state))
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
                state = serializedState
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
            payoutAmount = operation.payoutAmount ?: ""
        )
        return ProtoBuf.dump(OperationProto.Operation.serializer(), protoData)
    }

    private fun reconstitute(operation: OperationProto.Operation): VpmOperation {
        logger.debug("Reconstituting operation {}", operation.id)
        val state = OperationState.valueOf(operation.state)
        val miningOperation = VpmOperation(
            id = operation.id,
            reconstituting = true
        )
        if (operation.endorsedBlockNumber >= 0) {
            miningOperation.endorsedBlockHeight = operation.endorsedBlockNumber
        }
        if (operation.miningInstruction.publicationData.isNotEmpty()) {
            val miningInstruction = PopMiningInstruction(
                publicationData = operation.miningInstruction.publicationData,
                endorsedBlockHeader = operation.miningInstruction.endorsedBlockHeader,
                lastBitcoinBlock = operation.miningInstruction.lastBitcoinBlock,
                minerAddressBytes = operation.miningInstruction.minerAddress,
                endorsedBlockContextHeaders = operation.miningInstruction.bitcoinContextAtEndorsed
            )
            miningOperation.setMiningInstruction(miningInstruction)
        }
        if (operation.transaction.isNotEmpty()) {
            logger.debug(miningOperation) { "Rebuilding transaction" }
            val transaction: Transaction = bitcoinService.makeTransaction(operation.transaction)
            miningOperation.setTransaction(VpmSpTransaction(transaction, operation.transaction))
            logger.debug(miningOperation) { "Rebuilt transaction ${transaction.txId}" }
        }
        if (operation.blockOfProof.isNotEmpty()) {
            miningOperation.setConfirmed()
            logger.debug(miningOperation) { "Rebuilding block of proof" }
            val block = bitcoinService.makeBlock(operation.blockOfProof)
            miningOperation.setBlockOfProof(VpmSpBlock(block))
            logger.debug(miningOperation) { "Reattached block of proof ${block.hashAsString}" }
        }
        if (operation.merklePath.isNotEmpty()) {
            miningOperation.setMerklePath(VpmMerklePath(operation.merklePath))
        }
        if (operation.bitcoinContext.isNotEmpty()) {
            logger.debug(miningOperation) { "Rebuilding context blocks" }
            val contextBytes = operation.bitcoinContext
            val blocks = bitcoinService.makeBlocks(contextBytes)
            miningOperation.setContext(VpmContext(ArrayList(blocks)))
        } else if (operation.popTxId.isNotEmpty()) {
            miningOperation.setContext(VpmContext())
        }
        if (operation.popTxId.isNotEmpty()) {
            miningOperation.setProofOfProofId(operation.popTxId)
        }
        if (operation.payoutBlockHash.isNotEmpty()) {
            miningOperation.complete(operation.payoutBlockHash, operation.payoutAmount)
        }
        if (state == OperationState.FAILED) {
            miningOperation.fail("Loaded as failed")
        }
        miningOperation.reconstituting = false
        return miningOperation
    }
}
