// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.sdk.models;

import org.veriblock.sdk.util.Preconditions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AltPublication {
    public final static int MAX_CONTEXT_COUNT = 15000;
    
    private final VeriBlockTransaction transaction;
    private final VeriBlockMerklePath merklePath;
    private final VeriBlockBlock containingBlock;
    private final List<VeriBlockBlock> context;

    public VeriBlockTransaction getTransaction() {
        return transaction;
    }

    public VeriBlockMerklePath getMerklePath() {
        return merklePath;
    }

    public VeriBlockBlock getContainingBlock() {
        return containingBlock;
    }

    public List<VeriBlockBlock> getContext() {
        return context;
    }

    public List<VeriBlockBlock> getBlocks() {
        List<VeriBlockBlock> blocks = new ArrayList<>();
        if (getContext() != null) {
            blocks.addAll(getContext());
        }

        if (getContainingBlock() != null) {
            blocks.add(getContainingBlock());
        }

        return blocks;
    }

    public VeriBlockBlock getFirstBlock() {
        List<VeriBlockBlock> blocks = getBlocks();
        if (blocks.size() > 0) {
            return blocks.get(0);
        }

        return null;
    }

    public AltPublication(VeriBlockTransaction transaction,
                          VeriBlockMerklePath merklePath,
                          VeriBlockBlock containingBlock,
                          List<VeriBlockBlock> context) {
        Preconditions.notNull(transaction, "Transaction cannot be null");
        Preconditions.notNull(merklePath, "Merkle path cannot be null");
        Preconditions.notNull(containingBlock, "Containing block cannot be null");

        this.transaction = transaction;
        this.merklePath = merklePath;
        this.containingBlock = containingBlock;
        this.context = context != null ? context : Collections.emptyList();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        AltPublication obj = (AltPublication)o;

        return transaction.equals(obj.transaction) &&
                merklePath.equals(obj.merklePath) &&
                containingBlock.equals(obj.containingBlock);
    }
}
