package org.veriblock.sdk.util;

import java.nio.ByteBuffer;

public class BytesUtility {
    public static short readBEInt16(ByteBuffer buffer) {
        return buffer.getShort();
    }

    public static int readBEInt32(ByteBuffer buffer) {
        return buffer.getInt();
    }

    public static int readLEInt32(ByteBuffer buffer) {
        return Integer.reverseBytes(buffer.getInt());
    }

    public static void putBEInt16(ByteBuffer buffer, short value) {
        buffer.putShort(value);
    }

    public static void putBEInt32(ByteBuffer buffer, int value) {
        buffer.putInt(value);
    }

    public static void putBEBytes(ByteBuffer buffer, byte[] value) {
        buffer.put(value);
    }

    public static void putLEBytes(ByteBuffer buffer, byte[] value) {
        buffer.put(org.veriblock.core.utilities.Utility.flip(value));
    }

    public static void putLEInt32(ByteBuffer buffer, int value) {
        buffer.putInt(Integer.reverseBytes(value));
    }
}
