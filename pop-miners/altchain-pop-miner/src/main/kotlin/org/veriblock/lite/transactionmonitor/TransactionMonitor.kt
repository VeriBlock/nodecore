// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.lite.transactionmonitor

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.veriblock.core.utilities.createLogger
import org.veriblock.lite.core.Context
import org.veriblock.lite.core.TransactionMeta
import org.veriblock.lite.net.NodeCoreGateway
import org.veriblock.miners.pop.service.sec
import org.veriblock.sdk.models.Address
import org.veriblock.sdk.models.Sha256Hash
import org.veriblock.sdk.models.VBlakeHash
import org.veriblock.sdk.models.VeriBlockMerklePath
import org.veriblock.sdk.models.VeriBlockTransaction
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.Collections
import java.util.HashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

private val logger = createLogger {}
const val TM_FILE_EXTENSION = ".txmon"
const val MIN_TX_CONFIRMATIONS: Int = 1

class TransactionMonitor(
    val context: Context,
    val address: Address,
    val gateway: NodeCoreGateway,
    transactionsToLoad: List<WalletTransaction> = emptyList()
) {
    private val lock = ReentrantLock(true)
    private val transactions: MutableMap<Sha256Hash, WalletTransaction> = HashMap()

    init {
        for (tx in transactionsToLoad) {
            transactions[tx.id] = tx
        }
    }

    fun start() {
        GlobalScope.launch {
            while (isActive) {
                delay(60.sec.toMillis())
                try {
                    checkPendingTransactions()
                } catch (e: Exception) {
                    logger.error(e) { "Error while polling for VBK transactions!" }
                }
            }
        }
    }

    private fun checkPendingTransactions() = lock.withLock {
        val pendingTxs = transactions.filter {
            it.value.transactionMeta.state === TransactionMeta.MetaState.PENDING
        }

        val txsInfo = gateway.getTransactions(pendingTxs.keys)
        for (txInfo in txsInfo) {
            if (txInfo.confirmations > MIN_TX_CONFIRMATIONS && !txInfo.blockHash.isEmpty) {
                val tx = pendingTxs.getValue(Sha256Hash.wrap(txInfo.transaction.txId.toByteArray()))
                tx.transactionMeta.depth = txInfo.confirmations
                tx.transactionMeta.appearsAtChainHeight = txInfo.blockNumber
                tx.transactionMeta.appearsInBestChainBlock = VBlakeHash.wrap(txInfo.blockHash.toByteArray())
                tx.merklePath = VeriBlockMerklePath(txInfo.merklePath)
                tx.transactionMeta.setState(TransactionMeta.MetaState.CONFIRMED)
            }
        }
    }

    fun getTransactions(): Collection<WalletTransaction> =
        Collections.unmodifiableCollection(transactions.values)

    fun commitTransaction(transaction: VeriBlockTransaction) = lock.withLock {
        if (transactions.containsKey(transaction.id)) {
            return@withLock
        }

        val walletTransaction = WalletTransaction.wrap(transaction)
        walletTransaction.transactionMeta.setState(TransactionMeta.MetaState.PENDING)
        transactions[transaction.id] = walletTransaction
    }

    fun getTransaction(transactionId: Sha256Hash): WalletTransaction {
        return transactions[transactionId]
            ?: error("Unable to find transaction $transactionId in the monitored address")
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

fun File.loadTransactionMonitor(context: Context, gateway: NodeCoreGateway): TransactionMonitor = try {
    FileInputStream(this).use { stream ->
        stream.readTransactionMonitor(context, gateway)
    }
} catch (e: IOException) {
    throw IllegalStateException("Unable to read VBK wallet from disk")
}
