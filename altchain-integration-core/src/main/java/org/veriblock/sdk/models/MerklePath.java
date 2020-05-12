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

public class MerklePath {

    protected String compactFormat;
    protected List<Sha256Hash> layers;
    protected Sha256Hash subject;
    protected int index;

    public int getIndex() {
        return index;
    }

    public Sha256Hash getSubject() {
        return subject;
    }

    public List<Sha256Hash> getLayers() {
        return layers;
    }

    protected Sha256Hash merkleRoot;
    /**
     * Returns the Merkle root produced by following the layers up to the top of the tree.
     *
     * @return The Merkle root produced by following the path up to the top of the transaction tree, encoded in hexadecimal
     */
    public final Sha256Hash getMerkleRoot() {
        return this.merkleRoot;
    }

    protected MerklePath() {}

    public MerklePath(int index, Sha256Hash subject, List<Sha256Hash> layers) {
        this.index = index;
        this.subject = subject;
        this.layers = layers;

        List<String> layerStrings = layers.stream().map(Sha256Hash::toString).collect(Collectors.toList());
        this.compactFormat = String.format("%d:%s:%s", index, subject.toString(), String.join(":", layerStrings));
        this.merkleRoot = MerklePathUtil.calculateMerkleRoot(index, subject, layers);
    }

    public MerklePath(String compactFormat) {
        Preconditions.notNull(compactFormat, "Merkle path compact format cannot be null");

        String[] parts = compactFormat.split(":");
        Preconditions.argument(parts.length > 2 && Integer.parseInt(parts[0]) >= 0, "Invalid merkle path: " + compactFormat);

        this.index = Integer.parseInt(parts[0]);
        this.subject = Sha256Hash.wrap(parts[1]);
        this.layers = new ArrayList<>();
        for (int i = 2; i < parts.length; i++) {
            this.layers.add(Sha256Hash.wrap(parts[i]));
        }

        this.merkleRoot = MerklePathUtil.calculateMerkleRoot(index, subject, layers);
        this.compactFormat = compactFormat;
    }

    public String toCompactString() {
        return compactFormat;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o != null && getClass() == o.getClass() &&
                toCompactString().equals(((MerklePath) o).toCompactString());
    }

}
