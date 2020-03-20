// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.serialization;

import nodecore.api.grpc.VeriBlockMessages;
import nodecore.api.grpc.utilities.ByteStringAddressUtility;
import org.veriblock.core.utilities.Utility;

public class PoPTransactionProblemReport {
    PoPTransactionProblemReport(VeriBlockMessages.PoPTransactionProblemReport report) {
        if (report.getAddress() != null) {
            address = ByteStringAddressUtility.parseProperAddressTypeAutomatically(report.getAddress());
        }

        if (report.getTxid() != null) {
            txid = Utility.bytesToHex(report.getTxid().toByteArray());
        }

        if (report.getEndorsedVbkBlockHash() != null) {
            endorsedVBKBlockHash = Utility.bytesToHex(report.getEndorsedVbkBlockHash().toByteArray());
        }

        endorsedVBKBlockIndex = report.getEndorsedVbkBlockIndex();

        if (report.getIncludedInVbkBlockHash() != null) {
            includedInVBKBlockHash = Utility.bytesToHex(report.getIncludedInVbkBlockHash().toByteArray());
        }

        includedInVBKBlockIndex = report.getIncludedInVbkBlockIndex();

        if (report.getPopPayoutVbkBlockHash() != null) {
            PotentialPoPPayoutVBKBlockHash = Utility.bytesToHex(report.getPopPayoutVbkBlockHash().toByteArray());
        }

        PotentialPoPPayoutVBKBlockIndex = report.getPopPayoutVbkBlockIndex();

        paidOutInPoPVBKBlock = report.getPaidOutInPopPayoutVbkBlock();

        PoPPayoutVBKAmount = Utility.formatAtomicLongWithDecimal(report.getPopPayoutVbkAmount());

        bitcoinTxID = Utility.bytesToHex(report.getBitcoinTxid().toByteArray());

        if (report.getIncludedInBtcBlockHash() != null) {
            includedInBTCBlockHash = Utility.bytesToHex(report.getIncludedInBtcBlockHash().toByteArray());
        }

        includedInBTCBlockIndex = report.getIncludedInBtcBlockIndex();

        endorsedVBKBlockInMainChain = report.getEndorsedVbkBlockInMainChain();

        includedInVBKBlockInMainChain = report.getIncludedInVbkBlockInMainChain();

        includedInBTCBlockInMainChain = report.getIncludedInBtcBlockInMainChain();

        firstBTCBlockOfEquivalentPOPEndorsementsIndex = report.getFirstBtcBlockOfEquivalentPopEndorsementsIndex();

        if (!endorsedVBKBlockInMainChain) {
            potentialCause += "This PoP transaction does not endorse a VBK block in the main VBK blockchain!\n";
        }

        if (!includedInVBKBlockInMainChain) {
            potentialCause += "This PoP transaction was not included in the main VBK blockchain!\n";
        }

        if (!includedInBTCBlockInMainChain) {
            potentialCause += "The BTC transaction was not included in a Bitcoin block in the main Bitcoin blockchain!\n";
        }

        int differenceFromBestCompetitor = firstBTCBlockOfEquivalentPOPEndorsementsIndex - includedInBTCBlockIndex;
        if (differenceFromBestCompetitor > 4) {
            potentialCause += "The BTC transaction was included " + differenceFromBestCompetitor + " BTC blocks after the first BTC block to contain a PoP publication of block " + endorsedVBKBlockHash + ".\n";
        }

        if (PotentialPoPPayoutVBKBlockIndex > report.getCurrentBlockHeight()) {
            potentialCause += "If rewarded, this PoP transaction would receive payment in VBK block " + PotentialPoPPayoutVBKBlockIndex + ", which has not occurred yet!\n";
        }

        if (includedInVBKBlockHash == null) {
            potentialCause += "The PoP transaction does not appear to have been included in any VeriBlock block!\n";
        }

        currentBlockHeight = report.getCurrentBlockHeight();

        potentialCause = potentialCause.trim();
    }

    public String address;

    public String txid;

    public String endorsedVBKBlockHash;

    public int endorsedVBKBlockIndex;

    public String includedInVBKBlockHash;

    public int includedInVBKBlockIndex;

    public String PotentialPoPPayoutVBKBlockHash;

    public int PotentialPoPPayoutVBKBlockIndex;

    public boolean paidOutInPoPVBKBlock;

    public String PoPPayoutVBKAmount;

    public String bitcoinTxID;

    public String includedInBTCBlockHash;

    public int includedInBTCBlockIndex;

    public boolean endorsedVBKBlockInMainChain;

    public boolean includedInVBKBlockInMainChain;

    public boolean includedInBTCBlockInMainChain;

    public int firstBTCBlockOfEquivalentPOPEndorsementsIndex;

    public int currentBlockHeight;

    public String potentialCause = "";
}
