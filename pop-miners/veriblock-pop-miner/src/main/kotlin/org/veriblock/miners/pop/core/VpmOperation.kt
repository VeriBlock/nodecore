// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.core

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.bitcoinj.core.TransactionConfidence
import org.veriblock.core.utilities.extensions.formatAtomicLongWithDecimal
import org.veriblock.core.utilities.extensions.toHex
import org.veriblock.miners.pop.EventBus
import org.veriblock.miners.pop.common.generateOperationId
import org.veriblock.miners.pop.model.PopMiningInstruction
import org.veriblock.miners.pop.service.Metrics
import java.time.LocalDateTime

class VpmOperation(
    id: String = generateOperationId(),
    endorsedBlockHeight: Int? = null,
    createdAt: LocalDateTime = LocalDateTime.now(),
    logs: List<OperationLog> = emptyList(),
    reconstituting: Boolean = false
) : MiningOperation(
    id, endorsedBlockHeight, createdAt, logs, reconstituting
) {
    var miningInstruction: PopMiningInstruction? = null
        private set
    var endorsementTransaction: VpmSpTransaction? = null
        private set
    var blockOfProof: VpmSpBlock? = null
        private set
    var merklePath: VpmMerklePath? = null
        private set
    var context: VpmContext? = null
        private set
    var proofOfProofId: String? = null
        private set
    var payoutBlockHash: String? = null
        private set
    var payoutAmount: Long? = null
        private set

    init {
        setState(VpmOperationState.INITIAL)
    }

    val transactionConfidenceEventChannel = BroadcastChannel<TransactionConfidence.ConfidenceType>(Channel.CONFLATED)

    val transactionListener = { confidence: TransactionConfidence, reason: TransactionConfidence.Listener.ChangeReason ->
        if (reason == TransactionConfidence.Listener.ChangeReason.TYPE) {
            transactionConfidenceEventChannel.offer(confidence.confidenceType)
        }
    }

    override fun onStateChanged() {
        EventBus.popMiningOperationStateChangedEvent.trigger(this)
    }
    
    fun setMiningInstruction(miningInstruction: PopMiningInstruction) {
        endorsedBlockHeight = miningInstruction.endorsedBlockHeight
        this.miningInstruction = miningInstruction
        setState(VpmOperationState.INSTRUCTION)
    }

    fun setTransaction(transaction: VpmSpTransaction) {
        if (state != VpmOperationState.INSTRUCTION) {
            error("Trying to set transaction without having the mining instruction")
        }
        endorsementTransaction = transaction
        setState(VpmOperationState.ENDORSEMENT_TRANSACTION)

        transaction.transaction.confidence.addEventListener(transactionListener)
        GlobalScope.launch {
            for (confidenceType in transactionConfidenceEventChannel.openSubscription()) {
                if (confidenceType == TransactionConfidence.ConfidenceType.PENDING) {
                    EventBus.transactionSufferedReorgEvent.trigger(this@VpmOperation)
                    // Reset the state to the endorsement transaction pending for confirmation
                    setState(VpmOperationState.ENDORSEMENT_TRANSACTION)
                }
            }
        }
    }

    fun setConfirmed() {
        if (state != VpmOperationState.ENDORSEMENT_TRANSACTION) {
            error("Trying to set as transaction confirmed without such transaction")
        }
        setState(VpmOperationState.CONFIRMED)

        endorsementTransaction?.let {
            Metrics.spentFeesCounter.increment(it.fee.toDouble())
        }
    }

    fun setBlockOfProof(blockOfProof: VpmSpBlock) {
        if (state != VpmOperationState.CONFIRMED) {
            error("Trying to set block of proof without having confirmed the transaction")
        }
        this.blockOfProof = blockOfProof
        setState(VpmOperationState.BLOCK_OF_PROOF)
    }

    fun setMerklePath(merklePath: VpmMerklePath) {
        if (state != VpmOperationState.BLOCK_OF_PROOF) {
            error("Trying to set merkle path without the block of proof")
        }
        this.merklePath = merklePath
        setState(VpmOperationState.PROVEN)
    }

    fun setContext(context: VpmContext) {
        if (state != VpmOperationState.PROVEN) {
            error("Trying to set context without the merkle path")
        }
        this.context = context
        setState(VpmOperationState.CONTEXT)
    }

    fun setProofOfProofId(proofOfProofId: String) {
        if (state != VpmOperationState.CONTEXT) {
            error("Trying to set Proof of Proof id without having the context")
        }
        this.proofOfProofId = proofOfProofId
        setState(VpmOperationState.SUBMITTED_POP_DATA)
    }

    fun setPayoutData(payoutBlockHash: String, payoutAmount: Long) {
        if (state != VpmOperationState.SUBMITTED_POP_DATA) {
            error("Trying to set Payout Data without having the Proof of Proof id")
        }
        this.payoutBlockHash = payoutBlockHash
        this.payoutAmount = payoutAmount
        setState(VpmOperationState.PAYOUT_DETECTED)

        Metrics.miningRewardCounter.increment(payoutAmount.toDouble())
    }

    override fun onCompleted() {
        EventBus.popMiningOperationCompletedEvent.trigger(id)
        EventBus.popMiningOperationFinishedEvent.trigger(this)
    }

    override fun onFailed() {
        EventBus.popMiningOperationFinishedEvent.trigger(this)
    }

    override fun getDetailedInfo(): Map<String, String> {
        val result = LinkedHashMap<String, String>()
        miningInstruction?.let {
            result["publicationData"] = it.publicationData.toHex()
            result["endorsedBlockHeader"] = it.endorsedBlockHeader.toHex()
            result["lastBitcoinBlock"] = it.lastBitcoinBlock.toHex()
            result["minerAddressBytes"] = it.minerAddressBytes.toHex()
            result["vbkEndorsedBlockContextHeaders"] = it.endorsedBlockContextHeaders.joinToString { it.toHex() }
        }
        endorsementTransaction?.let {
            result["btcEndorsementTransactionId"] = it.txId
            result["btcEndorsementTransactionFee"] = it.fee.formatAtomicLongWithDecimal()
        }
        blockOfProof?.let {
            result["btcBlockOfProof"] = it.hash
        }
        merklePath?.let {
            result["merklePath"] = it.compactFormat
        }
        context?.let {
            result["btcContextBlocks"] = it.blocks.joinToString { it.hashAsString }
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
