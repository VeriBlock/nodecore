// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.transactionmonitor

import org.veriblock.core.crypto.VbkTxId
import org.veriblock.core.utilities.createLogger
import org.veriblock.miners.pop.core.ApmContext
import org.veriblock.miners.pop.core.TransactionMeta
import org.veriblock.miners.pop.net.SpvGateway
import org.veriblock.sdk.models.Address
import org.veriblock.sdk.models.VeriBlockTransaction
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.Collections
import java.util.HashMap
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import org.veriblock.core.utilities.debugError
import org.veriblock.sdk.models.VeriBlockBlock
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

    private fun handleReorganizedBlocks(blocks: List<VeriBlockBlock>) = lock.withLock {
        removeConfirmations(blocks.size)
    }

    private fun handleNewBlock(block: FullBlock) = lock.withLock {
        logger.debug { "New VBK block received at height ${block.height}: ${block.hash}" }
        val blockTransactions = filterTransactionsFrom(block)

        logger.debug { "Found ${blockTransactions.size} relevant transactions in the VBK block" }
        for (tx in transactions.values) {
            val meta = tx.transactionMeta
            if (meta.state === TransactionMeta.MetaState.CONFIRMED) {
                meta.incrementDepth()
            } else if (blockTransactions.containsKey(tx.id)) {
                meta.setState(TransactionMeta.MetaState.CONFIRMED)
                meta.depth = 1
                meta.addBlockAppearance(block.hash)
                meta.appearsInBestChainBlock = block.hash
                meta.appearsAtChainHeight = block.height
                tx.merklePath = blockTransactions.getValue(tx.id).merklePath
            }
        }

        var balanceChanged = false
        for (tx in blockTransactions.values) {
            if (tx.sourceAddress == address) {
                logger.info { "Detected outgoing VBK transaction: ${tx.id}" }
                balanceChanged = true
            }
            if (tx.outputs.any { it.address == address }) {
                logger.info { "Detected incoming VBK transaction: ${tx.id}" }
                balanceChanged = true
            }
        }
        balanceChanged
    }

    private fun removeConfirmations(amount: Int) = lock.withLock {
        for (tx in transactions.values) {
            val meta = tx.transactionMeta
            if (meta.state === TransactionMeta.MetaState.CONFIRMED) {
                if (amount < meta.depth) {
                    meta.depth = meta.depth - amount
                } else {
                    meta.setState(TransactionMeta.MetaState.PENDING)
                    tx.merklePath = null
                }
            }
        }
    }

    private fun filterTransactionsFrom(block: FullBlock): Map<VbkTxId, WalletTransaction> {
        val relevantTransactions = HashMap<VbkTxId, WalletTransaction>()

        val merkleTree: MerkleTree by lazy {
            MerkleTree.of(block)
        }

        block.normalTransactions.forEachIndexed { i, transaction ->
            if (transaction.isRelevant()) {
                val walletTransaction = WalletTransaction.wrap(transaction)
                walletTransaction.merklePath = merkleTree.getMerklePath(transaction.id, i, true)

                relevantTransactions[transaction.id] = walletTransaction
            }
        }
        return relevantTransactions
    }

    fun onBlockChainReorganized(oldBlocks: List<VeriBlockBlock>, newBlocks: List<FullBlock>) {
        handleReorganizedBlocks(oldBlocks)
        for (block in newBlocks) {
            handleNewBlock(block)
        }

        save()
    }

    fun onNewBestBlock(newBlock: FullBlock): Boolean {
        val balanceChanged = handleNewBlock(newBlock)

        save()

        return balanceChanged
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
