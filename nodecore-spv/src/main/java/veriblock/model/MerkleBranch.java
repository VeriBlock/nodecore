// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package veriblock.model;

import org.veriblock.sdk.models.MerklePath;
import org.veriblock.sdk.models.Sha256Hash;
import veriblock.util.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MerkleBranch {

    private final boolean regular;
    private final MerklePath merklePath;

    public MerkleBranch(int index, Sha256Hash subject, List<Sha256Hash> merklePathHashes, boolean regular) {
        this.regular = regular;
        this.merklePath = new MerklePath(index, subject, merklePathHashes);
    }

    public boolean isRegular() {
        return regular;
    }

    public int getIndex() {
        return merklePath.getIndex();
    }

    public Sha256Hash getSubject() {
        return merklePath.getSubject();
    }

    public List<Sha256Hash> getMerklePathHashes() {
        return merklePath.getLayers();
    }

    public boolean verify(Sha256Hash root) {
        Sha256Hash calculated = getSubject();

        for (int j = 0; j < getMerklePathHashes().size(); j++) {
            if (j == getMerklePathHashes().size() - 1) {
                // Transactions always come from the right child
                calculated = Utils.hash(getMerklePathHashes().get(j), calculated);
            } else if (j == getMerklePathHashes().size() - 2) {
                // Transaction root calculated is determined by the regular flag
                if (regular) {
                    calculated = Utils.hash(getMerklePathHashes().get(j), calculated);
                } else {
                    calculated = Utils.hash(calculated, getMerklePathHashes().get(j));
                }
            } else {
                int idx = merklePath.getIndex() >> j;
                if (idx % 2 == 0) {
                    calculated = Utils.hash(calculated, getMerklePathHashes().get(j));
                } else {
                    calculated = Utils.hash(getMerklePathHashes().get(j), calculated);
                }
            }
        }

        return Utils.matches(calculated, root);
    }

    @Override
    public String toString() {
        List<String> hashes = new ArrayList<>();
        hashes.add(merklePath.getSubject().toString());
        hashes.addAll(getMerklePathHashes().stream().map(Sha256Hash::toString).collect(Collectors.toList()));

        return String.join(":", hashes);
    }
}
