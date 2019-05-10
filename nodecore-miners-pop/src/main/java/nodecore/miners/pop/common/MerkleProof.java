// VeriBlock NodeCore
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.common;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.bitcoinj.core.PartialMerkleTree;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.VerificationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.bitcoinj.core.Utils.checkBitLE;
import static org.bitcoinj.core.Utils.reverseBytes;

public class MerkleProof {
    private static final Logger logger = LoggerFactory.getLogger(MerkleProof.class);

    private final List<Sha256Hash> hashes;
    private final byte[] matchedChildBits;
    private final int transactionCount;

    private Sha256Hash[][] tree;
    private Map<Sha256Hash, Integer> positions;

    public MerkleProof(List<Sha256Hash> hashes, byte[] bits, int transactionCount) {
        this.hashes = hashes;
        this.matchedChildBits = bits;
        this.transactionCount = transactionCount;

        buildTree();
    }

    public String getCompactPath(Sha256Hash txId) {
        if (positions == null || tree == null) throw new IllegalStateException("Tree has not been initialized");

        Integer txPosition = positions.get(txId);
        if (txPosition == null) {
            return null;
        }

        StringBuilder path = new StringBuilder(txPosition + ":" + Utility.bytesToHex(txId.getReversedBytes()));

        int pos = txPosition;
        /* Fill up the path with the corresponding nodes */
        for (int i = 0; i < tree.length; i++)
        {
            int elementIndex = (pos % 2 == 0) ? pos + 1 : pos - 1;
            if (elementIndex == tree[i].length) {
                elementIndex = elementIndex - 1;
            }

            /* Get the complementary element (left or right) at the next layer */
            path.append(":").append(Utility.bytesToHex(get(i, elementIndex).getReversedBytes()));

            /* Index in above layer will be floor(foundIndex / 2) */
            pos /= 2;
        }

        return path.toString();
    }

    private void buildTree() {
        int height = 0;
        int width;
        List<Integer> widths = new ArrayList<>();
        while ((width = getTreeWidth(transactionCount, height)) > 1) {
            widths.add(height, width);
            height++;
        }

        tree = new Sha256Hash[height][];
        positions = new HashMap<>();
        for (int i = 0; i < widths.size(); i++) {
            tree[i] = new Sha256Hash[widths.get(i)];
        }

        ValuesUsed used = new ValuesUsed();
        recursiveExtractHashes(height, 0, used);

        if ((used.bitsUsed+7)/8 != matchedChildBits.length ||
                // verify that all hashes were consumed
                used.hashesUsed != hashes.size())
            throw new VerificationException("Got a CPartialMerkleTree that didn't need all the data it provided");
    }

    private Sha256Hash get(int height, int offset) {
        if (tree[height].length == offset) {
            offset -= 1;
        }

        Sha256Hash element = tree[height][offset];
        if (element != null) return element;

        // Return a ZERO HASH because we can't descend any further
        if (height == 0) return Sha256Hash.ZERO_HASH;

        return combineLeftRight(get(height - 1, offset * 2).getBytes(), get(height - 1, (offset * 2) + 1).getBytes());
    }

    // Below code replicated from bitcoinj

    // helper function to efficiently calculate the number of nodes at given height in the merkle tree
    private static int getTreeWidth(int transactionCount, int height) {
        return (transactionCount + (1 << height) - 1) >> height;
    }

    private static class ValuesUsed {
        public int bitsUsed = 0, hashesUsed = 0;
    }

    // recursive function that traverses tree nodes, consuming the bits and hashes produced by TraverseAndBuild.
    // it returns the hash of the respective node.
    private Sha256Hash recursiveExtractHashes(int height, int pos, ValuesUsed used) throws VerificationException {
        if (used.bitsUsed >= matchedChildBits.length*8) {
            // overflowed the bits array - failure
            throw new VerificationException("PartialMerkleTree overflowed its bits array");
        }
        boolean parentOfMatch = checkBitLE(matchedChildBits, used.bitsUsed++);
        if (height == 0 || !parentOfMatch) {
            // if at height 0, or nothing interesting below, use stored hash and do not descend
            if (used.hashesUsed >= hashes.size()) {
                // overflowed the hash array - failure
                throw new VerificationException("PartialMerkleTree overflowed its hash array");
            }
            Sha256Hash hash = hashes.get(used.hashesUsed++);
            tree[height][pos] = hash;
            if (height == 0 && parentOfMatch) {
                positions.put(hash, pos);
            }
            return hash;
        } else {
            // otherwise, descend into the subtrees to extract matched txids and hashes
            byte[] left = recursiveExtractHashes(height - 1, pos * 2, used).getBytes(), right;
            if (pos * 2 + 1 < getTreeWidth(transactionCount, height-1)) {
                right = recursiveExtractHashes(height - 1, pos * 2 + 1, used).getBytes();
                if (Arrays.equals(right, left))
                    throw new VerificationException("Invalid merkle tree with duplicated left/right branches");
            } else {
                right = left;
            }
            // and combine them before returning
            return combineLeftRight(left, right);
        }
    }

    private static Sha256Hash combineLeftRight(byte[] left, byte[] right) {
        return Sha256Hash.wrapReversed(Sha256Hash.hashTwice(
                reverseBytes(left), 0, 32,
                reverseBytes(right), 0, 32));
    }

    @SuppressWarnings("unchecked")
    public static MerkleProof parse(PartialMerkleTree partialMerkleTree) {
        try {
            List<Sha256Hash> hashes = (List<Sha256Hash>) FieldUtils.readField(partialMerkleTree, "hashes", true);
            byte[] bits = (byte[])FieldUtils.readField(partialMerkleTree, "matchedChildBits", true);

            return new MerkleProof(hashes, bits, partialMerkleTree.getTransactionCount());
        } catch (IllegalAccessException e) {
            logger.error("Unable to parse Partial Merkle Tree", e);
        }
        return null;
    }
}
