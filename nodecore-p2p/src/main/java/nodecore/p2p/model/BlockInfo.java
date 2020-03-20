// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.p2p.model;

public class BlockInfo {
    private int number;
    public int getNumber() {
        return number;
    }

    private String hash;
    public String getHash() {
        return hash;
    }

    public BlockInfo(int number, String hash) {
        this.number = number;
        this.hash = hash;
    }
}
