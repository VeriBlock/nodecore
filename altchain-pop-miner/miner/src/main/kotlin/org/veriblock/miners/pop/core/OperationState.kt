// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.core

import org.veriblock.core.utilities.extensions.toHex
import org.veriblock.lite.wallet.WalletTransaction
import org.veriblock.sdk.VeriBlockBlock
import org.veriblock.sdk.VeriBlockMerklePath
import org.veriblock.sdk.VeriBlockPublication
import org.veriblock.sdk.alt.PublicationDataWithContext


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
        override fun toString() = "Publications submitted, waiting for depth completion"
        override fun getDetailedInfo() = super.getDetailedInfo() +
            "Proof of Proof Id: $proofOfProofId"
    }

    class Completed(
        previous: SubmittedPopData
    ) : SubmittedPopData(previous, previous.proofOfProofId) {
        override fun toString() = "Completed"
    }

    object Reorganized : OperationState() {
        override fun toString() = "SP Chain has reorganized"
    }

    object Failed : OperationState() {
        override fun toString() = "Failed"
    }
}
