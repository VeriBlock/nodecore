// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.er;

package org.veriblock.extensions.ledger;

import com.google.protobuf.ByteString;
import nodecore.api.grpc.VeriBlockMessages;
import org.veriblock.core.crypto.Crypto;
import org.veriblock.core.utilities.BlockUtility;
import org.veriblock.core.utilities.Utility;

import java.util.stream.Collectors;

/**
 * A LedgerProofWithContext demonstrates that a particular ledger proof exists in a particular VeriBlock block header.
 *
 * The hash of the RHT is not stored directly in the block header; the Merkle root in the block header is a
 * Merkle root of a datastructure containing several different fields, including the RHT hash.
 *
 * A LedgerProof contains a cryptographic authentication of an address's existence (and associated balance/sigindex)
 * or of an address's lack of presence in a RHT uniquely identified by the RHT hash, and a LedgerProofWithContext
 * provides authentication of that RHT hash to the Merkle root stored in a VeriBlock block header.
 */

public class LedgerProofWithContext {
    // The LedgerProof which contains either a LedgerProofOfExistance or LedgerProofOfNonexistance, depending on
    // whether the address was routed by the Ledger RHT at the time the proof was produced.
    private final LedgerProof ledgerProof;

    // The top path consists of the hash of the three opposing hashes (hash of the block fee table, hash of the
    // left side of the block content metapackage container, and root of the transaction tree) which authenticate
    // the ledger hash to the top-level Merkle root contained in the VeriBlock block header
    private final byte[][] topPath;

    // The top root is the top-level root which is calculated by combining the ledger hash with the top path and
    // is included in the VeriBlock block header.
    private final byte[] topRoot;

    // The VeriBlock block header containing the
    private final byte[] containingHeader;

    public LedgerProofWithContext(LedgerProof ledgerProof, byte[][] topPath, byte[] containingHeader) {
        if (ledgerProof == null) {
            throw new IllegalArgumentException("A LedgerProofWithContext cannot be constructed with a null ledger proof!");
        }

        this.ledgerProof = ledgerProof;

        if (topPath == null) {
            throw new IllegalArgumentException("A LedgerProofWithContext cannot be constructed with a null topPath byte " +
                    "array!");
        }

        if (topPath.length != 5) {
            throw new IllegalArgumentException("A LedgerProofWithContext cannot be constructed with a topPath with " +
                    (topPath.length < 5 ? "less" : "more") + " than five layers (" + topPath.length + " provided).");
        }

        if (topPath[0].length != 32) {
            throw new IllegalArgumentException("A LedgerProofWithContext cannot be constructed with a topPath[0] byte" +
                    " array containing a path which isn't a 32-byte hash!");
        }
        if (topPath[1].length != 32) {
            throw new IllegalArgumentException("A LedgerProofWithContext cannot be constructed with a topPath[1] byte" +
                    " array containing a path which isn't a 32-byte hash!");
        }
        if (topPath[2].length != 32) {
            throw new IllegalArgumentException("A LedgerProofWithContext cannot be constructed with a topPath[2] byte" +
                    " array containing a path which isn't a 32-byte hash!");
        }
        if (topPath[3].length != 8) {
            throw new IllegalArgumentException("A LedgerProofWithContext cannot be constructed with a topPath[3] byte" +
                    " array containing a path which isn't an 8-byte long!");
        }
        if (topPath[4].length != 32) {
            throw new IllegalArgumentException("A LedgerProofWithContext cannot be constructed with a topPath[4] byte" +
                    " array containing a path which isn't a 32-byte hash!");
        }

        if (containingHeader == null || containingHeader.length != 64) {
            throw new IllegalArgumentException("A LedgerProofWithContext requires a 64-byte containing header!");
        }

        this.topPath = new byte[topPath.length][];
        for (int i = 0; i < topPath.length; i++) {
            this.topPath[i] = new byte[topPath[i].length];
            System.arraycopy(topPath[i], 0, this.topPath[i], 0, topPath[i].length);
        }

        byte[] ledgerHash = new Crypto().SHA256ReturnBytes(ledgerProof.getLedgerHash());
        byte[] workingHash = new byte[ledgerHash.length];
        System.arraycopy(ledgerHash, 0, workingHash, 0, ledgerHash.length);

        // Ledger hash is the 4th entry in the block content metapackage hash structure
        int workingIndex = 3;

        Crypto crypto = new Crypto();

        // Calculate top-level hash based on the top path combined with the calculated ledger hash
        for (int i = 0; i < this.topPath.length; i++) {
            byte[] left = (workingIndex % 2 == 0 ? workingHash : topPath[i]);
            byte[] right = (workingIndex % 2 == 0 ? topPath[i] : workingHash);

            byte[] staging = new byte[left.length + right.length];

            System.arraycopy(left, 0, staging, 0, left.length);
            System.arraycopy(right, 0, staging, left.length, right.length);

            workingHash = crypto.SHA256ReturnBytes(staging);
            workingIndex /= 2;
        }

        // Finished calculating top-level root hash
        this.topRoot = new byte[16];

        System.arraycopy(workingHash, 0, this.topRoot, 0, this.topRoot.length);

        byte[] topLevelExtractedRoot = BlockUtility.extractMerkleRootFromBlockHeader(containingHeader);

        if (!Utility.byteArraysAreEqual(this.topRoot, topLevelExtractedRoot)) {
            throw new IllegalArgumentException("The provided topPath doesn't properly authenticate the provided " +
                    "LedgerProof for address " + ledgerProof.getAddress() +
                    " to the provided block header (" + Utility.bytesToHex(containingHeader) + ")!");
        }

        this.containingHeader = new byte[containingHeader.length];
        System.arraycopy(containingHeader, 0, this.containingHeader, 0, containingHeader.length);
    }

