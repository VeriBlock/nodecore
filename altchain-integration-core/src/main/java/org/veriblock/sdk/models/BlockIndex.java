// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.sdk.models;

public class BlockIndex {
    protected final long height;
    protected final String hash;

    public long getHeight() {
        return height;
    }

    public String getHash() {
        return hash;
    }

    public BlockIndex(long height, String hash) {
        if (hash == null) throw new IllegalArgumentException("hash cannot be null");

        this.height = height;
        this.hash = hash;
    }
}
