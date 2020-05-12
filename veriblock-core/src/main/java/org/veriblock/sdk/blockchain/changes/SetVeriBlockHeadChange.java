// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.sdk.blockchain.changes;

import org.veriblock.sdk.auditor.Change;
import org.veriblock.sdk.auditor.Operation;
import org.veriblock.sdk.blockchain.store.StoredVeriBlockBlock;
import org.veriblock.sdk.models.Constants;

public class SetVeriBlockHeadChange extends Change {
    @Override
    public String getChainIdentifier() {
        return Constants.VERIBLOCK_HEADER_MAGIC;
    }

    @Override
    public Operation getOperation() {
        return Operation.SET_HEAD;
    }

    public SetVeriBlockHeadChange(StoredVeriBlockBlock oldValue, StoredVeriBlockBlock newValue) {
        super(oldValue != null ? oldValue.serialize() : new byte[]{},
                newValue != null ? newValue.serialize() : new byte[]{});
    }
}
