// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.core

import org.veriblock.core.utilities.extensions.formatAtomicLongWithDecimal
import org.veriblock.core.utilities.extensions.toHex
import org.veriblock.miners.pop.transactionmonitor.WalletTransaction
import org.veriblock.miners.pop.EventBus
import org.veriblock.miners.pop.securityinheriting.SecurityInheritingMonitor
import org.veriblock.miners.pop.service.Metrics
import org.veriblock.sdk.alt.ApmInstruction
import org.veriblock.sdk.alt.SecurityInheritingChain
import org.veriblock.sdk.alt.model.SecurityInheritingBlock
import org.veriblock.sdk.models.Coin
import org.veriblock.sdk.models.VeriBlockBlock
import org.veriblock.sdk.models.VeriBlockMerklePath
import org.veriblock.sdk.services.SerializeDeserializeService
import java.time.LocalDateTime
import java.util.UUID
import org.veriblock.core.utilities.Utility

class ApmOperation(
    id: String? = null,
    val chain: SecurityInheritingChain,
    val chainMonitor: SecurityInheritingMonitor,
    endorsedBlockHeight: Int? = null,
    createdAt: LocalDateTime = LocalDateTime.now(),
    logs: List<OperationLog> = emptyList(),
    reconstituting: Boolean = false
) : MiningOperation(
    id ?: chain.key + UUID.randomUUID().toString().substring(0, 8),
    endorsedBlockHeight,
    createdAt,
    logs, reconstituting
) {
    var miningInstruction: ApmInstruction? = null
        private set
    var endorsementTransaction: ApmSpTransaction? = null
        private set
    var blockOfProof: VeriBlockBlock? = null
        private set
    var merklePath: VeriBlockMerklePath? = null
        private set
    var atvId: String? = null
        private set
    var payoutBlockHash: String? = null
        private set
    var payoutAmount: Long? = null
        private set

    // (mem-only) should NOT be serialized
    var requiredConfirmations: Int? = null
    // (mem-only) should NOT be serialized
    var currentConfirmations: Int? = null
    // (mem-only) should NOT be serialized
    var atvBlock: SecurityInheritingBlock? = null

    init {
        setState(ApmOperationState.INITIAL)
    }

    override fun onStateChanged() {
        super.onStateChanged()
        EventBus.operationStateChangedEvent.trigger(this)
    }

    override fun onCompleted() {
        super.onCompleted()
        EventBus.operationFinishedEvent.trigger(this)
    }

    override fun onFailed(originalState: MiningOperationState) {
        super.onFailed(originalState)
        EventBus.operationFinishedEvent.trigger(this)
    }

    fun atvReorganized() {
        this.requiredConfirmations = null
        this.currentConfirmations = null
        this.atvBlock = null
    }

    fun setMiningInstruction(miningInstruction: ApmInstruction) {
        endorsedBlockHeight = miningInstruction.endorsedBlockHeight
        this.miningInstruction = miningInstruction
        setState(ApmOperationState.INSTRUCTION)
    }

    fun setTransaction(transaction: ApmSpTransaction) {
        if (state != ApmOperationState.INSTRUCTION) {
            error("Trying to set transaction without having the mining instruction")
        }
        endorsementTransaction = transaction
        setState(ApmOperationState.ENDORSEMENT_TRANSACTION)
    }

    fun setConfirmed() {
        if (state != ApmOperationState.ENDORSEMENT_TRANSACTION) {
            error("Trying to set as transaction confirmed without such transaction")
        }
        setState(ApmOperationState.ENDORSEMENT_TX_CONFIRMED)

        endorsementTransaction?.let {
            Metrics.spentFeesCounter.increment(it.fee.toDouble())
        }
    }

    fun setBlockOfProof(blockOfProof: VeriBlockBlock) {
        if (state != ApmOperationState.ENDORSEMENT_TX_CONFIRMED) {
            error("Trying to set block of proof without having confirmed the transaction")
        }
        this.blockOfProof = blockOfProof
        setState(ApmOperationState.BLOCK_OF_PROOF)
    }

    fun setMerklePath(merklePath: VeriBlockMerklePath) {
        if (state != ApmOperationState.BLOCK_OF_PROOF) {
            error("Trying to set merkle path without the block of proof")
        }
        this.merklePath = merklePath
        setState(ApmOperationState.PROVEN)
    }

    fun setAtvId(atvId: String) {
        if (state != ApmOperationState.PROVEN) {
            error("Trying to set ATV id without having proven the transaction")
        }
        this.atvId = atvId
        setState(ApmOperationState.SUBMITTED_POP_DATA)
    }

    fun setPayoutData(payoutBlockHash: String, payoutAmount: Long) {
        if (state != ApmOperationState.SUBMITTED_POP_DATA) {
            error("Trying to set Payout Data without having the Proof of Proof id")
        }
        this.payoutBlockHash = payoutBlockHash
        this.payoutAmount = payoutAmount
        setState(ApmOperationState.PAYOUT_DETECTED)

        Metrics.miningRewardCounter.increment(payoutAmount.toDouble())
    }

    override fun getDetailedInfo(): Map<String, String> {
        val result = LinkedHashMap<String, String>()
        endorsedBlockHeight?.let {
            result["expectedRewardBlock"] = (it + chain.getPayoutDelay()).toString()
        }
        miningInstruction?.let { instruction ->
            result["chainIdentifier"] = instruction.publicationData.identifier.toString()
            result["endorsedBlockHeight"] = instruction.endorsedBlockHeight.toString()
            result["publicationDataHeader"] = instruction.publicationData.header.toHex()
            result["publicationDataContextInfo"] = instruction.publicationData.contextInfo.toHex()
            result["publicationDataPayoutInfo"] = instruction.publicationData.payoutInfo.toHex()
            result["publicationDataPayoutInfoDisplay"] = chain.extractAddressDisplay(instruction.publicationData.payoutInfo)
            result["vbkContextBlockHashes"] = instruction.context.joinToString { it.toHex() }
            result["btcContextBlockHashes"] = instruction.btcContext.joinToString { it.toHex() }
        }
        endorsementTransaction?.let {
            result["vbkEndorsementTxId"] = it.txId
            result["vbkEndorsementTxFee"] = it.fee.formatAtomicLongWithDecimal()
            result["vbkEndorsementTxFeePerByte"] = it.feePerByte.toString()
        }
        blockOfProof?.let {
            result["vbkBlockOfProof"] = it.hash.toString()
            result["vbkBlockOfProofHeight"] = it.height.toString()
        }
        merklePath?.let {
            result["merklePath"] = it.toCompactString()
        }
        atvId?.let {
            result["altAtvId"] = it
        }
        atvBlock?.let {
            result["altAtvBlock"] = "${it.hash} @ ${it.height}"
            result["altAtvCurrentConfirmations"] = currentConfirmations.toString()
            result["altAtvRequiredConfirmations"] = requiredConfirmations.toString()
        }
        payoutBlockHash?.let {
            result["payoutBlockHash"] = it
        }
        payoutAmount?.let {
            result["payoutAmount"] = it.toString()
        }
        failureReason?.let {
            result["failureReason"] = it
        }
        return result
    }
}

