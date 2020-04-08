// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.service

import kotlinx.serialization.protobuf.ProtoBuf
import org.veriblock.lite.proto.OperationProto
import org.veriblock.lite.transactionmonitor.WalletTransaction
import org.veriblock.miners.pop.core.ApmOperation
import org.veriblock.miners.pop.core.parseOperationLogs
import org.veriblock.miners.pop.core.toJson
import org.veriblock.miners.pop.storage.OperationRepository
import org.veriblock.miners.pop.storage.OperationStateRecord

class OperationService(
    private val repository: OperationRepository,
    private val operationSerializer: OperationSerializer
) {
    fun getActiveOperations(txFactory: (String) -> WalletTransaction): List<ApmOperation> {
        val activeOperations = repository.getActiveOperations()
        return activeOperations.map {
            val protoData = ProtoBuf.load(OperationProto.Operation.serializer(), it.state)
            operationSerializer.deserialize(protoData, it.createdAt, it.logs.parseOperationLogs(), txFactory)
        }
    }

    fun storeOperation(operation: ApmOperation) {
        val serialized = operationSerializer.serialize(operation)
        repository.saveOperationState(
            OperationStateRecord(
                operation.id,
                operation.state.id,
                ProtoBuf.dump(OperationProto.Operation.serializer(), serialized),
                operation.createdAt,
                operation.getLogs().toJson()
            )
        )
    }
}
