// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.er;

package org.veriblock.extensions.ledger;

import com.google.protobuf.ByteString;
import nodecore.api.grpc.VeriBlockMessages;
import nodecore.api.grpc.utilities.ByteStringUtility;
import org.veriblock.core.crypto.Crypto;

import javax.annotation.Nullable;

/**
 * A node of a ledger proof contains the appropriate route, along with the (ordered) hashes of all child nodes (except
 * the hash of the child node which will be calculated when processing the ledger proof(s) this ledger node is contained
 * in), or the corresponding LedgerValue if the node is a terminating node.
 *
 * Note that a ledger proof layer is missing the "middle" content (sandwiched between the left and right contiguous
 * child hashes); this middle content is produced when the Merkle path is calculated (similar to how only the opposing
 * side is provided in a Merkle root).
 */
public class LedgerProofNode {
    enum Type {
        NORMAL,
        TERMINATING
    }

    // Whether the node is a normal or terminating node
    private final Type type;

    // The route that the node at the corresponding layer contains
    private final char[] route;

    // The serialized children hashes to the left of the "bubbling-up" hash (NORMAL type only)
    private final byte[] leftContiguousChildHashes;

    // The serialized children hashes to the right of the "bubbling-up" hash (NORMAL type only)
    private final byte[] rightContiguousChildHashes;

    // The LedgerValue which this node contains (TERMINATING type only)
    private final LedgerValue ledgerValue;

    public LedgerProofNode(char[] route, byte[] leftContiguousChildHashes, byte[] rightContiguousChildHashes) {
        if (route == null) {
            throw new IllegalArgumentException("A LedgerProofNode cannot be constructed with a null route!");
        }

        if (leftContiguousChildHashes == null) {
            throw new IllegalArgumentException("A non-terminating LedgerProofNode cannot be constructed with a null " +
                    "array of left contiguous child hashes!");
        }

        if (rightContiguousChildHashes == null) {
            throw new IllegalArgumentException("A non-terminating LedgerProofNode cannot be constructed with a null" +
                    " array of right contiguous child hashes!");
        }

        this.route = new char[route.length];
        System.arraycopy(route, 0, this.route, 0, route.length);

        this.leftContiguousChildHashes = new byte[leftContiguousChildHashes.length];
        System.arraycopy(leftContiguousChildHashes,
                0,
                this.leftContiguousChildHashes,
                0,
                leftContiguousChildHashes.length);

        this.rightContiguousChildHashes = new byte[rightContiguousChildHashes.length];
        System.arraycopy(rightContiguousChildHashes,
                0,
                this.rightContiguousChildHashes,
                0,
                rightContiguousChildHashes.length);

        this.ledgerValue = null;

        this.type = Type.NORMAL;
    }

    public LedgerProofNode(char[] route, LedgerValue ledgerValue) {
        if (route == null) {
            throw new IllegalArgumentException("A LedgerProofNode cannot be constructed with a null route!");
        }

        if (ledgerValue == null) {
            throw new IllegalArgumentException("A terminating LedgerProofNode cannot be constructed with a null" +
                    " ledger value!");
        }

        this.route = new char[route.length];
        System.arraycopy(route, 0, this.route, 0, route.length);

        this.leftContiguousChildHashes = null;

        this.rightContiguousChildHashes = null;

        this.ledgerValue = ledgerValue.copyOf();

        this.type = Type.TERMINATING;
    }

    public static LedgerProofNode parseFrom(VeriBlockMessages.LedgerProofNodeOrBuilder message) {
        if (message.hasLedgerValue()) {
            // Terminating node
            return new LedgerProofNode(
                    ByteStringUtility.byteStringToBase59(message.getRoute()).toCharArray(),
                    new LedgerValue(message.getLedgerValue()));
        } else {
            // Normal node
            return new LedgerProofNode(
                    ByteStringUtility.byteStringToBase59(message.getRoute()).toCharArray(),
                    message.getLeftContiguousChildHashes().toByteArray(),
                    message.getRightContiguousChildHashes().toByteArray());
        }
    }

    public char[] getRoute() {
        char[] temp = new char[route.length];
        System.arraycopy(route, 0, temp, 0, route.length);
        return temp;
    }

