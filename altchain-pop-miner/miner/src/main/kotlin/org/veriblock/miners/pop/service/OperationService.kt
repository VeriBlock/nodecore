// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.service

import com.google.protobuf.InvalidProtocolBufferException
import org.veriblock.core.utilities.createLogger
import org.veriblock.lite.transactionmonitor.WalletTransaction
import org.veriblock.miners.pop.core.MiningOperation
import org.veriblock.miners.pop.storage.OperationRepository
import org.veriblock.miners.pop.storage.OperationStateData
import org.veriblock.miners.pop.storage.Pop
import org.veriblock.miners.pop.storage.StateSerializer
import java.util.*

private val logger = createLogger {}

class OperationService(
    private val repository: OperationRepository
) {
    fun getActiveOperations(txFactory: (String) -> WalletTransaction): List<MiningOperation> {
        val workflows = ArrayList<MiningOperation>()

        val activeOperations = repository.getActiveOperations()
        while (activeOperations.hasNext()) {
            val next = activeOperations.next()

            try {
                val serialized = Pop.WorkflowState.parseFrom(next.state)
                workflows.add(StateSerializer.deserialize(serialized, txFactory))
            } catch (e: InvalidProtocolBufferException) {
                logger.error("Unable to load saved workflow")
            } catch (e: Exception) {
                logger.error("Unable to construct saved workflow", e)
            }

        }

        return workflows
    }

    fun storeOperation(operation: MiningOperation) {
        val serialized = StateSerializer.serialize(operation)

        val entity = OperationStateData()
        entity.id = operation.id
        entity.status = operation.status.value
        entity.state = serialized.toByteArray()

        repository.saveOperationState(entity)
    }
}
