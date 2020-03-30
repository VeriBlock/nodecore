// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.lite.transactionmonitor

import io.kotlintest.shouldBe
import org.junit.Test
import org.veriblock.core.utilities.Configuration
import org.veriblock.lite.core.Context
import org.veriblock.lite.core.TransactionMeta
import org.veriblock.lite.core.randomAddress
import org.veriblock.lite.core.randomCoin
import org.veriblock.lite.core.randomOutput
import org.veriblock.lite.core.randomPublicationData
import org.veriblock.lite.core.randomTransactionMeta
import org.veriblock.lite.core.randomVeriBlockMerklePath
import org.veriblock.lite.core.randomWalletTransaction
import org.veriblock.lite.params.NetworkConfig
import org.veriblock.lite.params.NetworkParameters
import org.veriblock.sdk.models.VeriBlockTransaction

class WalletTransactionTest {
    private val networkParameters = NetworkParameters(NetworkConfig())
    private val context = Context(Configuration(), networkParameters)

    @Test
    fun transactionDataByData() {
        // Given
        val type: Byte = 0x01
        val sourceAddress = randomAddress()
        val sourceAmount = randomCoin()
        val outputs = (1..10).map { randomOutput() }
        val signatureIndex: Long = 7
        val publicationData = randomPublicationData()
        val signature = ByteArray(10)
        val publicKey = ByteArray(8)
        val networkByte: Byte? = networkParameters.transactionPrefix
        val transactionMeta: TransactionMeta = randomTransactionMeta()
        val merklePath = randomVeriBlockMerklePath()

        // When
        val walletTransaction = randomWalletTransaction(
            context, type, sourceAddress, sourceAmount,
            outputs, signatureIndex, publicationData, signature, publicKey, networkByte, transactionMeta, merklePath
        )

        // Then
        walletTransaction.type shouldBe type
        walletTransaction.sourceAddress shouldBe sourceAddress
        walletTransaction.sourceAmount shouldBe sourceAmount
        walletTransaction.outputs shouldBe outputs
        walletTransaction.signatureIndex shouldBe signatureIndex
        walletTransaction.publicationData shouldBe publicationData
        walletTransaction.signature shouldBe signature
        walletTransaction.publicKey shouldBe publicKey
        walletTransaction.networkByte shouldBe networkByte
        walletTransaction.transactionMeta shouldBe transactionMeta
        walletTransaction.merklePath shouldBe merklePath
    }

    @Test
    fun transactionDataByVeriblockTransaction() {
        // Given
        val type: Byte = 0x01
        val sourceAddress = randomAddress()
        val sourceAmount = randomCoin()
        val outputs = (1..10).map { randomOutput() }
        val signatureIndex: Long = 7
        val publicationData = randomPublicationData()
        val signature = ByteArray(10)
        val publicKey = ByteArray(8)
        val networkByte: Byte? = networkParameters.transactionPrefix

        // When
        val veriblockTransaction = VeriBlockTransaction(
            type, sourceAddress, sourceAmount, outputs,
            signatureIndex, publicationData, signature, publicKey, networkByte
        )
        val walletTransaction = WalletTransaction.wrap(veriblockTransaction)

        // Then
        walletTransaction.type shouldBe type
        walletTransaction.sourceAddress shouldBe sourceAddress
        walletTransaction.sourceAmount shouldBe sourceAmount
        walletTransaction.outputs shouldBe outputs
        walletTransaction.signatureIndex shouldBe signatureIndex
        walletTransaction.publicationData shouldBe publicationData
        walletTransaction.signature shouldBe signature
        walletTransaction.publicKey shouldBe publicKey
        walletTransaction.networkByte shouldBe networkByte
        walletTransaction.merklePath shouldBe null
    }
}
