// VeriBlock NodeCore
// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.cli.serialization

import nodecore.api.grpc.RpcPopTransactionProblemReport
import org.veriblock.sdk.extensions.toHex
import org.veriblock.sdk.extensions.toProperAddressType
import org.veriblock.core.utilities.extensions.formatAtomicLongWithDecimal
import org.veriblock.core.utilities.extensions.toHex

class PopTransactionProblemReport(
    report: RpcPopTransactionProblemReport
) {
    val address = report.address?.toProperAddressType()

    val txid = report.txid?.toHex()

    val endorsedVbkBlockHash = report.endorsedVbkBlockHash?.toHex()

    val endorsedVbkBlockIndex = report.endorsedVbkBlockIndex

    val includedInVbkBlockHash = report.includedInVbkBlockHash?.toHex()

    val includedInVbkBlockIndex = report.includedInVbkBlockIndex

    val potentialPopPayoutVbkBlockHash = report.popPayoutVbkBlockHash?.toHex()

    val potentialPopPayoutVbkBlockIndex = report.popPayoutVbkBlockIndex

    val paidOutInPopVbkBlock = report.paidOutInPopPayoutVbkBlock

    val popPayoutVbkAmount = report.popPayoutVbkAmount.formatAtomicLongWithDecimal()

    val bitcoinTxId = report.bitcoinTxid.toByteArray().toHex()

    val includedInBtcBlockHash = report.includedInBtcBlockHash?.toHex()

    val includedInBtcBlockIndex = report.includedInBtcBlockIndex

    val endorsedVbkBlockInMainChain = report.endorsedVbkBlockInMainChain

    val includedInVbkBlockInMainChain = report.includedInVbkBlockInMainChain

    val includedInBtcBlockInMainChain = report.includedInBtcBlockInMainChain

    val firstBtcBlockOfEquivalentPopEndorsementsIndex = report.firstBtcBlockOfEquivalentPopEndorsementsIndex

    val currentBlockHeight = report.currentBlockHeight

    var potentialCause = ""

    init {
        if (!endorsedVbkBlockInMainChain) {
            potentialCause += "This PoP transaction does not endorse a VBK block in the main VBK blockchain!\n"
        }
        if (!includedInVbkBlockInMainChain) {
            potentialCause += "This PoP transaction was not included in the main VBK blockchain!\n"
        }
        if (!includedInBtcBlockInMainChain) {
            potentialCause += "The BTC transaction was not included in a Bitcoin block in the main Bitcoin blockchain!\n"
        }
        val differenceFromBestCompetitor = firstBtcBlockOfEquivalentPopEndorsementsIndex - includedInBtcBlockIndex
        if (differenceFromBestCompetitor > 4) {
            potentialCause += "The BTC transaction was included $differenceFromBestCompetitor BTC blocks after the first BTC block to contain a PoP publication of block $endorsedVbkBlockHash.\n"
        }
        if (potentialPopPayoutVbkBlockIndex > report.currentBlockHeight) {
            potentialCause += "If rewarded, this PoP transaction would receive payment in VBK block $potentialPopPayoutVbkBlockIndex, which has not occurred yet!\n"
        }
        if (includedInVbkBlockHash == null) {
            potentialCause += "The PoP transaction does not appear to have been included in any VeriBlock block!\n"
        }
        potentialCause = potentialCause.trim()
    }
}
