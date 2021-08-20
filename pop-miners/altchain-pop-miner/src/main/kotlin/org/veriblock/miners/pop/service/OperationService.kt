// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.service

import kotlinx.serialization.protobuf.ProtoBuf
import org.veriblock.core.crypto.VbkTxId
import org.veriblock.core.utilities.createLogger
import org.veriblock.core.utilities.debugError
import org.veriblock.miners.pop.proto.OperationProto
import org.veriblock.miners.pop.transactionmonitor.WalletTransaction
import org.veriblock.miners.pop.core.ApmOperation
import org.veriblock.miners.pop.core.MiningOperationStatus
import org.veriblock.miners.pop.core.parseOperationLogs
import org.veriblock.miners.pop.core.toJson
import org.veriblock.miners.pop.storage.ApmOperationRepository
import org.veriblock.miners.pop.storage.ApmOperationStateRecord

private val logger = createLogger {}

class OperationService(
    private val repository: ApmOperationRepository,
    private val operationSerializer: OperationSerializer
) {
    fun getOperation(id: String, txFactory: (VbkTxId) -> WalletTransaction): ApmOperation? {
        val operation = repository.getOperation(id)
            ?: return null
        return operation.deserializeOperation(txFactory)
    }

    fun getActiveOperations(txFactory: (VbkTxId) -> WalletTransaction): List<ApmOperation> {
        val activeOperations = repository.getActiveOperations()
        return activeOperations.mapNotNull {
            it.deserializeOperation(txFactory)
        }
    }

    fun getOperations(altchainKey: String?, state: MiningOperationStatus, limit: Int, offset: Int, txFactory: (VbkTxId) -> WalletTransaction): List<ApmOperation> {
        val operations = repository.getOperations(altchainKey, state, limit, offset)
        return operations.mapNotNull {
            it.deserializeOperation(txFactory)
        }
    }

    fun getOperationsCount(altchainKey: String?, state: MiningOperationStatus): Int {
        return repository.getOperationsCount(altchainKey, state)
    }

    fun storeOperation(operation: ApmOperation) {
        val serialized = operationSerializer.serialize(operation)
        repository.saveOperationState(
            ApmOperationStateRecord(
                operation.id,
                operation.chain.key,
                operation.state.id,
                ProtoBuf.encodeToByteArray(OperationProto.Operation.serializer(), serialized),
                operation.createdAt,
                operation.getLogs().toJson()
            )
        )
    }

    private fun ApmOperationStateRecord.deserializeOperation(txFactory: (VbkTxId) -> WalletTransaction): ApmOperation? {
        val protoData = ProtoBuf.decodeFromByteArray(OperationProto.Operation.serializer(), state)
        return try {
            operationSerializer.deserialize(protoData, createdAt, logs.parseOperationLogs(), txFactory)
        } catch (exception: Exception) {
            logger.debugError(exception) { "Unable to deserialize the operation" }
            null
        }
    }
}
