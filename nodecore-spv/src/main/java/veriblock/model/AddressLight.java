// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package veriblock.model;

import java.io.IOException;
import java.io.OutputStream;

public abstract class AddressLight {
    private final String address;

    public final String get() {
        return address;
    }

    public abstract byte getType();

    protected AddressLight(String address) {
        this.address = address;
    }

    public abstract byte[] toByteArray();

    public final void serializeToStream(OutputStream stream) throws IOException {
        byte[] bytes = toByteArray();
        stream.write(getType());
        stream.write((byte) bytes.length);
        stream.write(bytes);
    }

    @Override
    public String toString() {
        return address;
    }

    @Override
    public int hashCode() {
        return address.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return address.equals(obj);
    }
}
