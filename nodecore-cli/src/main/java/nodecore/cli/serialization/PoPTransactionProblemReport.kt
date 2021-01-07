// VeriBlock NodeCore
// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.cli.serialization

import nodecore.api.grpc.VeriBlockMessages
import nodecore.api.grpc.utilities.extensions.toHex
import nodecore.api.grpc.utilities.extensions.toProperAddressType
import org.veriblock.core.utilities.extensions.formatAtomicLongWithDecimal
import org.veriblock.core.utilities.extensions.toHex

class PoPTransactionProblemReport(
    report: VeriBlockMessages.PoPTransactionProblemReport)
{
    val address = report.address?.let {
        it.toProperAddressType()
    }

    val txid = report.txid?.toHex()

    val endorsedVBKBlockHash = report.endorsedVbkBlockHash?.toHex()

    val endorsedVBKBlockIndex = report.endorsedVbkBlockIndex

    val includedInVBKBlockHash = report.includedInVbkBlockHash?.toHex()

    val includedInVBKBlockIndex = report.includedInVbkBlockIndex

    val PotentialPoPPayoutVBKBlockHash = report.popPayoutVbkBlockHash?.toHex()

    val PotentialPoPPayoutVBKBlockIndex = report.popPayoutVbkBlockIndex

    val paidOutInPoPVBKBlock = report.paidOutInPopPayoutVbkBlock

    val PoPPayoutVBKAmount = report.popPayoutVbkAmount.formatAtomicLongWithDecimal()

    val bitcoinTxID = report.bitcoinTxid.toByteArray().toHex()

    val includedInBTCBlockHash = report.includedInBtcBlockHash?.toHex()

    val includedInBTCBlockIndex = report.includedInBtcBlockIndex

    val endorsedVBKBlockInMainChain = report.endorsedVbkBlockInMainChain

    val includedInVBKBlockInMainChain = report.includedInVbkBlockInMainChain

    val includedInBTCBlockInMainChain = report.includedInBtcBlockInMainChain

    val firstBTCBlockOfEquivalentPOPEndorsementsIndex = report.firstBtcBlockOfEquivalentPopEndorsementsIndex

    val currentBlockHeight = report.currentBlockHeight

    var potentialCause = ""

    init {
        if (!endorsedVBKBlockInMainChain) {
            potentialCause += "This PoP transaction does not endorse a VBK block in the main VBK blockchain!\n"
        }
        if (!includedInVBKBlockInMainChain) {
            potentialCause += "This PoP transaction was not included in the main VBK blockchain!\n"
        }
        if (!includedInBTCBlockInMainChain) {
            potentialCause += "The BTC transaction was not included in a Bitcoin block in the main Bitcoin blockchain!\n"
        }
        val differenceFromBestCompetitor = firstBTCBlockOfEquivalentPOPEndorsementsIndex - includedInBTCBlockIndex
        if (differenceFromBestCompetitor > 4) {
            potentialCause += "The BTC transaction was included $differenceFromBestCompetitor BTC blocks after the first BTC block to contain a PoP publication of block $endorsedVBKBlockHash.\n"
        }
        if (PotentialPoPPayoutVBKBlockIndex > report.currentBlockHeight) {
            potentialCause += "If rewarded, this PoP transaction would receive payment in VBK block $PotentialPoPPayoutVBKBlockIndex, which has not occurred yet!\n"
        }
        if (includedInVBKBlockHash == null) {
            potentialCause += "The PoP transaction does not appear to have been included in any VeriBlock block!\n"
        }
        potentialCause = potentialCause.trim()
    }
}
