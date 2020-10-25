// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.sdk

import org.junit.Assert
import org.junit.Test
import org.veriblock.core.bitcoinj.Base58
import org.veriblock.core.utilities.Utility
import org.veriblock.sdk.models.Constants
import org.veriblock.sdk.models.PublicationData
import org.veriblock.sdk.services.SerializeDeserializeService.parsePublicationData
import org.veriblock.sdk.services.SerializeDeserializeService.serialize
import org.veriblock.sdk.util.writeSingleByteLengthValue
import org.veriblock.sdk.util.writeVariableLengthValue
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.Base64

class PublicationDataTests {
    @Test
    fun parse() {
        val identifier = 1L
        val header = Base64.getDecoder().decode("AAATbQAC+QNCG0kCwUNJxXulsJNWN4YGed3VXuT9IQguGGhuQZwPGl6HY18fMkR2ONB77VybkYoHAhMwn88yVg==")
        val payoutInfo = Base58.decode("VB2zTVQH6JmjJJZTYwCcrDB9kAJp7G")
        val contextInfo = Base64.getDecoder().decode(
            "AAAAIPfeKZWJiACrEJr5Z3m5eaYHFdqb8ru3RbMAAAAAAAAA+FSGAmv06tijekKSUzLsi1U/jjEJdP6h66I4987mFl4iE7dchBoBGi4A8po="
        )
        var data: ByteArray? = null
        try {
            ByteArrayOutputStream().use { stream ->
                stream.writeSingleByteLengthValue(identifier)
                stream.writeVariableLengthValue(header)
                stream.writeVariableLengthValue(contextInfo)
                stream.writeVariableLengthValue(payoutInfo)
                data = stream.toByteArray()
            }
        } catch (e: IOException) {
            Assert.fail()
        }
        val pubData = parsePublicationData(data!!)
        Assert.assertEquals(identifier, pubData!!.identifier)
        Assert.assertArrayEquals(header, pubData.header)
        Assert.assertArrayEquals(payoutInfo, pubData.payoutInfo)
        Assert.assertArrayEquals(contextInfo, pubData.contextInfo)
    }

    @Test
    fun parseWhenInvalid() {
        val identifier: Long = 1
        var header = Utility.fillBytes(0.toByte(), Constants.MAX_HEADER_SIZE_PUBLICATION_DATA + 1)
        var payoutInfo = Utility.fillBytes(0.toByte(), 1)
        var contextInfo = Utility.fillBytes(0.toByte(), 1)
        var data = PublicationData(identifier, header!!, payoutInfo!!, contextInfo!!)
        var serialized = serialize(data)
        try {
            parsePublicationData(serialized)
            Assert.fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            Assert.assertTrue(e.message!!.startsWith("Unexpected length"))
        }
        header = Utility.fillBytes(0.toByte(), 1)
        payoutInfo = Utility.fillBytes(0.toByte(), Constants.MAX_PAYOUT_SIZE_PUBLICATION_DATA + 1)
        contextInfo = Utility.fillBytes(0.toByte(), 1)
        data = PublicationData(identifier, header, payoutInfo, contextInfo)
        serialized = serialize(data)
        try {
            parsePublicationData(serialized)
            Assert.fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            Assert.assertTrue(e.message!!.startsWith("Unexpected length"))
        }
        header = Utility.fillBytes(0.toByte(), 1)
        payoutInfo = Utility.fillBytes(0.toByte(), 1)
        contextInfo = Utility.fillBytes(0.toByte(), Constants.MAX_CONTEXT_SIZE_PUBLICATION_DATA + 1)
        data = PublicationData(identifier, header, payoutInfo, contextInfo)
        serialized = serialize(data)
        try {
            parsePublicationData(serialized)
            Assert.fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            Assert.assertTrue(e.message!!.startsWith("Unexpected length"))
        }
    }
}
