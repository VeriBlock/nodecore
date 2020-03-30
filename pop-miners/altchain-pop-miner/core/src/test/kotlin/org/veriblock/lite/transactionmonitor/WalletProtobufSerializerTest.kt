// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.lite.core

import io.kotlintest.shouldBe
import org.junit.Test
import org.veriblock.core.utilities.Configuration
import org.veriblock.lite.params.NetworkConfig
import org.veriblock.lite.params.NetworkParameters
import org.veriblock.lite.transactionmonitor.readTransactionMonitor
import org.veriblock.lite.transactionmonitor.writeTransactionMonitor
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class WalletProtobufSerializerTest {
    private val networkParameters = NetworkParameters(NetworkConfig())
    private val context = Context(Configuration(), networkParameters)

    @Test
    fun roundTrip() {
        // Given
        val wallet = randomTransactionMonitor(context)
        val outputStream = ByteArrayOutputStream()
        outputStream.writeTransactionMonitor(wallet)
        val inputStream = ByteArrayInputStream(outputStream.toByteArray())

        // When
        val roundTrip = inputStream.readTransactionMonitor(context)

        // Then
        wallet shouldBe roundTrip
    }
}