class ApmSpTransaction(
    val transaction: WalletTransaction
) {
    val txId: String get() = transaction.id.bytes.toHex()
    // sourceAmount - sum(outputs)
    val fee: Long get() = transaction.sourceAmount.subtract(
        transaction.outputs.asSequence().map {
            it.amount
        }.fold(Coin.ZERO) { total, out ->
            total.add(out)
        }
    ).atomicUnits

    val feePerByte: Long get() {
        val publicationDataSize = transaction.publicationData?.let { publicationData ->
            SerializeDeserializeService.serialize(publicationData)
        }?.size ?: 0
        val transactionSize = predictAltChainEndorsementTransactionSize(
            dataLength = publicationDataSize,
            sigIndex = transaction.signatureIndex
        )
        return fee / transactionSize
    }
}

private fun predictAltChainEndorsementTransactionSize(dataLength: Int, sigIndex: Long): Int {
    var totalSize = 0

    // Using an estimated total fee of 1 VBK
    val inputAmount = 100000000L
    totalSize += 1 // Transaction Version
    totalSize += 1 // Type of Input Address
    totalSize += 1 // Standard Input Address Length Byte
    totalSize += 22 // Standard Input Address Length
    val inputAmountBytes = Utility.trimmedByteArrayFromLong(inputAmount)
    val inputAmountLength = inputAmountBytes.size.toLong()
    totalSize += 1 // Input Amount Length Byte
    totalSize += inputAmountLength.toInt() // Input Amount Length
    totalSize += 1 // Number of Outputs, will be 0
    val sigIndexBytes = Utility.trimmedByteArrayFromLong(sigIndex)
    totalSize += 1 // Sig Index Length Bytes
    totalSize += sigIndexBytes.size // Sig Index Bytes
    val dataSizeBytes = Utility.trimmedByteArrayFromInteger(dataLength)
    totalSize += 1 // Data Length Bytes Length
    totalSize += dataSizeBytes.size // Data Length Bytes (value will be 0)
    totalSize += dataLength
    return totalSize
}
