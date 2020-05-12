// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.sdk.models;

import org.veriblock.sdk.util.MerklePathUtil;
import org.veriblock.sdk.util.Preconditions;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class VeriBlockMerklePath extends MerklePath {
    private final int treeIndex;
    private String compactFormat;
    private List<Sha256Hash> layers;
    private Sha256Hash subject;
    private int index;

    public VeriBlockMerklePath(int treeIndex, int index, Sha256Hash subject, List<Sha256Hash> layers) {
        this.treeIndex = treeIndex;
        this.index = index;
        this.subject = subject;
        this.layers = layers;

        List<String> layerStrings = layers.stream().map(Sha256Hash::toString).collect(Collectors.toList());
        this.compactFormat = String.format("%d:%d:%s:%s", treeIndex, index, subject.toString(), String.join(":", layerStrings));
        merkleRoot = MerklePathUtil.calculateVeriMerkleRoot(this);
    }

    public VeriBlockMerklePath(String compactFormat) {
        Preconditions.notNull(compactFormat, "Merkle path compact format cannot be null");

        String[] parts = compactFormat.split(":");
        Preconditions.argument(parts.length > 3 && Integer.parseInt(parts[0]) >= 0 && Integer.parseInt(parts[1]) >= 0,
                "Invalid merkle path compact format: " + compactFormat);

        this.treeIndex = Integer.parseInt(parts[0]);
        this.index = Integer.parseInt(parts[1]);
        this.subject = Sha256Hash.wrap(parts[2]);
        this.layers = new ArrayList<>(parts.length - 3);

        for (int i = 3; i < parts.length; i++) {
            this.layers.add(Sha256Hash.wrap(parts[i]));
        }

        this.compactFormat = compactFormat;
        merkleRoot = MerklePathUtil.calculateVeriMerkleRoot(this);
    }

    public int getTreeIndex() {
        return treeIndex;
    }

    public String toCompactString() {
        return compactFormat;
    }

    public List<Sha256Hash> getLayers() {
        return layers;
    }

    public Sha256Hash getSubject() {
        return subject;
    }

    public int getIndex() {
        return index;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o != null && getClass() == o.getClass() &&
                toCompactString().equals(((VeriBlockMerklePath) o).toCompactString());
    }
}
