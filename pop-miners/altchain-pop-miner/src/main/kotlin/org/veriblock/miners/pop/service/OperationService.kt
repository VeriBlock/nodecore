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
import org.veriblock.miners.pop.proto.OperationProto
import org.veriblock.miners.pop.transactionmonitor.WalletTransaction
import org.veriblock.miners.pop.core.ApmOperation
import org.veriblock.miners.pop.core.MiningOperationStatus
import org.veriblock.miners.pop.core.parseOperationLogs
import org.veriblock.miners.pop.core.toJson
import org.veriblock.miners.pop.storage.OperationRepository
import org.veriblock.miners.pop.storage.OperationStateRecord

class OperationService(
    private val repository: OperationRepository,
    private val operationSerializer: OperationSerializer
) {
    fun getOperation(id: String, txFactory: (VbkTxId) -> WalletTransaction): ApmOperation? {
        val operation = repository.getOperation(id)
            ?: return null
        val protoData = ProtoBuf.decodeFromByteArray(OperationProto.Operation.serializer(), operation.state)
        return operationSerializer.deserialize(protoData, operation.createdAt, operation.logs.parseOperationLogs(), txFactory)
    }

    fun getActiveOperations(txFactory: (VbkTxId) -> WalletTransaction): List<ApmOperation> {
        val activeOperations = repository.getActiveOperations()
        return activeOperations.map {
            val protoData = ProtoBuf.decodeFromByteArray(OperationProto.Operation.serializer(), it.state)
            operationSerializer.deserialize(protoData, it.createdAt, it.logs.parseOperationLogs(), txFactory)
        }
    }

    fun getOperations(state: MiningOperationStatus, limit: Int, offset: Int, txFactory: (VbkTxId) -> WalletTransaction): List<ApmOperation> {
        val operations = repository.getOperations(state, limit, offset)
        return operations.map {
            val protoData = ProtoBuf.decodeFromByteArray(OperationProto.Operation.serializer(), it.state)
            operationSerializer.deserialize(protoData, it.createdAt, it.logs.parseOperationLogs(), txFactory)
        }
    }

    fun getOperationsCount(state: MiningOperationStatus): Int {
        return repository.getOperationsCount(state)
    }

    fun storeOperation(operation: ApmOperation) {
        val serialized = operationSerializer.serialize(operation)
        repository.saveOperationState(
            OperationStateRecord(
                operation.id,
                operation.state.id,
                ProtoBuf.encodeToByteArray(OperationProto.Operation.serializer(), serialized),
                operation.createdAt,
                operation.getLogs().toJson()
            )
        )
    }
}
