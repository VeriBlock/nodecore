// VeriBlock NodeCore
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.events;

import nodecore.miners.pop.contracts.VeriBlockHeader;

public class NewVeriBlockFoundEvent {
    private final VeriBlockHeader block;
    public VeriBlockHeader getBlock() {
        return block;
    }

    private final VeriBlockHeader previousHead;
    public VeriBlockHeader getPreviousHead() {
        return previousHead;
    }

    public NewVeriBlockFoundEvent(VeriBlockHeader block, VeriBlockHeader previousHead) {
        this.block = block;
        this.previousHead = previousHead;
    }
}