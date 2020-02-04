// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.lite.transactionmonitor

import io.kotlintest.matchers.collections.shouldContain
import io.kotlintest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import org.junit.Test
import org.veriblock.lite.core.MerkleTree
import org.veriblock.lite.core.TransactionMeta
import org.veriblock.lite.core.randomAddress
import org.veriblock.lite.core.randomCoin
import org.veriblock.lite.core.randomFullBlock
import org.veriblock.lite.core.randomTransactionMeta
import org.veriblock.lite.core.randomTransactionMonitor
import org.veriblock.lite.core.randomVeriBlockBlock
import org.veriblock.lite.core.randomVeriBlockTransaction
import org.veriblock.lite.core.randomWalletTransaction

class TransactionMonitorTest {

    @Test
    fun loadTransactions() {
        // Given
        val walletTransactions = (1..10).map { randomWalletTransaction() }

        // When
        val transactionMonitor = randomTransactionMonitor(walletTransactions = walletTransactions)

        // Then
        transactionMonitor.getTransactions() shouldContainExactlyInAnyOrder walletTransactions
    }

    @Test
    fun commitTransaction() {
        // Given
        val transaction = randomVeriBlockTransaction()
        val transactionMonitor = randomTransactionMonitor()

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


    @Test
    fun onBlockChainDownloaded() {
        // Given
        val address = randomAddress()
        val confirmedTransactionsWithWrongBlocks = (1..10).map {
            randomWalletTransaction(transactionMeta = randomTransactionMeta(metaState = TransactionMeta.MetaState.CONFIRMED))
        }
        val confirmedTransactionsWithRightBlocks = (1..10).map {
            randomWalletTransaction(transactionMeta = randomTransactionMeta(metaState = TransactionMeta.MetaState.CONFIRMED))
        }
        val unknownTransactionsWithRightBlocks = (1..10).map {
            randomWalletTransaction(sourceAddress = address, transactionMeta = randomTransactionMeta(metaState = TransactionMeta.MetaState.UNKNOWN))
        }
        val confirmedTransactionBlocks = (1..10).map {
            confirmedTransactionsWithRightBlocks[it - 1].transactionMeta.appearsInBestChainBlock!! to randomFullBlock()
        }.toMap()

        val unknownTransactionBlocks = (1..1).map {
            unknownTransactionsWithRightBlocks[it - 1].transactionMeta.appearsInBestChainBlock!! to randomFullBlock(normalTransactions = unknownTransactionsWithRightBlocks)
        }.toMap()

        val allTransactions = confirmedTransactionsWithWrongBlocks + confirmedTransactionsWithRightBlocks + unknownTransactionsWithRightBlocks
        val allBlocks =  confirmedTransactionBlocks + unknownTransactionBlocks
        val transactionMonitor = randomTransactionMonitor(address, allTransactions)

        // When
        transactionMonitor.onBlockChainDownloaded(allBlocks)

        // Then
        confirmedTransactionsWithWrongBlocks.forEach {
            it.transactionMeta.state shouldBe TransactionMeta.MetaState.UNKNOWN
        }
        confirmedTransactionsWithRightBlocks.forEach {
            it.transactionMeta.depth shouldBe allBlocks.size
        }
        unknownTransactionBlocks.values.forEach { block ->
            unknownTransactionsWithRightBlocks.forEach { transaction ->
                transaction.transactionMeta.state shouldBe TransactionMeta.MetaState.CONFIRMED
                transaction.transactionMeta.depth shouldBe 1

                transaction.transactionMeta.getAppearsInBlock() shouldContain block.hash
                transaction.transactionMeta.appearsInBestChainBlock shouldBe block.hash
                transaction.transactionMeta.appearsAtChainHeight shouldBe block.height
                transaction.merklePath shouldBe MerkleTree.of(block).getMerklePath(
                    transaction.id,
                    unknownTransactionsWithRightBlocks.indexOf(transaction),
                    true
                )
            }
        }
    }

    @Test
    fun onBlockChainReorganized() {
        // Given
        val address = randomAddress()
        val oldBlocks = (1..10).map {
            randomVeriBlockBlock()
        }
        val confirmedWithWrongDepthTransactions = (1..5).map {
            randomWalletTransaction(sourceAddress = address, transactionMeta = randomTransactionMeta(metaState = TransactionMeta.MetaState.CONFIRMED, depthCount =  50))
        }
        val confirmedWithRightDepthTransactions = (1..5).map {
            randomWalletTransaction(sourceAddress = address, sourceAmount = randomCoin(5), transactionMeta = randomTransactionMeta(metaState = TransactionMeta.MetaState.CONFIRMED, depthCount =  oldBlocks.size))
        }

        val allTransactions = confirmedWithWrongDepthTransactions + confirmedWithRightDepthTransactions
        val newBlocks = (1..20).map {
            randomFullBlock(normalTransactions = confirmedWithRightDepthTransactions)
        }

        val transactionMonitor = randomTransactionMonitor(address = address, walletTransactions = allTransactions)

        // When
        transactionMonitor.onBlockChainReorganized(oldBlocks, newBlocks)

        // Then
        confirmedWithWrongDepthTransactions.forEach {
            it.transactionMeta.depth shouldBe 60
        }
        confirmedWithRightDepthTransactions.forEach {
            it.transactionMeta.state shouldBe TransactionMeta.MetaState.CONFIRMED
            it.merklePath shouldNotBe null
        }
    }
}
