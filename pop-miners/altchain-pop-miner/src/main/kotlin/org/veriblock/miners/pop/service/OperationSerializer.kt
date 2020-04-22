// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.service

import org.veriblock.lite.proto.OperationProto
import org.veriblock.lite.transactionmonitor.WalletTransaction
import org.veriblock.miners.pop.core.ApmContext
import org.veriblock.miners.pop.core.ApmMerklePath
import org.veriblock.miners.pop.core.ApmOperation
import org.veriblock.miners.pop.core.ApmSpBlock
import org.veriblock.miners.pop.core.ApmSpTransaction
import org.veriblock.miners.pop.core.OperationLog
import org.veriblock.miners.pop.core.OperationState
import org.veriblock.miners.pop.securityinheriting.SecurityInheritingService
import org.veriblock.sdk.alt.ApmInstruction
import org.veriblock.sdk.alt.plugin.PluginService
import org.veriblock.sdk.models.Address
import org.veriblock.sdk.models.BitcoinBlock
import org.veriblock.sdk.models.BitcoinTransaction
import org.veriblock.sdk.models.MerklePath
import org.veriblock.sdk.models.PublicationData
import org.veriblock.sdk.models.VeriBlockBlock
import org.veriblock.sdk.models.VeriBlockMerklePath
import org.veriblock.sdk.models.VeriBlockPoPTransaction
import org.veriblock.sdk.models.VeriBlockPublication
import org.veriblock.sdk.services.SerializeDeserializeService
import java.time.LocalDateTime
import java.util.ArrayList

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
            publicationData = operation.miningInstruction?.let {
                serialize(it.publicationData)
            } ?: OperationProto.PublicationData(),
            publicationContext = operation.miningInstruction?.context ?: emptyList(),
            publicationBtcContext = operation.miningInstruction?.btcContext ?: emptyList(),
            txId = operation.endorsementTransaction?.txId ?: "",
            blockOfProof = operation.blockOfProof?.let {
                SerializeDeserializeService.serializeHeaders(it.block)
            } ?: ByteArray(0),
            merklePath = operation.merklePath?.compactFormat ?: "",
            veriblockPublications = operation.context?.publications?.map {
                serialize(it)
            } ?: emptyList(),
            proofOfProofId = operation.proofOfProofId ?: "",
            payoutBlockHash = operation.payoutBlockHash ?: "",
            payoutAmount = operation.payoutAmount ?: 0L
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
            if (serialized.publicationData.header.isNotEmpty()) {
                setMiningInstruction(
                    ApmInstruction(
                        serialized.blockHeight,
                        deserialize(serialized.publicationData),
                        serialized.publicationContext,
                        serialized.publicationBtcContext
                    ))
            }

            if (serialized.txId.isNotEmpty()) {
                try {
                    setTransaction(ApmSpTransaction(txFactory(serialized.txId)))
                } catch (e: IllegalStateException) {
                    fail(e.message ?: "Unable to load VBK transaction ${serialized.txId}")
                }
            }

            if (serialized.blockOfProof.isNotEmpty()) {
                setConfirmed()
                setBlockOfProof(ApmSpBlock(SerializeDeserializeService.parseVeriBlockBlock(serialized.blockOfProof)))
            }

            if (serialized.merklePath.isNotEmpty()) {
                setMerklePath(ApmMerklePath(VeriBlockMerklePath(serialized.merklePath)))
            }

            if (serialized.veriblockPublications.isNotEmpty()) {
                setContext(ApmContext(serialized.veriblockPublications.map {
                    deserialize(it)
                }))
            }

            if (serialized.proofOfProofId.isNotEmpty()) {
                setProofOfProofId(serialized.proofOfProofId)
            }

            if (serialized.payoutBlockHash.isNotEmpty()) {
                complete(serialized.payoutBlockHash, serialized.payoutAmount)
            }

            if (state == OperationState.FAILED) {
                fail("Loaded as failed")
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

    private fun serialize(publication: VeriBlockPublication): OperationProto.VeriBlockPublication {
        return OperationProto.VeriBlockPublication(
            transaction = serialize(publication.transaction),
            merklePath = publication.merklePath.toCompactString(),
            containingBlock = SerializeDeserializeService.serializeHeaders(publication.containingBlock),
            context = publication.context.map { SerializeDeserializeService.serializeHeaders(it) }
        )
    }

    private fun serialize(tx: VeriBlockPoPTransaction): OperationProto.PopTransaction {
        return OperationProto.PopTransaction(
            txId = tx.id?.toString() ?: "",
            address = tx.address?.toString() ?: "",
            publishedBlock = SerializeDeserializeService.serializeHeaders(tx.publishedBlock),
            bitcoinTx = tx.bitcoinTransaction.rawBytes,
            merklePath = tx.merklePath.toCompactString(),
            blockOfProof = SerializeDeserializeService.getHeaderBytesBitcoinBlock(tx.blockOfProof),
            bitcoinContext = tx.blockOfProofContext.map {
                SerializeDeserializeService.getHeaderBytesBitcoinBlock(it)
            },
            signature = tx.signature,
            publicKey = tx.publicKey,
            networkByte = tx.networkByte?.toInt() ?: 0
        )
    }

    private fun deserialize(serialized: OperationProto.PopTransaction): VeriBlockPoPTransaction {
        val context = ArrayList<BitcoinBlock>(serialized.bitcoinContext.size)
        for (raw in serialized.bitcoinContext) {
            context.add(SerializeDeserializeService.parseBitcoinBlock(raw))
        }

        var networkByte: Byte? = null
        if (serialized.networkByte > 0) {
            networkByte = serialized.networkByte.toByte()
        }

        return VeriBlockPoPTransaction(
            Address(serialized.address),
            SerializeDeserializeService.parseVeriBlockBlock(serialized.publishedBlock),
            BitcoinTransaction(serialized.bitcoinTx),
            MerklePath(serialized.merklePath),
            SerializeDeserializeService.parseBitcoinBlock(serialized.blockOfProof),
            context,
            serialized.signature,
            serialized.publicKey,
            networkByte
        )
    }

    private fun deserialize(serialized: OperationProto.VeriBlockPublication): VeriBlockPublication {
        val context = ArrayList<VeriBlockBlock>(serialized.context.size)
        for (raw in serialized.context) {
            context.add(SerializeDeserializeService.parseVeriBlockBlock(raw))
        }

        return VeriBlockPublication(
            deserialize(serialized.transaction),
            VeriBlockMerklePath(serialized.merklePath),
            SerializeDeserializeService.parseVeriBlockBlock(serialized.containingBlock),
            context
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