    public byte[] getContiguousLeftChildHashes() {
        if (type != Type.NORMAL) {
            throw new UnsupportedOperationException("The left or right child hashes do not exist for a normal " +
                    "LedgerProofNode!");
        }

        byte[] temp = new byte[leftContiguousChildHashes.length];
        System.arraycopy(leftContiguousChildHashes, 0, temp, 0, leftContiguousChildHashes.length);
        return temp;
    }

    public byte[] getContiguousRightChildHashes() {
        if (type != Type.NORMAL) {
            throw new UnsupportedOperationException("The left or right child hashes do not exist for a normal " +
                    "LedgerProofNode!");
        }

        byte[] temp = new byte[rightContiguousChildHashes.length];
        System.arraycopy(rightContiguousChildHashes, 0, temp, 0, rightContiguousChildHashes.length);
        return temp;
    }

    public LedgerValue getLedgerValue() {
        if (type != Type.TERMINATING) {
            throw new UnsupportedOperationException("The ledger value does not exist for a non-terminating " +
                    "LedgerProofNode!");
        }

        return ledgerValue.copyOf();
    }

    public Type getType() {
        return type;
    }

    public boolean isBottomNode() {
        return type == Type.TERMINATING;
    }

    /**
     * Calculates the hash of the node, (optionally) with the provided hash placed between the left and right
     * contiguous child hashes.
     * @param bubbleUp
     * @return
     */
    public byte[] calculateHash(@Nullable byte[] bubbleUp,
                                @Nullable byte[] substituteChildHashes) {
        if (substituteChildHashes != null) {
            if (bubbleUp != null) {
                // If substitute children are provided, then they encompass all child hashes, so no bubble-up hash
                // can be inserted.
                throw new IllegalArgumentException("calculateHash cannot be called with both a bubbleUp byte and " +
                        "substitute child hashes array!");
            }
        }

        Crypto crypto = new Crypto();
        byte[] hash;

        if (type == Type.TERMINATING) {
            hash = crypto.SHA256ReturnBytes(String.format(
                    "%s:%d:%d",
                    new String(route),
                    ledgerValue.getAvailableAtomicUnits(),
                    ledgerValue.getSignatureIndex()));

            return crypto.SHA256ReturnBytes(hash);
        } else {
            hash = crypto.SHA256ReturnBytes(new String(route));

            byte[] input = new byte[hash.length +
                    (substituteChildHashes == null ?
                            leftContiguousChildHashes.length + ((bubbleUp == null ? 0 : bubbleUp.length)) +
                                    rightContiguousChildHashes.length :
                            substituteChildHashes.length)];

            int cursor = 0;
            System.arraycopy(hash, 0, input, 0, hash.length);
            cursor += hash.length;

            if (substituteChildHashes == null) {
                System.arraycopy(leftContiguousChildHashes, 0, input, cursor, leftContiguousChildHashes.length);
                cursor += leftContiguousChildHashes.length;

                if (bubbleUp != null) {
                    System.arraycopy(bubbleUp, 0, input, cursor, bubbleUp.length);
                    cursor += bubbleUp.length;
                }

                System.arraycopy(rightContiguousChildHashes, 0, input, cursor, rightContiguousChildHashes.length);
            } else {
                System.arraycopy(substituteChildHashes, 0, input, cursor, substituteChildHashes.length);
            }

            return crypto.SHA256ReturnBytes(input);
        }
    }

    public LedgerProofNode copyOf() {
        if (type == Type.NORMAL) {
            return new LedgerProofNode(route, leftContiguousChildHashes, rightContiguousChildHashes);
        } else {
            return new LedgerProofNode(route, ledgerValue);
        }
    }

    public VeriBlockMessages.LedgerProofNode.Builder getMessageBuilder() {
        VeriBlockMessages.LedgerProofNode.Builder builder = VeriBlockMessages.LedgerProofNode.newBuilder();

        builder.setRoute(ByteStringUtility.base59ToByteString(new String(route)));

        if (getType() == Type.NORMAL) {
            builder.setLeftContiguousChildHashes(ByteString.copyFrom(getContiguousLeftChildHashes()));
            builder.setRightContiguousChildHashes(ByteString.copyFrom(getContiguousRightChildHashes()));
        } else {
            builder.setLedgerValue(ledgerValue.getMessageBuilder());
        }

        return builder;
    }

}
