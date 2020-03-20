// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package veriblock.model;

public class SigningResult {
    private byte[] publicKey;
    private byte[] signature;
    private boolean succeeded;

    public SigningResult(boolean succeeded, byte[] signature, byte[] publicKey) {
        this.succeeded = succeeded;
        this.signature = signature;
        this.publicKey = publicKey;
    }

    public boolean succeeded() {
        return succeeded;
    }

    public byte[] getSignature() {
        return signature;
    }

    public byte[] getPublicKey() {
        return publicKey;
    }
}
