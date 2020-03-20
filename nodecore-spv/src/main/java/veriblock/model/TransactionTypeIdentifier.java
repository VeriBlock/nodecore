// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package veriblock.model;

public enum TransactionTypeIdentifier {
    STANDARD((byte)0x01),
    PROOF_OF_PROOF((byte)0x02),
    MULTISIG((byte)0x03);

    private final byte id;
    TransactionTypeIdentifier(byte id) { this.id = id; }


    public byte id() { return id; }
}
