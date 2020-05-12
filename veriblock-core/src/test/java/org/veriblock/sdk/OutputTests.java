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
import org.veriblock.sdk.models.Output;
import org.veriblock.sdk.services.SerializeDeserializeService;

import java.nio.ByteBuffer;

public class OutputTests {
    @Test
    public void parse() {
        Output input = Output.of("VFFDWUMLJwLRuNzH4NX8Rm32E59n6d", 1234567890L);
        byte[] serialized = SerializeDeserializeService.serialize(input);
        Output deserialized = SerializeDeserializeService.parseOutput(ByteBuffer.wrap(serialized));

        Assert.assertEquals(input, deserialized);
    }
}
