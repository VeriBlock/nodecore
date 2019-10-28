// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.lite.core

import io.kotlintest.shouldBe
import org.junit.Test
import org.veriblock.lite.wallet.TransactionMonitorProtobufSerializer
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class WalletProtobufSerializerTest {

    private val serializer = TransactionMonitorProtobufSerializer()

    @Test
    fun roundTrip() = with(serializer) {
        // Given
        val wallet = randomTransactionMonitor()
        val outputStream = ByteArrayOutputStream()
        outputStream.writeTransactionMonitor(wallet)
        val inputStream = ByteArrayInputStream(outputStream.toByteArray())

        // When
        val roundTrip = inputStream.readTransactionMonitor()

        // Then
        wallet shouldBe roundTrip
    }
}
