// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.transactionmonitor

import org.veriblock.core.crypto.VbkTxId
import org.veriblock.core.crypto.asAnyVbkHash
import org.veriblock.core.utilities.createLogger
import org.veriblock.core.utilities.debugError
import org.veriblock.miners.pop.core.ApmContext
import org.veriblock.miners.pop.core.TransactionMeta
import org.veriblock.miners.pop.net.SpvGateway
import org.veriblock.sdk.models.Address
import org.veriblock.sdk.models.VeriBlockMerklePath
import org.veriblock.sdk.models.VeriBlockTransaction
import org.veriblock.spv.util.SpvEventBus
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

private val logger = createLogger {}
const val TM_FILE_EXTENSION = ".txmon"
const val MIN_TX_CONFIRMATIONS: Int = 1

class TransactionMonitor(
    val context: ApmContext,
    val gateway: SpvGateway,
    val address: Address,
    transactionsToLoad: List<WalletTransaction> = emptyList()
) {
    private val lock = ReentrantLock(true)
    private val transactions: MutableMap<VbkTxId, WalletTransaction> = ConcurrentHashMap()

    init {
        for (tx in transactionsToLoad) {
            transactions[tx.id] = tx
        }
    }

    fun getTransactions(): Collection<WalletTransaction> =
        Collections.unmodifiableCollection(transactions.values)

    fun getPendingTransactions(): Collection<WalletTransaction> =
        getTransactions()
            .filter { it.transactionMeta.state == TransactionMeta.MetaState.PENDING }

    fun start() {
        SpvEventBus.newBestBlockEvent.register(this) {
            checkPendingTransactions()
        }
    }

    private fun checkPendingTransactions() {
        val pendingTxs = getPendingTransactions()
            .map { it.id }
            .toList()

        val txsInfo = gateway.getTransactions(pendingTxs)

        txsInfo.asSequence()
            .filter { it.confirmations >= MIN_TX_CONFIRMATIONS }
            .forEach {
                val tx = transactions[it.transaction.txId]
                    ?: error("Unable to retrieve pending transactions")
                tx.transactionMeta.depth = it.confirmations
                tx.transactionMeta.appearsAtChainHeight = it.blockNumber
                tx.transactionMeta.appearsInBestChainBlock = it.blockHash.asAnyVbkHash()
                tx.merklePath = VeriBlockMerklePath(it.merklePath)
                tx.transactionMeta.setState(TransactionMeta.MetaState.CONFIRMED)
            }

        save()
    }

    private fun save() {
        val diskWallet = File(context.directory, context.filePrefix + TM_FILE_EXTENSION)

        lock.withLock {
            try {
                FileOutputStream(diskWallet).use { stream ->
                    stream.writeTransactionMonitor(this@TransactionMonitor)
                }
            } catch (e: IOException) {
                logger.debugError(e) { "Unable to save VBK wallet to disk" }
            }
        }
    }

    fun commitTransaction(transaction: VeriBlockTransaction) = lock.withLock {
        if (transactions.containsKey(transaction.id)) {
            return
        }

        val walletTransaction = WalletTransaction.wrap(transaction)
        walletTransaction.transactionMeta.setState(TransactionMeta.MetaState.PENDING)
        transactions[transaction.id] = walletTransaction
    }

    fun getTransaction(transactionId: VbkTxId): WalletTransaction {
        return transactions[transactionId]
            ?: error("Unable to find VBK transaction $transactionId in the monitored address ($address)")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TransactionMonitor

        if (transactions != other.transactions) return false
        if (address != other.address) return false

        return true
    }

    override fun hashCode(): Int {
        var result = transactions.hashCode()
        result = 31 * result + address.hashCode()
        return result
    }

    override fun toString(): String {
        return "TransactionMonitor(address=$address, transactions=$transactions)"
    }

    private fun VeriBlockTransaction.isRelevant(): Boolean {
        if (transactions.containsKey(id)) {
            return true
        }
        if (sourceAddress == address) {
            return true
        }
        if (outputs.any { it.address == address }) {
            return true
        }
        return false
    }
}

fun File.loadTransactionMonitor(context: ApmContext, gateway: SpvGateway): TransactionMonitor = try {
    FileInputStream(this).use { stream ->
        stream.readTransactionMonitor(context, gateway)
    }
} catch (e: IOException) {
    throw IllegalStateException("Unable to read VBK wallet from disk")
}
