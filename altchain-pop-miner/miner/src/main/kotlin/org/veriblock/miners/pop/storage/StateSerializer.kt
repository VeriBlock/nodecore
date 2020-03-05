// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.storage

import com.google.protobuf.ByteString
import org.veriblock.lite.transactionmonitor.WalletTransaction
import org.veriblock.miners.pop.core.MiningOperation
import org.veriblock.miners.pop.core.OperationState
import org.veriblock.miners.pop.core.OperationStatus
import org.veriblock.miners.pop.core.StateChangeEvent
import org.veriblock.sdk.alt.PublicationDataWithContext
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
import java.util.ArrayList

object StateSerializer {
    fun serialize(operation: MiningOperation): Pop.WorkflowState {
        val builder = Pop.WorkflowState.newBuilder()

        builder.operationId = operation.id
        builder.chainId = operation.chainId
        builder.status = operation.status.value

        builder.blockHeight = operation.blockHeight ?: 0

        val state = operation.state.let {
            if (it !is OperationState.Failed) it else it.previous
        }
        if (state is OperationState.PublicationData) {
            builder.publicationData = serialize(state.publicationDataWithContext.publicationData)
            for (context in state.publicationDataWithContext.context) {
                builder.addPublicationContext(ByteString.copyFrom(context))
            }
            for (context in state.publicationDataWithContext.btcContext) {
                builder.addPublicationBtcContext(ByteString.copyFrom(context))
            }
        }

        if (state is OperationState.EndorsementTransaction) {
            builder.txId = state.transaction.id.toString()
        }

        if (state is OperationState.BlockOfProof) {
            builder.blockOfProof = ByteString.copyFrom(SerializeDeserializeService.serializeHeaders(state.blockOfProof))
        }

        if (state is OperationState.TransactionProved) {
            builder.merklePath = state.merklePath.toCompactString()
        }

        if (state is OperationState.KeystoneOfProof) {
            builder.keystoneOfProof = ByteString.copyFrom(SerializeDeserializeService.serializeHeaders(state.keystoneOfProof))
        }

        if (state is OperationState.VeriBlockPublications) {
            for (publication in state.veriBlockPublications) {
                builder.addVeriblockPublications(serialize(publication))
            }
        }

        if (state is OperationState.SubmittedPopData) {
            builder.proofOfProofId = state.proofOfProofId
        }

        if (state is OperationState.AltEndorsedBlockConfirmed) {
            builder.altEndorsementBlockHash = state.altEndorsementBlockHash
        }

        if (state is OperationState.Completed) {
            builder.payoutBlockHash = state.payoutBlockHash
            builder.payoutAmount = state.payoutAmount
        }

        val changeHistory = operation.getChangeHistory().toList()
        if (changeHistory.isNotEmpty()) {
            for (event in changeHistory) {
                builder.addChangeHistory(serialize(event))
            }
        }

        return builder.build()
    }

    fun deserialize(serialized: Pop.WorkflowState, txFactory: (String) -> WalletTransaction): MiningOperation {
        return MiningOperation(
            id = serialized.operationId,
            chainId = serialized.chainId,
            changeHistory = serialized.changeHistoryList.map {
                deserialize(it)
            },
            status = OperationStatus.parse(serialized.status),
            blockHeight = serialized.blockHeight
        ).apply {
            if (serialized.publicationData != null) {
                setPublicationDataWithContext(PublicationDataWithContext(
                    serialized.blockHeight,
                    deserialize(serialized.publicationData),
                    serialized.publicationContextList.map { it.toByteArray() },
                    serialized.publicationBtcContextList.map { it.toByteArray() }
                ))
            }

            if (serialized.txId != null && serialized.txId.isNotEmpty()) {
                try {
                    setTransaction(txFactory(serialized.txId))
                } catch (e: IllegalStateException) {
                    fail(e.message ?: "Unable to load VBK transaction ${serialized.txId}")
                }
            }

            if (serialized.blockOfProof != null && serialized.blockOfProof.size() > 0) {
                setConfirmed()
                setBlockOfProof(SerializeDeserializeService.parseVeriBlockBlock(serialized.blockOfProof.toByteArray()))
            }

            if (serialized.merklePath != null && serialized.merklePath.isNotEmpty()) {
                setMerklePath(VeriBlockMerklePath(serialized.merklePath))
            }

            if (serialized.keystoneOfProof != null && serialized.keystoneOfProof.size() > 0) {
                setKeystoneOfProof(SerializeDeserializeService.parseVeriBlockBlock(serialized.keystoneOfProof.toByteArray()))
            }

            if (serialized.veriblockPublicationsCount > 0) {
                setVeriBlockPublications(serialized.veriblockPublicationsList.map {
                    deserialize(it)
                })
            }

            if (serialized.proofOfProofId != null && serialized.proofOfProofId.isNotEmpty()) {
                setProofOfProofId(serialized.proofOfProofId)
            }

            if (serialized.altEndorsementBlockHash != null && serialized.altEndorsementBlockHash.isNotEmpty()) {
                setAltEndorsementTransactionConfirmed()
                setAltEndorsedBlockHash(serialized.altEndorsementBlockHash)
            }

            if (serialized.payoutBlockHash != null && serialized.payoutBlockHash.isNotEmpty()) {
                complete(serialized.payoutBlockHash, serialized.payoutAmount)
            }

            if (status == OperationStatus.FAILED) {
                fail("Loaded as failed")
            }
        }
    }

