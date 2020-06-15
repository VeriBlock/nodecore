// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package veriblock.lite.core

import org.junit.Assert
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import java.io.IOException
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel

class NIOClientTest {

    private lateinit var client: SocketChannel

    @Before
    fun setUp() {
        try {
            client = SocketChannel.open(InetSocketAddress("localhost", 7500))
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    @Test
    @Ignore
    fun name() {
        val buffer = ByteBuffer.wrap("Test message".toByteArray())
        var response: String? = null
        try {
            client.write(buffer)
            buffer.clear()
            client.read(buffer)
            response = String(buffer.array()).trim { it <= ' ' }
            println("response=$response")
            buffer.clear()
        } catch (e: IOException) {
            Assert.fail()
        }
        println(response)
    }

}
