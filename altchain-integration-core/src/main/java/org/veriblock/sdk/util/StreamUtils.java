// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.sdk.util;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class StreamUtils {
    public static void writeSingleByteLengthValueToStream(OutputStream stream, int value) throws IOException {
        byte[] trimmed = Utils.trimmedByteArrayFromInteger(value);
        stream.write(trimmed.length);
        stream.write(trimmed);
    }

    public static void writeSingleByteLengthValueToStream(OutputStream stream, long value) throws IOException {
        byte[] trimmed = Utils.trimmedByteArrayFromLong(value);
        stream.write(trimmed.length);
        stream.write(trimmed);
    }

    public static void writeSingleByteLengthValueToStream(OutputStream stream, byte[] value) throws IOException {
        stream.write((byte)value.length);
        stream.write(value);
    }
    
    public static void writeSingleIntLengthValueToStream(OutputStream stream, int value) throws IOException {
    	byte[] valueBytes = Utils.toByteArray(value);
        writeSingleByteLengthValueToStream(stream, valueBytes);
    }
    
    public static void writeVariableLengthValueToStream(OutputStream stream, byte[] value) throws IOException {
        byte[] dataSize = Utils.trimmedByteArrayFromInteger(value.length);
        stream.write((byte)dataSize.length);
        stream.write(dataSize);
        stream.write(value);
    }
    

    public static byte[] getSingleByteLengthValue(ByteBuffer buffer, int minLength, int maxLength) {
        int length = buffer.get();
        checkLength(length, minLength, maxLength);
        
        byte[] value = new byte[length];
        buffer.get(value);

        return value;
    }
    
    public static byte[] getSingleByteLengthValue(ByteBuffer buffer) {
        return getSingleByteLengthValue(buffer, 0, 255);
    }
    
    public static int getSingleIntValue(ByteBuffer buffer) {
        return Utils.toInt(getSingleByteLengthValue(buffer, 4, 4));
    }

    public static byte[] getVariableLengthValue(ByteBuffer buffer, int minLength, int maxLength) {
        byte lengthLength = buffer.get();
        checkLength(lengthLength, 0, 4);
        
        byte[] lengthBytes = new byte[4];
        buffer.get(lengthBytes, 4 - lengthLength, lengthLength);

        int length = ByteBuffer.wrap(lengthBytes).getInt();
        checkLength(length, minLength, maxLength);
        
        byte[] value = new byte[length];
        buffer.get(value);

        return value;
    }
    
    public static byte[] getVariableLengthValue(ByteBuffer buffer) {  
        return getVariableLengthValue(buffer, 0, Integer.MAX_VALUE);
    }
    
    private static void checkLength(int length, int minLength, int maxLength)
    {
        if (length < minLength || length > maxLength)
            throw new IllegalArgumentException("Unexpected length: " + length
                + " (expected a value between " + minLength
                + " and " + maxLength + ")");
    }
}
