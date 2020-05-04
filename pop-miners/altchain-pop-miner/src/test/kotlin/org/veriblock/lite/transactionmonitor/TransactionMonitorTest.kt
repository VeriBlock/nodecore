// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.lite.transactionmonitor

import io.kotlintest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotlintest.shouldBe
import org.junit.Test
import org.veriblock.core.utilities.Configuration
import org.veriblock.lite.core.Context
import org.veriblock.lite.core.TransactionMeta
import org.veriblock.lite.core.randomTransactionMonitor
import org.veriblock.lite.core.randomVeriBlockTransaction
import org.veriblock.lite.core.randomWalletTransaction
import org.veriblock.lite.net.NodeCoreGateway
import org.veriblock.lite.net.createFullNode
import org.veriblock.lite.params.NetworkConfig
import org.veriblock.lite.params.NetworkParameters

class TransactionMonitorTest {
    private val networkParameters = NetworkParameters(NetworkConfig())
    private val gateway = NodeCoreGateway(networkParameters, createFullNode(networkParameters))
    private val context = Context(Configuration(), networkParameters)

    @Test
    fun loadTransactions() {
        // Given
        val walletTransactions = (1..10).map { randomWalletTransaction(context) }

        // When
        val transactionMonitor = randomTransactionMonitor(context, gateway, walletTransactions = walletTransactions)

        // Then
        transactionMonitor.getTransactions() shouldContainExactlyInAnyOrder walletTransactions
    }

    @Test
    fun commitTransaction() {
        // Given
        val transaction = randomVeriBlockTransaction(context)
        val transactionMonitor = randomTransactionMonitor(context, gateway)

        // When
        transactionMonitor.commitTransaction(transaction)

        // Then
        val walletTransaction = transactionMonitor.getTransaction(transaction.id)
        walletTransaction.type shouldBe transaction.type
        walletTransaction.sourceAddress shouldBe transaction.sourceAddress
        walletTransaction.sourceAmount shouldBe transaction.sourceAmount
        walletTransaction.outputs shouldBe transaction.outputs
        walletTransaction.signatureIndex shouldBe transaction.signatureIndex
        walletTransaction.publicationData shouldBe transaction.publicationData
        walletTransaction.signature shouldBe transaction.signature
        walletTransaction.publicKey shouldBe transaction.publicKey
        walletTransaction.networkByte shouldBe transaction.networkByte
        walletTransaction.transactionMeta.state shouldBe TransactionMeta.MetaState.PENDING
    }

}
