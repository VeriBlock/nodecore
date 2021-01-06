// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.cli.serialization

import nodecore.api.grpc.VeriBlockMessages
import nodecore.api.grpc.utilities.ByteStringAddressUtility
import org.veriblock.core.utilities.Utility

class PoPTransactionProblemReport(
    report: VeriBlockMessages.PoPTransactionProblemReport)
{
    val address = report.address?.let {
        ByteStringAddressUtility.parseProperAddressTypeAutomatically(it)
    }

    val txid = report.txid?.let {
        Utility.bytesToHex(it.toByteArray())
    }

    val endorsedVBKBlockHash = report.endorsedVbkBlockHash?.let {
        Utility.bytesToHex(it.toByteArray())
    }

    val endorsedVBKBlockIndex = report.endorsedVbkBlockIndex

    val includedInVBKBlockHash = report.includedInVbkBlockHash?.let {
        Utility.bytesToHex(it.toByteArray())
    }

    val includedInVBKBlockIndex = report.includedInVbkBlockIndex

    val PotentialPoPPayoutVBKBlockHash = report.popPayoutVbkBlockHash?.let {
        Utility.bytesToHex(it.toByteArray())
    }

    val PotentialPoPPayoutVBKBlockIndex = report.popPayoutVbkBlockIndex

    val paidOutInPoPVBKBlock = report.paidOutInPopPayoutVbkBlock

    val PoPPayoutVBKAmount = Utility.formatAtomicLongWithDecimal(report.popPayoutVbkAmount)

    val bitcoinTxID = Utility.bytesToHex(report.bitcoinTxid.toByteArray())

    val includedInBTCBlockHash = report.includedInBtcBlockHash?.let {
        Utility.bytesToHex(it.toByteArray())
    }

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
        potentialCause = potentialCause.trim { it <= ' ' }
    }
}