    private fun serialize(data: PublicationData): Pop.PublicationData {
        val builder = Pop.PublicationData.newBuilder()

        builder.identifier = data.identifier

        if (data.header != null) {
            builder.header = ByteString.copyFrom(data.header)
        }

        if (data.payoutInfo != null) {
            builder.payoutInfo = ByteString.copyFrom(data.payoutInfo)
        }

        if (data.contextInfo != null) {
            builder.veriblockContext = ByteString.copyFrom(data.contextInfo)
        }

        return builder.build()
    }

    private fun serialize(publication: VeriBlockPublication): Pop.VeriBlockPublication {
        val builder = Pop.VeriBlockPublication.newBuilder()

        if (publication.transaction != null) {
            builder.transaction = serialize(publication.transaction)
        }

        if (publication.merklePath != null) {
            builder.merklePath = publication.merklePath.toCompactString()
        }

        if (publication.containingBlock != null) {
            builder.containingBlock = ByteString.copyFrom(SerializeDeserializeService.serializeHeaders(publication.containingBlock))
        }

        val context = publication.context
        if (context != null && context.size > 0) {
            for (block in context) {
                val serializedBlock = SerializeDeserializeService.serializeHeaders(block)
                builder.addContext(ByteString.copyFrom(serializedBlock))
            }
        }

        return builder.build()
    }

    private fun serialize(tx: VeriBlockPoPTransaction): Pop.PoPTransaction {
        val builder = Pop.PoPTransaction.newBuilder()

        if (tx.id != null) {
            builder.txId = tx.id.toString()
        }

        if (tx.address != null) {
            builder.address = tx.address.toString()
        }

        if (tx.publishedBlock != null) {
            builder.publishedBlock = ByteString.copyFrom(SerializeDeserializeService.serializeHeaders(tx.publishedBlock))
        }

        if (tx.bitcoinTransaction != null) {
            builder.bitcoinTx = ByteString.copyFrom(tx.bitcoinTransaction.rawBytes)
        }

        if (tx.merklePath != null) {
            builder.merklePath = tx.merklePath.toCompactString()
        }

        if (tx.blockOfProof != null) {
            builder.blockOfProof = ByteString.copyFrom(SerializeDeserializeService.getHeaderBytesBitcoinBlock(tx.blockOfProof))
        }

        val blockOfProofContext = tx.blockOfProofContext
        if (blockOfProofContext != null && blockOfProofContext.size > 0) {
            for (block in blockOfProofContext) {
                builder.addBitcoinContext(ByteString.copyFrom(SerializeDeserializeService.getHeaderBytesBitcoinBlock(block)))
            }
        }

        if (tx.signature != null) {
            builder.signature = ByteString.copyFrom(tx.signature)
        }

        if (tx.publicKey != null) {
            builder.publicKey = ByteString.copyFrom(tx.publicKey)
        }

        if (tx.networkByte != null) {
            builder.networkByte = tx.networkByte!!.toInt()
        }

        return builder.build()
    }

    private fun serialize(event: StateChangeEvent): Pop.Event {
        val builder = Pop.Event.newBuilder()

        builder.change = event.state
        builder.timestamp = event.timestamp

        return builder.build()
    }

    private fun deserialize(serialized: Pop.Event): StateChangeEvent {
        return StateChangeEvent(serialized.change, serialized.timestamp)
    }

    private fun deserialize(serialized: Pop.PoPTransaction): VeriBlockPoPTransaction {
        val context = ArrayList<BitcoinBlock>(serialized.bitcoinContextCount)
        for (raw in serialized.bitcoinContextList) {
            context.add(SerializeDeserializeService.parseBitcoinBlock(raw.toByteArray()))
        }

        var networkByte: Byte? = null
        if (serialized.networkByte > 0) {
            networkByte = serialized.networkByte.toByte()
        }

        return VeriBlockPoPTransaction(
            Address(serialized.address),
            SerializeDeserializeService.parseVeriBlockBlock(serialized.publishedBlock.toByteArray()),
            BitcoinTransaction(serialized.bitcoinTx.toByteArray()),
            MerklePath(serialized.merklePath),
            SerializeDeserializeService.parseBitcoinBlock(serialized.blockOfProof.toByteArray()),
            context,
            serialized.signature.toByteArray(),
            serialized.publicKey.toByteArray(),
            networkByte
        )
    }

    private fun deserialize(serialized: Pop.VeriBlockPublication): VeriBlockPublication {
        val context = ArrayList<VeriBlockBlock>(serialized.contextCount)
        for (raw in serialized.contextList) {
            context.add(SerializeDeserializeService.parseVeriBlockBlock(raw.toByteArray()))
        }

        return VeriBlockPublication(
            deserialize(serialized.transaction),
            VeriBlockMerklePath(serialized.merklePath),
            SerializeDeserializeService.parseVeriBlockBlock(serialized.containingBlock.toByteArray()),
            context
        )
    }

    private fun deserialize(serialized: Pop.PublicationData): PublicationData {
        return PublicationData(
            serialized.identifier,
            serialized.header.toByteArray(),
            serialized.payoutInfo.toByteArray(),
            serialized.veriblockContext.toByteArray()
        )
    }
}
