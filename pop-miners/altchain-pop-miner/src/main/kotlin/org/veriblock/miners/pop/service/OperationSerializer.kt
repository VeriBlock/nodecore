// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.service

import org.veriblock.miners.pop.proto.OperationProto
import org.veriblock.miners.pop.transactionmonitor.WalletTransaction
import org.veriblock.miners.pop.core.ApmOperation
import org.veriblock.miners.pop.core.ApmSpTransaction
import org.veriblock.miners.pop.core.OperationLog
import org.veriblock.miners.pop.core.MiningOperationState
import org.veriblock.miners.pop.securityinheriting.SecurityInheritingService
import org.veriblock.sdk.alt.ApmInstruction
import org.veriblock.sdk.alt.plugin.PluginService
import org.veriblock.sdk.models.PublicationData
import org.veriblock.sdk.models.VeriBlockMerklePath
import org.veriblock.sdk.services.SerializeDeserializeService
import java.time.LocalDateTime

class OperationSerializer(
    private val pluginService: PluginService,
    private val securityInheritingService: SecurityInheritingService
) {
    fun serialize(operation: ApmOperation): OperationProto.Operation {
        val protoData = OperationProto.Operation(
            operationId = operation.id,
            chainId = operation.chain.key,
            state = operation.state.id,
            blockHeight = operation.endorsedBlockHeight ?: 0,
            miningInstruction = operation.miningInstruction?.let {
                serialize(it.publicationData)
            } ?: OperationProto.PublicationData(),
            publicationContext = operation.miningInstruction?.context ?: emptyList(),
            publicationBtcContext = operation.miningInstruction?.btcContext ?: emptyList(),
            txId = operation.endorsementTransaction?.txId ?: "",
            blockOfProof = operation.blockOfProof?.let {
                SerializeDeserializeService.serializeHeaders(it)
            } ?: ByteArray(0),
            merklePath = operation.merklePath?.toCompactString() ?: "",
            atvId = operation.atvId ?: "",
            atvBlockHash = operation.atvBlockHash ?: "",
            payoutBlockHash = operation.payoutBlockHash ?: "",
            payoutAmount = operation.payoutAmount ?: 0L,
            failureReason = operation.failureReason ?: ""
        )
        return protoData
    }

    fun deserialize(
        serialized: OperationProto.Operation,
        createdAt: LocalDateTime,
        logs: List<OperationLog>,
        txFactory: (String) -> WalletTransaction
    ): ApmOperation {
        val chain = pluginService[serialized.chainId]
            ?: error("Unable to load plugin ${serialized.chainId} for operation ${serialized.operationId}")
        val chainMonitor = securityInheritingService.getMonitor(serialized.chainId)
            ?: error("Unable to load monitor ${serialized.chainId} for operation ${serialized.operationId}")
        return ApmOperation(
            id = serialized.operationId,
            chain = chain,
            chainMonitor = chainMonitor,
            logs = logs,
            endorsedBlockHeight = serialized.blockHeight,
            createdAt = createdAt,
            reconstituting = true
        ).apply {
            if (serialized.miningInstruction.header.isNotEmpty()) {
                setMiningInstruction(
                    ApmInstruction(
                        serialized.blockHeight,
                        deserialize(serialized.miningInstruction),
                        serialized.publicationContext,
                        serialized.publicationBtcContext
                    )
                )
            }

            if (serialized.txId.isNotEmpty()) {
                try {
                    setTransaction(ApmSpTransaction(txFactory(serialized.txId)))
                } catch (e: IllegalStateException) {
                    fail(e.message ?: "Unable to load VBK transaction ${serialized.txId}")
                    reconstituting = false
                    return this
                }
            }

            if (serialized.blockOfProof.isNotEmpty()) {
                setConfirmed()
                setBlockOfProof(SerializeDeserializeService.parseVeriBlockBlock(serialized.blockOfProof))
            }

            if (serialized.merklePath.isNotEmpty()) {
                setMerklePath(VeriBlockMerklePath(serialized.merklePath))
            }

            if (serialized.atvId.isNotEmpty()) {
                setAtvId(serialized.atvId)
            }

            if (serialized.atvBlockHash.isNotEmpty()) {
                setAtvBlockHash(serialized.atvBlockHash)
            }

            if (serialized.payoutBlockHash.isNotEmpty()) {
                setPayoutData(serialized.payoutBlockHash, serialized.payoutAmount)
                complete()
            }

            if (serialized.state == MiningOperationState.FAILED.id) {
                failureReason = serialized.failureReason
            }

            reconstituting = false
        }
    }

    private fun serialize(data: PublicationData): OperationProto.PublicationData {
        return OperationProto.PublicationData(
            identifier = data.identifier,
            header = data.header,
            payoutInfo = data.payoutInfo,
            veriblockContext = data.contextInfo
        )
    }

    private fun deserialize(serialized: OperationProto.PublicationData): PublicationData {
        return PublicationData(
            serialized.identifier,
            serialized.header,
            serialized.payoutInfo,
            serialized.veriblockContext
        )
    }
}
