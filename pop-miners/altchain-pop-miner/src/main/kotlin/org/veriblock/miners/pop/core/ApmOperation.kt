// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.core

import org.veriblock.core.utilities.extensions.formatAtomicLongWithDecimal
import org.veriblock.core.utilities.extensions.toHex
import org.veriblock.lite.core.AsyncEvent
import org.veriblock.lite.transactionmonitor.WalletTransaction
import org.veriblock.lite.util.Threading
import org.veriblock.miners.pop.securityinheriting.SecurityInheritingMonitor
import org.veriblock.miners.pop.service.Metrics
import org.veriblock.sdk.alt.ApmInstruction
import org.veriblock.sdk.alt.SecurityInheritingChain
import org.veriblock.sdk.models.Coin
import org.veriblock.sdk.models.VeriBlockBlock
import org.veriblock.sdk.models.VeriBlockMerklePath
import org.veriblock.sdk.models.VeriBlockPublication
import java.time.LocalDateTime
import java.util.UUID

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
    var blockOfProof: ApmSpBlock? = null
        private set
    var merklePath: ApmMerklePath? = null
        private set
    var context: ApmContext? = null
        private set
    var proofOfProofId: String? = null
        private set
    var payoutBlockHash: String? = null
        private set
    var payoutAmount: Long? = null
        private set

    val stateChangedEvent = AsyncEvent<ApmOperation>(Threading.MINER_THREAD)

    init {
        setState(ApmOperationState.INITIAL)
    }

    override fun onStateChanged() {
        stateChangedEvent.trigger(this)
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
        setState(ApmOperationState.CONFIRMED)

        endorsementTransaction?.let {
            Metrics.spentFeesCounter.increment(it.fee.toDouble())
        }
    }

    fun setBlockOfProof(blockOfProof: ApmSpBlock) {
        if (state != ApmOperationState.CONFIRMED) {
            error("Trying to set block of proof without having confirmed the transaction")
        }
        this.blockOfProof = blockOfProof
        setState(ApmOperationState.BLOCK_OF_PROOF)
    }

    fun setMerklePath(merklePath: ApmMerklePath) {
        if (state != ApmOperationState.BLOCK_OF_PROOF) {
            error("Trying to set merkle path without the block of proof")
        }
        this.merklePath = merklePath
        setState(ApmOperationState.PROVEN)
    }

    fun setContext(context: ApmContext) {
        if (state != ApmOperationState.PROVEN) {
            error("Trying to set context without the merkle path")
        }
        this.context = context
        setState(ApmOperationState.CONTEXT)
    }

    fun setProofOfProofId(proofOfProofId: String) {
        if (state != ApmOperationState.CONTEXT) {
            error("Trying to set Proof of Proof id without having the context")
        }
        this.proofOfProofId = proofOfProofId
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
        miningInstruction?.let { instruction ->
            result["chainIdentifier"] = instruction.publicationData.identifier.toString()
            result["publicationDataHeader"] = instruction.publicationData.header.toHex()
            result["publicationDataContextInfo"] = instruction.publicationData.contextInfo.toHex()
            result["publicationDataPayoutInfo"] = instruction.publicationData.payoutInfo.toHex()
            result["vbkContextBlockHashes"] = instruction.context.joinToString { it.toHex() }
            result["btcContextBlockHashes"] = instruction.btcContext.joinToString { it.toHex() }
        }
        endorsementTransaction?.let {
            result["vbkEndorsementTxId"] = it.txId
            result["vbkEndorsementTxFee"] = it.fee.formatAtomicLongWithDecimal()
        }
        blockOfProof?.let {
            result["vbkBlockOfProof"] = it.hash
        }
        merklePath?.let {
            result["merklePath"] = it.compactFormat
        }
        context?.let { context ->
            result["vtbTransactions"] = context.publications.joinToString { it.transaction.id.bytes.toHex() }
            result["vtbBtcBlocks"] = context.publications.joinToString { it.firstBitcoinBlock.hash.bytes.toHex() }
        }
        proofOfProofId?.let {
            result["proofOfProofId"] = it
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
}

class ApmSpBlock(
    val block: VeriBlockBlock
) {
    val hash: String get() = block.hash.bytes.toHex()
}

class ApmMerklePath(
    val merklePath: VeriBlockMerklePath
) {
    val compactFormat: String get() = merklePath.toCompactString()
}

class ApmContext(
    val publications: List<VeriBlockPublication>
)