    public byte[] getLedgerHash() {
        return ledgerProof.getLedgerHash();
    }

    public String getLedgerAddress() {
        return ledgerProof.getAddress();
    }

    public LedgerProof getLedgerProof() {
        return ledgerProof;
    }

    public byte[] getTopRootHash() {
        byte[] copy = new byte[topRoot.length];
        System.arraycopy(topRoot, 0, copy, 0, topRoot.length);
        return copy;
    }

    public byte[] getContainingHeader() {
        byte[] copy = new byte[containingHeader.length];
        System.arraycopy(containingHeader, 0, copy, 0, containingHeader.length);
        return copy;
    }

    public VeriBlockMessages.LedgerProofWithContext.Builder getBuilder() {
        VeriBlockMessages.LedgerProofWithContext.Builder builder = VeriBlockMessages.LedgerProofWithContext.newBuilder();
        builder.setLedgerProof(ledgerProof.getMessageBuilder());

        for (int i = 0; i < topPath.length; i++) {
            builder.addLedgerProofContextLayers(ByteString.copyFrom(topPath[i]));
        }

        VeriBlockMessages.BlockHeader.Builder blockHeaderBuilder = VeriBlockMessages.BlockHeader.newBuilder();
        blockHeaderBuilder.setHeader(ByteString.copyFrom(containingHeader));
        blockHeaderBuilder.setHash(ByteString.copyFrom(Utility.hexToBytes(BlockUtility.hashBlock(containingHeader))));

        builder.setBlockHeader(blockHeaderBuilder);

        return builder;
    }

    public static LedgerProofWithContext parseFrom(VeriBlockMessages.LedgerProofWithContextOrBuilder message) {
        LedgerProof ledgerProof = LedgerProof.parseFrom(message.getLedgerProof());

        byte[][] contextLayers = message.getLedgerProofContextLayersList()
                .stream().map(ByteString::toByteArray)
                .collect(Collectors.toList())
                .toArray(new byte[message.getLedgerProofContextLayersCount()][]);

        byte[] containingHeader = message.getBlockHeader().getHeader().toByteArray();

        return new LedgerProofWithContext(ledgerProof, contextLayers, containingHeader);
    }
}
