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
import java.time.LocalDateTime

class VpmOperation(
    id: String = generateOperationId(),
    endorsedBlockHeight: Int? = null,
    createdAt: LocalDateTime = LocalDateTime.now(),
    logs: List<OperationLog> = emptyList(),
    reconstituting: Boolean = false
) : MiningOperation<PopMiningInstruction, VpmSpTransaction, VpmSpBlock, VpmMerklePath, VpmContext>(
    id, endorsedBlockHeight, createdAt, logs, reconstituting
) {
    val transactionConfidenceEventChannel = BroadcastChannel<TransactionConfidence.ConfidenceType>(Channel.CONFLATED)

    val transactionListener = { confidence: TransactionConfidence, reason: TransactionConfidence.Listener.ChangeReason ->
        if (reason == TransactionConfidence.Listener.ChangeReason.TYPE) {
            transactionConfidenceEventChannel.offer(confidence.confidenceType)
        }
    }

    override fun onStateChanged() {
        EventBus.popMiningOperationStateChangedEvent.trigger(this)
    }

    override fun onTransactionSet(transaction: VpmSpTransaction) {
        transaction.transaction.confidence.addEventListener(transactionListener)
        GlobalScope.launch {
            for (confidenceType in transactionConfidenceEventChannel.openSubscription()) {
                if (confidenceType == TransactionConfidence.ConfidenceType.PENDING) {
                    EventBus.transactionSufferedReorgEvent.trigger(this@VpmOperation)
                    // Reset the state to the endorsement transaction pending for confirmation
                    setState(OperationState.ENDORSEMENT_TRANSACTION)
                }
            }
        }
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
