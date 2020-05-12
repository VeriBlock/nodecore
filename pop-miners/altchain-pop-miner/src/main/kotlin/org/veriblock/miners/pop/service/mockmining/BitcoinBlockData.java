// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.service.mockmining;

import org.veriblock.sdk.models.MerklePath;
import org.veriblock.sdk.models.Sha256Hash;

import java.util.ArrayList;
import java.util.List;

public class BitcoinBlockData extends ArrayList<byte[]> {

    public Sha256Hash getMerkleRoot() {
        return calculateSubtreeHash(0, 0);
    }

    // calculate the number of bits it takes to store size()
    private int getMaxDepth() {
        return (int)(Math.log(size()) / Math.log(2) + 1);
    }

    // at each depth, there are 2**depth subtrees
    // leaves are at the depth equal to getMaxDepth()
    private Sha256Hash calculateSubtreeHash(int index, int depth) {
        if (depth >= getMaxDepth()) {
            return Sha256Hash.twiceOf(index < size() ? get(index) : new byte[0]);
        }

        return Sha256Hash.twiceOf(calculateSubtreeHash(index * 2, depth + 1).getBytes(),
                                  calculateSubtreeHash(index * 2 + 1, depth + 1).getBytes());
    }

    public MerklePath getMerklePath(int index) {
        if (index >= size())
            throw new IndexOutOfBoundsException("index must be less than size()");

        int maxDepth = getMaxDepth();
        int layerIndex = index;

        Sha256Hash subject = calculateSubtreeHash(index, maxDepth);
        List<Sha256Hash> layers = new ArrayList<>(maxDepth);

        for (int depth = maxDepth; depth > 0; depth--) {
            // invert the last bit of layerIndex to reach the opposite subtree
            Sha256Hash layer = calculateSubtreeHash(layerIndex ^ 1, depth);
            layers.add(layer);

            layerIndex /= 2;
        }

        return new MerklePath(index, subject, layers);
    }
}
