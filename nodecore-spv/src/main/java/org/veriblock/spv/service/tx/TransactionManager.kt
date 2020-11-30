package org.veriblock.spv.service.tx

import org.veriblock.core.crypto.Sha256Hash
import org.veriblock.core.utilities.Event
import org.veriblock.core.utilities.createLogger
import org.veriblock.sdk.models.Address
import org.veriblock.sdk.models.VeriBlockBlock
import org.veriblock.sdk.models.VeriBlockMerklePath
import org.veriblock.spv.model.Transaction
import org.veriblock.spv.service.Blockchain
import org.veriblock.spv.service.TransactionInfo
import org.veriblock.spv.service.tx.TxStatusChangedEventData.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

private val logger = createLogger {}

private const val TEN_MINUTES = 10 * 60 * 1000 // ms

class TransactionManager(
    val blockchain: Blockchain
) {
    private val lock = ReentrantLock()
    private val mempool: MutableMap<Sha256Hash, MempoolEntry> = ConcurrentHashMap()
    private val monitoredTransactionIds = ConcurrentHashMap<Sha256Hash, TransactionIdMonitorEntry>()
    private val transactionsByAddress = ConcurrentHashMap<Address, ArrayList<Sha256Hash>>()

    fun getPendingTransactionIds(): List<Sha256Hash> = monitoredTransactionIds.keys().toList()

    fun addTransaction(id: Sha256Hash, confirmations: Int = 1): Event<TxStatusChangedEventData> {
        require(confirmations > 0) {
            "Confirmations must be > 0"
        }

        val event = Event<TxStatusChangedEventData>("OnTxStatusChanged=${id}");
        monitoredTransactionIds[id] = TransactionIdMonitorEntry(
            id = id,
            event = event,
            requiredConfirmations = confirmations,
            lastTxInfoTime = System.currentTimeMillis()
        )

        return event
    }

    fun addTransaction(transaction: Transaction, confirmations: Int = 1): Event<TxStatusChangedEventData> = lock.withLock {
        require(confirmations > 0) {
            "Confirmations must be > 0"
        }

        val id = transaction.txId
        mempool[id] = MempoolEntry(transaction)
        val addr = Address(transaction.inputAddress.get())
        transactionsByAddress.getOrPut(addr) { ArrayList() }.add(id)

        val event = addTransaction(id, confirmations)
        event.register(this) { e ->
            when (e) {
                is Invalid -> clearMempoolTxesWithGreaterOrEqualSigIndex(e.id)
                is GotRequiredConfirmationsN -> removeTransactionId(e.id)
                is Confirmation -> return@register // Ignore
            }
        }

        return event
    }

    fun removeTransactionId(id: Sha256Hash) {
        // first, try to remove from `monitoredTxIds`.
        // if does not exist - all other storages will not contain this `id`
        val e = monitoredTransactionIds.remove(id) ?: return

        // is `id` in mempool?
        val entry = mempool.remove(e.id) ?: return
        val addr = Address(entry.tx.inputAddress.get())
        // if txesByAddress contains `addr`, remove `id`
        transactionsByAddress[addr]?.remove(e.id)
        // if txesByAddress array is empty, cleanup
        if (transactionsByAddress[addr]?.isEmpty() == true) {
            transactionsByAddress.remove(addr)
        }
    }

    fun getTransaction(id: Sha256Hash): Transaction? {
        return mempool[id]?.tx
    }

    fun getTransactionInfo(id: Sha256Hash): TransactionInfo? {
        return monitoredTransactionIds[id]?.lastTxInfo
    }

    fun getPendingSignatureIndexForAddress(address: Address): Long? = transactionsByAddress[address]
        ?.mapNotNull { mempool[it] }
        ?.maxOfOrNull { it.tx.getSignatureIndex() }

    /**
     * Execute this callback whenever we get a response "missing transaction" for given tx 'id'
     * @return false if peer sent us response for tx which we never requested/not monitor
     */
    fun onMissingTransactionInfo(id: Sha256Hash): Boolean {
        val item = monitoredTransactionIds[id] ?: return false
        val current = System.currentTimeMillis()
        if (item.containingBlock == null && current - item.lastTxInfoTime > TEN_MINUTES) {
            // last time this tx received 'TxInfo' was 10 min ago, Tx is likely to be invalid.
            logger.warn { "Transaction=${item.id} is marked as invalid" }
            item.event.trigger(Invalid(item.id))
        }

        return true
    }

    /**
     * Execute this callback whenever we get a response with "TxInfo".
     * @return false if peer sent us txinfo which we never requested/not monitor, or cryptographic proof is invalid
     */
    fun onTransactionInfo(info: TransactionInfo): Boolean = lock.withLock {
        val id = info.transaction.txId
        val item = monitoredTransactionIds[id]
            ?: return false
        item.lastTxInfoTime = System.currentTimeMillis()
        item.lastTxInfo = info

        // do we know containing block?
        if (item.containingBlock == null && item.confirmations <= 0) {
            // no. is tx in mempool?
            if (info.confirmations == 0) {
                // yes! tx is is mempool
                item.confirmations = 0
                item.containingBlock = null
                return true
            }

            // we don't know containing block yet, and tx is not in mempool
        }

        val hash = try {
            info.blockHash.trimToPreviousBlockSize()
        } catch (e: Exception) {
            return false
        }
        val block = blockchain.getBlock(hash)
        // can't find block locally, ignore this tx info
            ?: return false

        val mp = VeriBlockMerklePath(info.merklePath)
        // verify merkle proof for this TX
        if (mp.merkleRoot.truncate() != block.header.merkleRoot) {
            // can't prove that TX is in `block`
            logger.warn { "mp=${mp.merkleRoot}, trimmed=${mp.merkleRoot.truncate()} == mroot=${block.header.merkleRoot}" }
            return false
        }

        // txinfo is cryptographically valid

        // determine which block is earlier AND on active chain - the one we know about or the one peer just sent us
        val containingBlock = item.containingBlock ?: block.header
        if (blockchain.isOnActiveChain(block.hash) && block.height < containingBlock.height) {
            // new block is on active chain and is earlier
            item.containingBlock = block.header
        } else if (blockchain.isOnActiveChain(containingBlock.hash)) {
            // old containing block is on active chain and is earlier
            item.containingBlock = containingBlock
        } else {
            // neither of these two are on active chain, reset known containing block...
            item.containingBlock = null
            item.confirmations = -1
            return true
        }

        // we do not rely on remote peer's 'confirmations', we calculate confirmations ourselves
        val tip = blockchain.activeChain.tip
        item.confirmations = tip.height - containingBlock.height + 1 /* containing itself */

        return true
    }

    /**
     * Execute this function whenever VBK Best Block changes.
     */
    fun onVbkBestBlock(block: VeriBlockBlock) {
        val index = blockchain.getBlockIndex(block.hash)
            ?: throw IllegalStateException(
                "Invariant failed: OnVbkBestBlock is called with a block which does not exist in VBK blockchain or not on active chain. Block=${block}"
            )

        // index is supposed to be on active chain.
        check(blockchain.isOnActiveChain(block.hash)) {
            // if this happens, it is likely caused by incorrect API usage or concurrency issue
            "Invariant failed: OnVbkBestBlock is called on a block=${block}, which is not on active chain!"
        }

        monitoredTransactionIds.values.forEach { tx ->
            val containingBlock = tx.containingBlock ?: return

            lock.withLock {
                // is containing block in direct ancestry of best block?
                val ancestor = index.getAncestorAtHeight(containingBlock.height)
                if (ancestor?.smallHash != containingBlock.hash.trimToPreviousBlockSize()) {
                    // containing block is not direct ancestor of best chain
                    tx.confirmations = -1
                    return
                }

                // this tx is contained in a block which is in direct ancestry of best block
                tx.confirmations = block.height - ancestor.height + 1 /* containing itself */
                tx.event.trigger(Confirmation(tx.id, containingBlock, tx.confirmations))
                if (tx.confirmations >= tx.requiredConfirmations) {
                    tx.event.trigger(GotRequiredConfirmationsN(tx.id, containingBlock, tx.confirmations))
                    // unsubscribe listeners
                    tx.event.clear()
                }
            }

        }
    }

    // we determined that TX with `id` is invalid. All TXes that have SigIndex >= TX.getSigIndex() shall be removed.
    private fun clearMempoolTxesWithGreaterOrEqualSigIndex(id: Sha256Hash) {
        // this id must be in mempool
        val invalid = mempool.remove(id)
            ?: throw IllegalStateException("Invariant failed: got Invalid(id=$id) but mempool does not contain this TX")

        // take all txids sent by this Address and filter them
        val address = Address(invalid.tx.inputAddress.get())
        val transactionIds = transactionsByAddress[address]
            ?: throw IllegalStateException("Invariant failed: mempool contained TX=$id but txesByAddress didn't!")

        // do remove
        transactionIds.asSequence()
            .mapNotNull { mempool[it] }
            // remove all txes whose sig index is greater or equal than invalid sigindex
            .filter { invalid.tx.getSignatureIndex() <= it.tx.getSignatureIndex() }
            .forEach { removeTransactionId(it.id) }
    }
}

