// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.er;

package org.veriblock.extensions.ledger;

import nodecore.api.grpc.VeriBlockMessages;

import java.util.stream.Collectors;

/**
 * A LedgerProofOfExistence proves that a particular LedgerValue is associated with a particular address in the
 * Ledger described by a particular hash.
 *
 * Because the Ledger is implemented as a radix hash tree, each non-bottom layer of ledger proof contains a number of
 * child hashes which represent the states of all children nodes (which each eventually terminate in one or more
 * bottom layers which contain a balance and signature index).
 *
 * These proofs of existence function by providing proof that a particular node would be reached while
 * traversing the tree to route the particular address, and proof that the node is part of the current radix hash tree
 * ledger structure by providing the appropriate ancillary child hashes which allow us to recalculate the RHT hash.
 *
 * For example, consider the following RHT (for illustration purposes, all addresses are 5 Base58 characters long;
 * terminating nodes are marked with a caret [^], and address checksums do not exist (normally, the last 5
 * characters of an address are a deterministic result of the previous 25 and so bottom/terminating nodes' routes are
 * always at least 6 characters long)):
 *                                                      [V]
 *                                                       |
 *                                     ---------------------------------
 *                                    /                 |               \
 *                                 [Ax]                [N]             [fty]
 *                                  |                   |                |
 *                              ---------           -----------       ----
 *                             /   |     \         /           \       |   \
 *                           [h4^] [mn^] [s]      [7f]         [P]   [d^]  [v^]
 *                                        |        |            |
 *                                  -------       ----        ----
 *                                /   |   \     /    \       /    \
 *                               [h^] [v^] [x^] [Q^] [Z^]   [c]   [iW^]
 *                                                           |
 *                                                     --------------
 *                                                    /    |    |    \
 *                                                  [5^] [D^] [g^] [t^]
 *
 * There are 14 routable addresses, and each one can have a LedgerProofOfExistence created for them.
 *
 * The address VAxmn (for example) can be proven to exist in the tree (and have a particular balance and signature
 * index) by providing the LedgerValue associated with VAxmn, the bottom route "mn" which is associated with the
 * terminating  node, the hashes of the terminating node [h4^] and of the non-terminating node [s], and the hashes of
 * the non-terminating nodes [N] and [fty]. Similar to a Merkle path, this proves the LedgerValue routed by VAxmn
 * in the tree (uniquely identified by its hash) exists.
 */

public class LedgerProofOfExistence {
    // The layers of the ledger radix hash tree which prove the ledger value against the top ledger hash
    private final LedgerProofNode[] verticalProofLayers;

    // The ledger hash is calculated based on the ledgerValue and proof layers provided to the constructor
    private final byte[] ledgerHash;

    // The address is reconstructed out of the combined routes of the ledger proof layers
    private final String address;

    // The ledger value proven by the proof (and contained in the first vertical proof layer)
    private final LedgerValue ledgerValue;

    public LedgerProofOfExistence(LedgerProofNode[] verticalProofLayers) {
        if (verticalProofLayers == null) {
            throw new IllegalArgumentException("A LedgerProofOfExistence cannot be constructed with a null " +
                    "array of vertical proof layers!");
        }

        for (int i = 0; i < verticalProofLayers.length; i++) {
            if (verticalProofLayers[i] == null) {
                throw new IllegalArgumentException("A LedgerProofOfExistence cannot be constructed with a null " +
                        "vertical proof layer!");
            }
        }

        if (!verticalProofLayers[0].isBottomNode()) {
            throw new IllegalArgumentException("A LedgerProofOfExistence cannot be constructed with a " +
                    "non-terminating vertical proof layer at index 0!");
        }

        for (int i = 1; i < verticalProofLayers.length; i++) {
            if (verticalProofLayers[i].isBottomNode()) {
                throw new IllegalArgumentException("A LedgerProofOfExistence cannot be constructed with a " +
                        "terminating vertical proof layer at any index except 0 (found terminating node at index " +
                        i + "!)");
            }
        }

        this.verticalProofLayers = new LedgerProofNode[verticalProofLayers.length];
        for (int i = 0; i < verticalProofLayers.length; i++) {
            this.verticalProofLayers[i] = verticalProofLayers[i].copyOf();
        }

        this.address = LedgerProofUtility.getAddressFromLedgerLayers(verticalProofLayers);
        this.ledgerHash = LedgerProofUtility.calculateTopHash(verticalProofLayers, null);
        this.ledgerValue = verticalProofLayers[0].getLedgerValue().copyOf();
    }

    public byte[] getLedgerHash() {
        byte[] copy = new byte[ledgerHash.length];
        System.arraycopy(ledgerHash, 0, copy, 0, ledgerHash.length);

        return copy;
    }

    public String getAddress() {
        return address;
    }

    public LedgerValue getLedgerValue() {
        return ledgerValue.copyOf();
    }

    public static LedgerProofOfExistence parseFrom(VeriBlockMessages.LedgerProofOfExistence message) {
        return new LedgerProofOfExistence(message.getVerticalProofLayersList().stream().map(LedgerProofNode::parseFrom)
                .collect(Collectors.toList()).toArray(new LedgerProofNode[message.getVerticalProofLayersCount()]));
    }

    public VeriBlockMessages.LedgerProofOfExistence.Builder getMessageBuilder() {
        VeriBlockMessages.LedgerProofOfExistence.Builder builder = VeriBlockMessages.LedgerProofOfExistence.newBuilder();
        for (int i = 0; i < verticalProofLayers.length; i++) {
            builder.addVerticalProofLayers(verticalProofLayers[i].getMessageBuilder());
        }

        return builder;
    }
}
