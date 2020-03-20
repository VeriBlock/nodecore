// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.er;

package org.veriblock.extensions.ledger;

import nodecore.api.grpc.VeriBlockMessages;

/**
 * A LedgerProof contains either a LedgerProofOfExistence or LedgerProofOfNonexistence.
 */
public class LedgerProof {
    private final LedgerProofOfExistence ledgerProofOfExistence;
    private final LedgerProofOfNonexistence ledgerProofOfNonexistence;

    public LedgerProof(LedgerProofOfExistence proofOfExistence) {
        this.ledgerProofOfExistence = proofOfExistence;
        this.ledgerProofOfNonexistence = null;
    }

    public LedgerProof(LedgerProofOfNonexistence proofOfNonexistence) {
        this.ledgerProofOfExistence = null;
        this.ledgerProofOfNonexistence = proofOfNonexistence;
    }

    public LedgerProofOfExistence getProofOfExistence() {
        return ledgerProofOfExistence;
    }

    public LedgerProofOfNonexistence getLedgerProofOfNonexistence() {
        return ledgerProofOfNonexistence;
    }

    public boolean addressExistsInLedgerTree() {
        return ledgerProofOfExistence != null;
    }

    public String getAddress() {
        if (ledgerProofOfExistence != null) {
            return ledgerProofOfExistence.getAddress();
        } else {
            return ledgerProofOfNonexistence.getAddress();
        }
    }

    public byte[] getLedgerHash() {
        if (ledgerProofOfExistence != null) {
            return ledgerProofOfExistence.getLedgerHash();
        } else {
            return ledgerProofOfNonexistence.getLedgerHash();
        }
    }

    public LedgerValue getLedgerValue() {
        if (ledgerProofOfExistence != null) {
            return ledgerProofOfExistence.getLedgerValue();
        } else {
            return null;
        }
    }

    public LedgerProofStatus checkAddress(String address) {
        if (ledgerProofOfExistence != null) {
            if (ledgerProofOfExistence.getAddress().equals(address)) {
                return LedgerProofStatus.PROVEN_TO_EXIST;
            } else {
                return LedgerProofStatus.UNKNOWN;
            }
        } else {
            if (ledgerProofOfNonexistence != null) {
                if (ledgerProofOfNonexistence.getAddress().equals(address)) {
                    // TODO: update LedgerProofOfNonexistence to validate multiple similar addresses to not exist
                    return LedgerProofStatus.PROVEN_TO_NOT_EXIST;
                } else {
                    return LedgerProofStatus.UNKNOWN;
                }
            } else {
                return LedgerProofStatus.UNKNOWN;
            }
        }
    }

    enum LedgerProofStatus {
        PROVEN_TO_EXIST, // A LedgerProofOfExistence proves that this address exists in the ledger tree
        PROVEN_TO_NOT_EXIST, // A LedgerProofOfNonexistence proves that this address does not exist in the ledger tree
        UNKNOWN // The address's existence isn't either proven or disproven
    }

    public VeriBlockMessages.LedgerProof.Builder getMessageBuilder() {
        VeriBlockMessages.LedgerProof.Builder builder = VeriBlockMessages.LedgerProof.newBuilder();
        if (ledgerProofOfExistence != null) {
            builder.setProofOfExistence(ledgerProofOfExistence.getMessageBuilder());
        } else {
            builder.setProofOfNonexistence(ledgerProofOfNonexistence.getMessageBuilder());
        }

        return builder;
    }

    public static LedgerProof parseFrom(VeriBlockMessages.LedgerProofOrBuilder message) {
        if (message.hasProofOfExistence()) {
            return new LedgerProof(LedgerProofOfExistence.parseFrom(message.getProofOfExistence()));
        } else {
            return new LedgerProof(LedgerProofOfNonexistence.parseFrom(message.getProofOfNonexistence()));
        }
    }
}