data class MempoolEntry(
    val tx: Transaction,
) {
    val id: Sha256Hash get() = tx.txId

    // timestamp, ms
    val addedTime: Long = System.currentTimeMillis()
}

sealed class TxStatusChangedEventData(
    val id: Sha256Hash
) {
    // intermediate status = can be received more than once
    class Confirmation(
        id: Sha256Hash,
        val block: VeriBlockBlock,
        val confirmations: Int
    ) : TxStatusChangedEventData(id)

    // TODO(warchant): when nodecore can respond "tx is invalid" in TransactionInfo, modify this event to support this
    // final status = can be received exactly once, then all listeners unsubscribed
    class Invalid(
        id: Sha256Hash
    ) : TxStatusChangedEventData(id)

    // final status
    class GotRequiredConfirmationsN(
        id: Sha256Hash,
        val block: VeriBlockBlock,
        val confirmations: Int
    ) : TxStatusChangedEventData(id)
}

private data class TransactionIdMonitorEntry(
    val id: Sha256Hash,
    var event: Event<TxStatusChangedEventData>,
    var lastTxInfoTime: Long,
    val requiredConfirmations: Int
) {
    var lastTxInfo: TransactionInfo? = null

    // it was cryptographically proven that this block contains this tx
    var containingBlock: VeriBlockBlock? = null

    // <0 = unknown
    // 0  = in a mempool
    // >0 = in a block
    var confirmations: Int = -1
}
