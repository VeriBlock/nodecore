// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.er;

package org.veriblock.extensions.ledger;

import com.google.protobuf.ByteString;
import nodecore.api.grpc.VeriBlockMessages;
import nodecore.api.grpc.utilities.ByteStringAddressUtility;
import org.veriblock.core.bitcoinj.Base58;
import org.veriblock.core.bitcoinj.Base59;
import org.veriblock.core.utilities.AddressUtility;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * A LedgerProofOfNonexistence proves that a particular address is not routed by (does not exist in) a particular
 * Ledger Tree, which is uniquely identified by its top-level hash.
 *
 * These proofs of nonexistence function by providing proof that a particular node would be reached while
 * traversing the tree to route the particular address, that this node does not contain any further routes which
 * would continue to route the address, and prove that the node is part of the current radix hash tree ledger structure.
 *
 * The bottom route or children (depending on nonexistence proof type) stored in a proof of nonexistence is the
 * remaining route which is unrouted in the RHT.
 *
 * Depending on the addresses contained in the RHT, the proof will take one of the following forms:
 *  1. Vertical layers, the lowest of which has an unsplit route which would have to be split for the particular address
 *     to be routable
 *  2. Vertical layers down to the node which would have to have a particular child to route the particular address,
 *     plus full representations of all of the children of the node, none of which extend the route required to route
 *     the particular address.
 *
 * For example, consider the following RHT (for illustration purposes, all addresses are 5 Base58 characters long;
 * terminating nodes are marked with a caret [^], and address checksums do not exist (normally, the last 5
 * characters of an address are a deterministic result of the previous 25 and so bottom/terminating nodes' routes are
 * always at least 6 characters long)):
 *
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
 *
 * There are 14 routable addresses, and all other addresses can have a LedgerProofOfNonexistence created to
 * demonstrate that they are not in the tree.
 *
 * The first type of proof (which requires only vertical layers) is possible for addresses like VN7d4: providing
 * The layer for 7f (with two children hashes of [Q^] and [Z^]), and providing vertical layers to prove that that
 * node 7f is part of the tree, proves that VN7d4 (or any other address with Vn7 prefix without a 'f' as the 4th
 * character) does not exist in the RHT.
 *
 * The second type of proof (which requires vertical layers and horizontal layers at the bottom) is required for
 * addresses like VN7fA: providing the layer for 7f ALONG WITH both full layers [Q^] and [Z^] (not just their hashes),
 * and providing vertical layers to prove that the node 7f is part of the tree, proves that VN7fA (or any other
 * addresses with Vn7f prefix without a 'Q' or 'Z' as the 5th character) does not exist in the RHT.
 *
 * So, addresses where a node exists in the RHT which partially extends their route but continues in an impossible
 * route (ex: we are routing the address VN7d4 and after traversing VN, we hit a node 7f) can be proven with the first
 * type, as if the address was routable that final node we encounter would continue the route to the address and have
 * at least two children, one of which continued to route the address.
 *
 * Addresses where a node exists in the RHT which extends the address route but has no children which continue to
 * extend the route required by the address (ex: we are routing the address Vn7fA and after traversing Vn, we hit
 * a node 7f but see that none of its children route 'A') require the second (larger) type of proof, which includes
 * each full child node.
 *
 * As a second example of an address requiring the second type of proof, consider the address VH3nP. This proof would
 * require the node [Ax] with the hashes of its three children [h4^], [mn^], and [s], the node [N] with the hashes of
 * its two children [7f] and [P], and the node [fty] with the hashes of its two children [d^] and [v^].
 *
 * For additional examples, the following addresses can all be proved to not exist in the RHT using the first type
 * of proof:
 *         VArh4 (or any VA* which isn't VAx*)
 *         VNPi4 (or any VNPi* which isn't VNPiW)
 *         VfAX6 (or any Vf* which isn't Vfty*)
 *
 * And the following addresses must be proven with the second type of proof:
 *    VAxr6 (or any VAx* which isn't VAxh*, VAxm*, or a VAx* which is in the tree)
 *    VNPcC (or any VNP* which isn't VNPi*, or a VNP* which is in the tree)
 *    VftyW (or any Vfty* which isn't in the tree)
 *    VBa7E (or any V* except VAxh*, VAxm*, VN7*, Vf*, VNPi*, or a V* which is in the tree)
 *
 *
 */
public class LedgerProofOfNonexistence {
    // The layers of the ledger radix hash tree which prove the bottom node of interest (which fails to route
    // the particular address) exists in the ledger RHT
    private final LedgerProofNode[] verticalProofLayers;

    // The layers of the ledger radix hash tree which comprise all children of the bottom node of interest; inspection
    // of these children shows that none of them extend the route of the particular address.
    private final LedgerProofNode[] horizontalProofLayers;

    // The ledger hash is calculated based on the ledgerValue and proof layers provided to the constructor
    private final byte[] ledgerHash;

    // The address is reconstructed out of the combined routes of the ledger proof layers
    private final String address;

    // The type of nonexistence proof:
    //  Type-1 proofs show a particular node in the route would have to be split
    //  to properly route the address, but since the node is not split, the address is unroutable.
    //
    //  Type-2 proofs show a particular node in the route has no children which continue to route the address.
    private final NonexistenceProofType type;

    public enum NonexistenceProofType {
        TYPE_1,
        TYPE_2
    }

    private void checkConstructorArguments(NonexistenceProofType proofType,
                                           LedgerProofNode[] verticalProofLayers,
                                           String address,
                                           @Nullable LedgerProofNode[] horizontalProofLayers) {
        if (verticalProofLayers == null) {
            throw new IllegalArgumentException("A LedgerProofOfNonexistence for a type-1 or type-2 proof cannot " +
                    "be constructed with a null array of vertical proof layers!");
        }

        if (verticalProofLayers.length < 1) {
            throw new IllegalArgumentException("A LedgerProofOfNonexistence for a type-1 or type-2 proof cannot " +
                    "be constructed with a 0-length vertical proof layer array!");
        }

        for (int i = 0; i < verticalProofLayers.length; i++) {
            if (verticalProofLayers[i] == null) {
                throw new IllegalArgumentException("A LedgerProofOfNonexistence for a type-2 proof cannot be " +
                        "constructed with a null vertical proof layer!");
            }
        }

        if (address == null) {
            throw new IllegalArgumentException("A LedgerProofOfNonexistence for a type-1 or type-2 proof cannot " +
                    "be constructed with a null address to route!");
        }

        if (!AddressUtility.isValidStandardOrMultisigAddress(address)) {
            throw new IllegalArgumentException("A LedgerProofOfNonexistence cannot be constructed with an invalid " +
                    "address! (" + address + ")");
        }

        for (int i = 1; i < verticalProofLayers.length; i++) {
            if (verticalProofLayers[i].isBottomNode()) {
                throw new IllegalArgumentException("A LedgerProofOfNonexistence cannot have a bottom node at any " +
                        "index of the vertical proof layers array except index 0!");
            }
        }

        if (proofType == NonexistenceProofType.TYPE_2) {
            // Type-2
            if (horizontalProofLayers == null) {
                throw new IllegalArgumentException("A LedgerProofOfNonexistence for a type-2 proof cannot be " +
                        "constructed with a null array of horizontal proof layers!");
            }

            for (int i = 0; i < horizontalProofLayers.length; i++) {
                if (horizontalProofLayers[i] == null) {
                    throw new IllegalArgumentException("A LedgerProofOfNonexistence for a type-2 proof cannot be " +
                            "constructed with a null horizontal proof layer!");
                }
            }

            if (horizontalProofLayers.length < 1) {
                throw new IllegalArgumentException("A LedgerProofOfNonexistence for a type-2 proof cannot be " +
                        "constructed with a 0-length horizontal proof layer array!");
            }

            if (verticalProofLayers[0].isBottomNode()) {
                throw new IllegalArgumentException("A LedgerProofOfNonexistence for a type-2 proof cannot " +
                        "contain a bottom node!");
            }
        } else {
            // Type-1
            if (horizontalProofLayers != null) {
                throw new IllegalArgumentException("A LedgerProofOfNonexistence for a type-1 proof cannot be " +
                        "constructed with a non-null array of horizontal proof layers!");
            }
        }
    }

    /**
     * Constructor for the first type of ledger nonexistence proof: where a particular node contains a local route
     * of at least two characters which partially but not completely extends the route of an address.
     *
     * If a LedgerProofOfNonexistence is being produced for a general address prefix without a particular single
     * address, then a valid mock address with the prefix and an appropriate checksum can be provided.
     *
     * @param verticalProofLayers The layers proving existence of the bottom node of interest in the tree
     * @param address The address which is proven to not exist in the tree
     */
    public LedgerProofOfNonexistence(LedgerProofNode[] verticalProofLayers,
                                     String address) {
        checkConstructorArguments(NonexistenceProofType.TYPE_1,
                verticalProofLayers,
                address,
                null);

        boolean isBottomNode = false;

        if (verticalProofLayers[0].isBottomNode()) {
            isBottomNode = true;
        }

        this.verticalProofLayers = new LedgerProofNode[verticalProofLayers.length];
        for (int i = 0; i < verticalProofLayers.length; i++) {
            this.verticalProofLayers[i] = verticalProofLayers[i].copyOf();
        }

        // No horizontal proof layers for a type-1 nonexistence proof
        this.horizontalProofLayers = null;

        String addressDivergence = new String(verticalProofLayers[0].getRoute());

        String recomposedAddress = LedgerProofUtility.getAddressFromLedgerLayers(
                Arrays.copyOfRange(verticalProofLayers, 1, verticalProofLayers.length));

        // The recomposed address must match the first part of the provided address.
        if (!address.startsWith(recomposedAddress)) {
            throw new IllegalArgumentException("A LedgerProofOfNonexistence for a type-1 proof attempted to prove " +
                    "that the address " + address + " did not exist in the RHT, but the provided proof layers " +
                    "recompose a partial address which doesn't match (" + recomposedAddress + ")!");
        }

        // Since this is a type-1 proof, the route of the final node in the proof must diverge with the address at
        // some point.
        String choppedAddress = address.substring(recomposedAddress.length());
        boolean foundDivergence = false;

        if (addressDivergence.length() < 2) {
            throw new IllegalArgumentException("A LegerProofOfNonexistence for a type-1 proof attempted to prove " +
                    "that the address " + address + " did not exist in the RHT, but the claimed divergence node " +
                    "doesn't have enough characters (must have at least 2, because the divergence must occur within " +
                    "the route of the node claiming to prove divergence)!");
        }

        // Starts at 1 because the divergence cannot occur at 0; a divergence at the beginning of the route of the
        // next node doesn't prove that an alternate sibling node to the provided bottom node doesn't properly route
        // the address (in that case, a type-2 proof needs to be used instead).
        if (choppedAddress.charAt(0) != addressDivergence.charAt(0)) {
            throw new IllegalArgumentException("A LegerProofOfNonexistence for a type-1 proof attempted to prove " +
                    "that the address " + address + " did not exist in the RHT, but the provided proof layers " +
                    " recomposed a partial address of " + recomposedAddress + " and the claimed divergence " +
                    addressDivergence + " diverges from the address we are proving on its first character, which " +
                    "doesn't prove that a sibling to the provided bottom node doesn't correctly route the address!");
        }

        for (int i = 1; i < choppedAddress.length() && i < addressDivergence.length(); i++) {
            if (choppedAddress.charAt(i) != addressDivergence.charAt(i)) {
                // Divergence found; this proof validly demonstrates that the particular address does not exist
                // in the ledger RHT!
                foundDivergence = true;
            }
        }

        if (!foundDivergence) {
            throw new IllegalArgumentException("A LegerProofOfNonexistence for a type-1 proof attempted to prove " +
                    "that the address " + address + " did not exist in the RHT, but the provided proof layers " +
                    " recomposed a partial address of " + recomposedAddress + " and claimed a divergence within " +
                    addressDivergence + " although no divergence exists there!");
        }

        // Finished following ledger proof up to ledger hash
        this.ledgerHash = LedgerProofUtility.calculateTopHash(
                verticalProofLayers,
                null);

        this.address = address;

        this.type = NonexistenceProofType.TYPE_1;
    }

    /**
     * Constructor for the second type of ledger nonexistence proof: where a particular node encountered in the RHT
     * traversal to route a particular address does not have any children which continue to route the particular
     * address.
     *
     * This type of proof requires the full children (including their left and right contiguous child hashes and
     * internal route) of the last node which continues to route the particular address. Validating the proof requires
     * checking that:
     *  1. The complete path to the final node properly routes the first portion of the unroutable address
     *  2. The final node which continues to route the address has no children which further route the address (a child
     *     could continue the route with one or more characters of their route but diverge from the address later
     *     in the internal route).
     */
    public LedgerProofOfNonexistence(LedgerProofNode[] verticalProofLayers,
                                     String address,
                                     LedgerProofNode[] horizontalProofLayers) {
        checkConstructorArguments(NonexistenceProofType.TYPE_2,
                verticalProofLayers,
                address,
                horizontalProofLayers);

        this.verticalProofLayers = new LedgerProofNode[verticalProofLayers.length];
        for (int i = 0; i < verticalProofLayers.length; i++) {
            this.verticalProofLayers[i] = verticalProofLayers[i].copyOf();
        }


        this.horizontalProofLayers = new LedgerProofNode[horizontalProofLayers.length];
        for (int i = 0; i < horizontalProofLayers.length; i++) {
            this.horizontalProofLayers[i] = horizontalProofLayers[i].copyOf();
        }

        String recomposedAddress = LedgerProofUtility.getAddressFromLedgerLayers(verticalProofLayers);

        // The recomposed address must match the first part of the provided address.
        if (!address.startsWith(recomposedAddress)) {
            throw new IllegalArgumentException("A LedgerProofOfNonexistence for a type-2 proof attempted to prove " +
                    "that the address " + address + " did not exist in the RHT, but the provided proof layers " +
                    "recompose a partial address which doesn't match (" + recomposedAddress + ")!");
        }

        // Since this is a type-2 proof, none of the children (horizontal nodes) can fully extend the address.
        // Instead, we check all of the children (horizontal layers) do not extend the address.
        String choppedAddress = address.substring(recomposedAddress.length());

        outer: for (int i = 0; i < horizontalProofLayers.length; i++) {
            LedgerProofNode child = horizontalProofLayers[i];
            String childRoute = new String(child.getRoute());

            for (int j = 0; j < childRoute.length(); j++) {
                if (choppedAddress.charAt(j) != childRoute.charAt(j)) {
                    continue outer;
                }
            }

            throw new IllegalArgumentException("A LedgerProofOfNonexistence for a type-2 proof attempted to prove " +
                    "that the address " + address + " did not exist in the RHT, but the provided child at index " +
                    i + " with route " + childRoute + " extended the chopped address " + choppedAddress + "!");
        }

        // Finished following ledger proof up to ledger hash
        this.ledgerHash = LedgerProofUtility.calculateTopHash(
                verticalProofLayers,
                horizontalProofLayers);

        this.address = address;

        this.type = NonexistenceProofType.TYPE_2;
    }

    public NonexistenceProofType getType() {
        return type;
    }

    /**
     * Fetches the ledger RHT hash which this proof authenticates to.
     * @return The ledger RHT hash which authenticates this proof
     */
    public byte[] getLedgerHash() {
        byte[] copy = new byte[ledgerHash.length];
        System.arraycopy(ledgerHash, 0, copy, 0, ledgerHash.length);
        return copy;
    }

    /**
     * Returns the address which was originally used to create this LedgerProofOfNonexistence. Note that this
     * LedgerProofOfNonexistence may prove multiple different addresses (all with the same prefix) do not exist
     * in the RHT.
     * @return The address which the proof of nonexistence was originally produced for
     */
    public String getAddress() {
        return address;
    }

    public VeriBlockMessages.LedgerProofOfNonexistence.Builder getMessageBuilder() {
        VeriBlockMessages.LedgerProofOfNonexistence.Builder builder = VeriBlockMessages.LedgerProofOfNonexistence.newBuilder();
        for (int i = 0; i < verticalProofLayers.length; i++) {
            builder.addVerticalProofLayers(verticalProofLayers[i].getMessageBuilder());
        }

        if (horizontalProofLayers != null) {
            for (int i = 0; i < horizontalProofLayers.length; i++) {
                builder.addHorizontalProofLayers(horizontalProofLayers[i].getMessageBuilder());
            }
        }

        if (AddressUtility.isValidStandardAddress(getAddress())) {
            builder.setAddress(ByteString.copyFrom(Base58.decode(getAddress())));
        } else {
            builder.setAddress(ByteString.copyFrom(Base59.decode(getAddress())));
        }

        return builder;
    }

    public static LedgerProofOfNonexistence parseFrom(VeriBlockMessages.LedgerProofOfNonexistenceOrBuilder message) {
        LedgerProofNode[] verticalLayers = message.getVerticalProofLayersList().stream().map(LedgerProofNode::parseFrom)
                .collect(Collectors.toList()).toArray(new LedgerProofNode[message.getVerticalProofLayersCount()]);

        String address = ByteStringAddressUtility.parseProperAddressTypeAutomatically(message.getAddress());

        if (message.getHorizontalProofLayersCount() == 0) {
            // This is a type-1 proof, which only needs vertical layers
            return new LedgerProofOfNonexistence(verticalLayers, address);
        } else {
            // This is a type-2 proof, which also uses horizontal layers
            LedgerProofNode[] horizontalLayers = message.getHorizontalProofLayersList().stream()
                    .map(LedgerProofNode::parseFrom).collect(Collectors.toList())
                    .toArray(new LedgerProofNode[message.getHorizontalProofLayersCount()]);

            return new LedgerProofOfNonexistence(verticalLayers, address, horizontalLayers);
        }
    }
}

