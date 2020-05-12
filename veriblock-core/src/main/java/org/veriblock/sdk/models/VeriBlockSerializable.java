// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.sdk.models;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public abstract class VeriBlockSerializable {
    public byte[] serialize() {
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
            serializeToStream(stream);

            return stream.toByteArray();
        } catch (IOException ignore) {
            // Should not happen
        }

        return new byte[] {};
    }

    public abstract void serializeToStream(OutputStream stream) throws IOException;
}
