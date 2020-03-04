// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.core

import org.veriblock.core.utilities.extensions.toHex
import org.veriblock.lite.transactionmonitor.WalletTransaction
import org.veriblock.sdk.alt.PublicationDataWithContext
import org.veriblock.sdk.models.VeriBlockBlock
import org.veriblock.sdk.models.VeriBlockMerklePath
import org.veriblock.sdk.models.VeriBlockPublication


sealed class OperationState {

    open val transaction: WalletTransaction? get() = null

    open fun getDetailedInfo(): List<String> = emptyList()

    object Initial : OperationState() {
        override fun toString() = "Initial state, to be started"
    }

    open class PublicationData(
        val publicationDataWithContext: PublicationDataWithContext
    ) : OperationState() {
        override fun toString() = "Publication Data retrieved, Endorsement VBK Transaction to be submitted"
        override fun getDetailedInfo() = super.getDetailedInfo() +
            publicationDataWithContext.getDetailedInfo()
    }

    open class EndorsementTransaction(
        previous: PublicationData,
        override val transaction: WalletTransaction
    ) : PublicationData(previous.publicationDataWithContext) {
        override fun toString() = "Endorsement VBK Transaction submitted and to be confirmed"
        override fun getDetailedInfo() = super.getDetailedInfo() +
            "Endorsement Transaction: ${transaction.id.bytes.toHex()}"
    }

    open class Confirmed(
        previous: EndorsementTransaction
    ) : EndorsementTransaction(previous, previous.transaction) {
        override fun toString() = "Endorsement VBK Transaction confirmed, waiting for Block of Proof"
    }

    open class BlockOfProof(
        previous: Confirmed,
        val blockOfProof: VeriBlockBlock
    ) : Confirmed(previous) {
        override fun toString() = "Block of Proof received, Endorsement VBK Transaction to be proved"
        override fun getDetailedInfo() = super.getDetailedInfo() +
            "Block of Proof: ${blockOfProof.hash.bytes.toHex()}"
    }

    open class TransactionProved(
        previous: BlockOfProof,
        val merklePath: VeriBlockMerklePath
    ) : BlockOfProof(previous, previous.blockOfProof) {
        override fun toString() = "Endorsement VBK Transaction Proved, waiting for Keystone of Proof"
        override fun getDetailedInfo() = super.getDetailedInfo() +
            "Endorsement Transaction Merkle Path: ${merklePath.toCompactString()}"
    }

    open class KeystoneOfProof(
        previous: TransactionProved,
        val keystoneOfProof: VeriBlockBlock
    ) : TransactionProved(previous, previous.merklePath) {
        override fun toString() = "VBK Keystone of Proof received, waiting for VBK Publications"
        override fun getDetailedInfo() = super.getDetailedInfo() +
            "Keystone of Proof: ${keystoneOfProof.hash.bytes.toHex()}"
    }

    open class VeriBlockPublications(
        previous: KeystoneOfProof,
        val veriBlockPublications: List<VeriBlockPublication>
    ) : KeystoneOfProof(previous, previous.keystoneOfProof) {
        override fun toString() = "VBK Publications received, waiting for submission response"
        override fun getDetailedInfo() = super.getDetailedInfo() + listOf(
            "VTB Transactions: ${veriBlockPublications.joinToString { it.transaction.id.bytes.toHex() }}",
            "VTB BTC Blocks: ${veriBlockPublications.joinToString { it.firstBitcoinBlock.hash.bytes.toHex() }}"
        )
    }

    open class SubmittedPopData(
        previous: VeriBlockPublications,
        val proofOfProofId: String
    ) : VeriBlockPublications(previous, previous.veriBlockPublications) {
        override fun toString() = "Publications submitted, waiting Alt Endorsement Transaction to be confirmed"
        override fun getDetailedInfo() = super.getDetailedInfo() +
            "Proof of Proof Id: $proofOfProofId"
    }

    open class AltEndorsementTransactionConfirmed(
        previous: SubmittedPopData
    ) : SubmittedPopData(previous, previous.proofOfProofId) {
        override fun toString() = "Alt Endorsement Transaction confirmed, waiting for Endorsing Block to be confirmed"
    }

    open class AltEndorsedBlockConfirmed(
        previous: AltEndorsementTransactionConfirmed,
        val altEndorsementBlockHash: String
    ) : AltEndorsementTransactionConfirmed(previous) {
        override fun toString() = "Alt Endorsement Block confirmed, waiting for payout block"
        override fun getDetailedInfo() = super.getDetailedInfo() +
            "Alt Endorsement Block Hash: $altEndorsementBlockHash"
    }

    class Completed(
        previous: AltEndorsedBlockConfirmed,
        val payoutBlockHash: String,
        val payoutAmount: Double
    ) : AltEndorsedBlockConfirmed(previous, previous.altEndorsementBlockHash) {
        override fun toString() = "Completed"
        override fun getDetailedInfo() = super.getDetailedInfo() + listOf(
            "Payout Block Hash: $payoutBlockHash",
            "Payout Amount: $payoutAmount"
        )
    }

    open class Failed(
        val previous: OperationState,
        private val reason: String
    ) : OperationState() {
        override fun toString() = "Failed: $reason"
        override fun getDetailedInfo() = previous.getDetailedInfo()
    }
}
