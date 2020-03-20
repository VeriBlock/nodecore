// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package veriblock.util;

import java.io.IOException;
import java.io.InputStream;

public class ByteSerialization {
    public static int readVariableLengthIntegerFromStream(InputStream stream) throws IOException {
        byte length = (byte)stream.read();
        if (length == (byte)0) return 0;

        byte[] data = new byte[length];
        stream.read(data);

        int value = 0;
        for (int i = 0; i < length; i++) {
            value <<= 8;
            value |= 0xFF & data[i];
        }

        return value;
    }
    public static long readVariableLengthLongFromStream(InputStream stream) throws IOException {
        byte length = (byte)stream.read();
        byte[] data = new byte[length];
        stream.read(data);

        long value = 0L;
        for (int i = 0; i < length; i++) {
            value <<= 8;
            value |= 0xFF & data[i];
        }

        return value;
    }

    public static byte[] readVariableLengthBytesFromStream(InputStream stream) throws IOException {
        int length = readVariableLengthIntegerFromStream(stream);
        if (length == 0) return null;

        byte[] value = new byte[length];
        stream.read(value);

        return value;
    }
}
