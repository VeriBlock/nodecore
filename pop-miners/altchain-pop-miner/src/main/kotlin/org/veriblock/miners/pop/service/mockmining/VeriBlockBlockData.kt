// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.service.mockmining;

import org.veriblock.sdk.models.Sha256Hash;
import org.veriblock.sdk.models.VeriBlockMerklePath;
import org.veriblock.sdk.models.VeriBlockPoPTransaction;
import org.veriblock.sdk.models.VeriBlockTransaction;
import org.veriblock.sdk.services.SerializeDeserializeService;

import java.util.ArrayList;
import java.util.List;

public class VeriBlockBlockData {

    private abstract class Subtree<T> extends ArrayList<T> {
        abstract public Sha256Hash getSubject(int index);

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
                return index < size() ? getSubject(index) : Sha256Hash.of(new byte[0]);
            }

            return Sha256Hash.of(calculateSubtreeHash(index * 2, depth + 1).getBytes(),
                                 calculateSubtreeHash(index * 2 + 1, depth + 1).getBytes());
        }

        public List<Sha256Hash> getMerkleLayers(int index) {
            if (index >= size())
                throw new IndexOutOfBoundsException("index must be less than size()");

            int maxDepth = getMaxDepth();
            int layerIndex = index;

            // 2 layers will be added by get*MerklePath()
            List<Sha256Hash> layers = new ArrayList<>(maxDepth + 2);

            for (int depth = maxDepth; depth > 0; depth--) {
                // invert the last bit of layerIndex to reach the opposite subtree
                Sha256Hash layer = calculateSubtreeHash(layerIndex ^ 1, depth);
                layers.add(layer);

                layerIndex /= 2;
            }

            return layers;
        }

    }

    public class RegularSubtree extends Subtree<VeriBlockTransaction> {
        public Sha256Hash getSubject(int index) {
            return SerializeDeserializeService.getId(get(index));
        }
    }

    public class PoPSubtree extends Subtree<VeriBlockPoPTransaction> {
        public Sha256Hash getSubject(int index) {
            return SerializeDeserializeService.getId(get(index));
        }
    }

    private RegularSubtree txs = new RegularSubtree();
    private PoPSubtree popTxs = new PoPSubtree();

    private byte[] blockContentMetapackage = new byte[0];

    public Sha256Hash getMerkleRoot() {
        return Sha256Hash.of(
                    Sha256Hash.of(blockContentMetapackage).getBytes(),
                    Sha256Hash.of(txs.getMerkleRoot().getBytes(),
                                  popTxs.getMerkleRoot().getBytes()).getBytes());
    }

    public RegularSubtree getRegularTransactions() {
        return txs;
    }

    public PoPSubtree getPoPTransactions() {
        return popTxs;
    }

    public void setBlockContentMetapackage(byte[] data) {
        blockContentMetapackage = data;
    }

    public byte[] getBlockContentMetapackage() {
        return blockContentMetapackage;
    }

    public VeriBlockMerklePath getRegularMerklePath(int index) {
        Sha256Hash subject = txs.getSubject(index);

        List<Sha256Hash> layers = txs.getMerkleLayers(index);

        // the other transaction subtree
        layers.add(popTxs.getMerkleRoot());

        layers.add(Sha256Hash.of(blockContentMetapackage));

        return new VeriBlockMerklePath(0, index, subject, layers);
    }

    public VeriBlockMerklePath getPoPMerklePath(int index) {
        Sha256Hash subject = popTxs.getSubject(index);

        List<Sha256Hash> layers =  popTxs.getMerkleLayers(index);

        // the other transaction subtree
        layers.add(txs.getMerkleRoot());

        layers.add(Sha256Hash.of(blockContentMetapackage));

        return new VeriBlockMerklePath(1, index, subject, layers);
    }

}
