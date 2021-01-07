// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.sdk;

import org.junit.Assert;
import org.junit.Test;
import org.veriblock.core.bitcoinj.Base58;
import org.veriblock.sdk.models.Constants;
import org.veriblock.sdk.models.PublicationData;
import org.veriblock.sdk.services.SerializeDeserializeService;
import org.veriblock.core.utilities.Utility;
import org.veriblock.sdk.util.StreamUtilsKt;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

public class PublicationDataTests {
    @Test
    public void parse() {
        long identifier = 1L;
        byte[] header = Base64.getDecoder().decode("AAATbQAC+QNCG0kCwUNJxXulsJNWN4YGed3VXuT9IQguGGhuQZwPGl6HY18fMkR2ONB77VybkYoHAhMwn88yVg==");
        byte[] payoutInfo = Base58.decode("VB2zTVQH6JmjJJZTYwCcrDB9kAJp7G");
        byte[] contextInfo = Base64.getDecoder().decode("AAAAIPfeKZWJiACrEJr5Z3m5eaYHFdqb8ru3RbMAAAAAAAAA+FSGAmv06tijekKSUzLsi1U/jjEJdP6h66I4987mFl4iE7dchBoBGi4A8po=");

        byte[] data = null;
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
            StreamUtilsKt.writeSingleByteLengthValue(stream, identifier);
            StreamUtilsKt.writeVariableLengthValue(stream, header);
            StreamUtilsKt.writeVariableLengthValue(stream, contextInfo);
            StreamUtilsKt.writeVariableLengthValue(stream, payoutInfo);

            data = stream.toByteArray();
        } catch (IOException e) {
            Assert.fail();
        }

        PublicationData pubData = SerializeDeserializeService.parsePublicationData(data);
        Assert.assertEquals(identifier, pubData.getIdentifier());
        Assert.assertArrayEquals(header, pubData.getHeader());
        Assert.assertArrayEquals(payoutInfo, pubData.getPayoutInfo());
        Assert.assertArrayEquals(contextInfo, pubData.getContextInfo());
    }
    
    @Test
    public void parseWhenInvalid() {    
        long identifier = 1;
        byte[] header = Utility.fillBytes((byte) 0, Constants.MAX_HEADER_SIZE_PUBLICATION_DATA + 1);
        byte[] payoutInfo = Utility.fillBytes((byte) 0, 1);
        byte[] contextInfo = Utility.fillBytes((byte) 0, 1);
        PublicationData data = new PublicationData(identifier, header, payoutInfo, contextInfo);
        
        byte[] serialized = SerializeDeserializeService.INSTANCE.serialize(data);
        
        try {
            SerializeDeserializeService.parsePublicationData(serialized);
            Assert.fail("Expected IllegalArgumentException");
        } catch(IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().startsWith("Unexpected length"));
        }
        
        header = Utility.fillBytes((byte) 0, 1);
        payoutInfo = Utility.fillBytes((byte) 0, Constants.MAX_PAYOUT_SIZE_PUBLICATION_DATA + 1);
        contextInfo = Utility.fillBytes((byte) 0, 1);
        data = new PublicationData(identifier, header, payoutInfo, contextInfo);
        
        serialized = SerializeDeserializeService.INSTANCE.serialize(data);
        
        try {
            SerializeDeserializeService.parsePublicationData(serialized);
            Assert.fail("Expected IllegalArgumentException");
        } catch(IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().startsWith("Unexpected length"));
        }
        
        header = Utility.fillBytes((byte) 0, 1);
        payoutInfo = Utility.fillBytes((byte) 0, 1);
        contextInfo = Utility.fillBytes((byte) 0, Constants.MAX_CONTEXT_SIZE_PUBLICATION_DATA + 1);
        data = new PublicationData(identifier, header, payoutInfo, contextInfo);
        
        serialized = SerializeDeserializeService.INSTANCE.serialize(data);
        
        try {
            SerializeDeserializeService.parsePublicationData(serialized);
            Assert.fail("Expected IllegalArgumentException");
        } catch(IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().startsWith("Unexpected length"));
        }
    }
}
