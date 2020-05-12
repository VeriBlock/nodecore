// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.sdk;

import org.junit.Assert;
import org.junit.Test;
import org.veriblock.sdk.models.Coin;
import org.veriblock.sdk.services.SerializeDeserializeService;
import org.veriblock.sdk.util.StreamUtils;
import org.veriblock.sdk.util.Utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class CoinTests {
    @Test
    public void parse() {
        Coin input = Coin.valueOf(123456789L);
        byte[] serialized = SerializeDeserializeService.serialize(input);
        Coin deserialized = Coin.parse(ByteBuffer.wrap(serialized));

        Assert.assertEquals(input, deserialized);
    }

    @Test
    public void parseWhenInvalid() throws IOException {
        byte[] array = Utils.fillBytes((byte) 0xFF, 9);

        try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
            StreamUtils.writeSingleByteLengthValueToStream(stream, array);
            ByteBuffer buffer = ByteBuffer.wrap(stream.toByteArray());
            Coin.parse(buffer);
            Assert.fail("Expected IllegalArgumentException");
        } catch(IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().startsWith("Unexpected length"));
        }
    }
}
