// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.lite.wallet

import org.veriblock.lite.core.Context
import org.veriblock.lite.core.FullBlock
import org.veriblock.lite.core.MerkleTree
import org.veriblock.lite.core.TransactionMeta
import org.veriblock.lite.util.invoke
import org.veriblock.sdk.Address
import org.veriblock.sdk.Sha256Hash
import org.veriblock.sdk.VBlakeHash
import org.veriblock.sdk.VeriBlockBlock
import org.veriblock.sdk.VeriBlockTransaction
import org.veriblock.core.utilities.createLogger
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import java.util.concurrent.locks.ReentrantLock

private val logger = createLogger {}
const val TM_FILE_EXTENSION = ".txmon"

class TransactionMonitor(
    val address: Address,
    transactionsToLoad: List<WalletTransaction> = emptyList()
) {
    private val serializer = TransactionMonitorProtobufSerializer()
    private val lock = ReentrantLock(true)
    private val transactions: MutableMap<Sha256Hash, WalletTransaction> = HashMap()

    init {
        for (tx in transactionsToLoad) {
            transactions[tx.id] = tx
        }
    }

    fun getTransactions(): Collection<WalletTransaction> =
        Collections.unmodifiableCollection(transactions.values)

    private fun save(serializer: TransactionMonitorProtobufSerializer) {
        val diskWallet = File(Context.directory, Context.filePrefix + TM_FILE_EXTENSION)

        lock {
            try {
                FileOutputStream(diskWallet).use { stream ->
                    with(serializer) { stream.writeTransactionMonitor(this@TransactionMonitor) }
                }
            } catch (e: IOException) {
                logger.error("Unable to save VBK wallet to disk", e)
            }
        }
    }

    fun commitTransaction(transaction: VeriBlockTransaction) = lock {
        if (transactions.containsKey(transaction.id)) {
            return@lock
        }

        val walletTransaction = WalletTransaction.wrap(transaction)
        walletTransaction.transactionMeta.setState(TransactionMeta.MetaState.PENDING)
        transactions[transaction.id] = walletTransaction
    }

    fun getTransaction(transactionId: Sha256Hash): WalletTransaction {
        return transactions[transactionId]
            ?: error("Unable to find transaction $transactionId in the monitored address")
    }

    private fun handleReorganizedBlocks(blocks: List<VeriBlockBlock>) = lock {
        removeConfirmations(blocks.size)
    }

    private fun handleNewBlock(block: FullBlock) = lock {
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

    private fun removeConfirmations(amount: Int) = lock {
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

    private fun filterTransactionsFrom(block: FullBlock): Map<Sha256Hash, WalletTransaction> {
        val relevantTransactions = HashMap<Sha256Hash, WalletTransaction>()

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

    fun onBlockChainDownloaded(blocks: Map<VBlakeHash, FullBlock>) {
        lock {
            for (tx in transactions.values) {
                // TODO: Fix this
                if (
                    tx.transactionMeta.state === TransactionMeta.MetaState.CONFIRMED &&
                    !blocks.containsKey(tx.transactionMeta.appearsInBestChainBlock)
                ) {
                    logger.warn { "The VBK transaction ${tx.id} appears in a block that's not known: ${tx.transactionMeta.appearsInBestChainBlock}" }
                    tx.transactionMeta.setState(TransactionMeta.MetaState.UNKNOWN)
                }
            }

            for (block in blocks.values) {
                handleNewBlock(block)
            }
        }

        save(serializer)
    }

    fun onBlockChainReorganized(oldBlocks: List<VeriBlockBlock>, newBlocks: List<FullBlock>) {
        handleReorganizedBlocks(oldBlocks)
        for (block in newBlocks) {
           handleNewBlock(block)
        }

        save(serializer)
    }

    fun onNewBestBlock(newBlock: FullBlock): Boolean {
        val balanceChanged = handleNewBlock(newBlock)

        save(serializer)

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
        // TODO: Proof-of-Proof endorsing chain
        // TODO: Alt-chain endorsement
        return false
    }
}

fun File.loadTransactionMonitor(): TransactionMonitor = try {
    FileInputStream(this).use { stream ->
        with (TransactionMonitorProtobufSerializer()) {
            stream.readTransactionMonitor()
        }
    }
} catch (e: IOException) {
    throw IllegalStateException("Unable to read VBK wallet from disk")
}
