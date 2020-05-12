// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.sdk.auditor;

public class ReadOnlyChange extends Change {
    private final String chainIdentifier;
    private final Operation operation;

    @Override
    public String getChainIdentifier() {
        return chainIdentifier;
    }

    @Override
    public Operation getOperation() {
        return operation;
    }

    public ReadOnlyChange(String chainIdentifier, Operation operation, byte[] oldValue, byte[] newValue) {
        super(oldValue, newValue);

        this.chainIdentifier = chainIdentifier;
        this.operation = operation;
    }

}