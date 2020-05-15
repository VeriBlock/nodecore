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
import org.veriblock.sdk.models.Address;
import org.veriblock.sdk.services.SerializeDeserializeService;

import java.nio.ByteBuffer;
import java.util.Base64;

public class AddressTests {
    @Test
    public void construct_WhenValidStandard() {
        final String address = "VFFDWUMLJwLRuNzH4NX8Rm32E59n6d";

        Address test = new Address(address);
        Assert.assertFalse(test.isMultisig());
        Assert.assertEquals(address, test.toString());
    }

    @Test
    public void construct_WhenValidMultisig() {
        final String address = "V23Cuyc34u5rdk9psJ86aFcwhB1md0";

        Address test = new Address(address);
        Assert.assertTrue(test.isMultisig());
        Assert.assertEquals(address, test.toString());
    }

    @Test
    public void isDerivedFromPublicKey_WhenItIs() {
        byte[] publicKey = Base64.getDecoder().decode("MFYwEAYHKoZIzj0CAQYFK4EEAAoDQgAEy0J+QaARSHQICkseKreSDiLNLRiMhxQN76RH7l/ES7hI4cDbXvIG3i5wAvbIaVK+SCOkwI5l5M2+uQSouVdjqg==");

        Address test = new Address("VFFDWUMLJwLRuNzH4NX8Rm32E59n6d");
        Assert.assertTrue(test.isDerivedFromPublicKey(publicKey));
    }

    @Test
    public void isDerivedFromPublicKey_WhenItIsNot() {
        byte[] publicKey = Base64.getDecoder().decode("MFYwEAYHKoZIzj0CAQYFK4EEAAoDQgAEy0J+QaARSHQICkseKreSDiLNLRiMhxQN76RH7l/ES7hI4cDbXvIG3i5wAvbIaVK+SCOkwI5l5M2+uQSouVdjqg==");

        Address test = new Address("V23Cuyc34u5rdk9psJ86aFcwhB1md0");
        Assert.assertFalse(test.isDerivedFromPublicKey(publicKey));
    }

    @Test
    public void parse_WhenStandard() {
        final String address = "VFFDWUMLJwLRuNzH4NX8Rm32E59n6d";

        Address input = new Address(address);
        byte[] serialized = SerializeDeserializeService.serialize(input);
        Address deserialized = SerializeDeserializeService.parseAddress(ByteBuffer.wrap(serialized));

        Assert.assertEquals(input, deserialized);
        Assert.assertFalse(deserialized.isMultisig());
    }

    @Test
    public void parse_WhenMultisig() {
        final String address = "V23Cuyc34u5rdk9psJ86aFcwhB1md0";

        Address input = new Address(address);
        byte[] serialized = SerializeDeserializeService.serialize(input);
        Address deserialized = SerializeDeserializeService.parseAddress(ByteBuffer.wrap(serialized));

        Assert.assertEquals(input, deserialized);
        Assert.assertTrue(deserialized.isMultisig());
    }
    
    @Test
    public void construct_WhenInvalid() {
        final String address = "VFFDWUMLJwLRuNzH4NX8Rm32E59n6dddd";

        try {
            new Address(address);
            Assert.fail("Expected IllegalArgumentException");
        } catch(IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().startsWith("The address"));
        }
    }
}
