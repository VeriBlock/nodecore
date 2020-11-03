package org.veriblock.spv.service.tx

import io.vertx.core.impl.ConcurrentHashSet
import org.veriblock.sdk.models.VeriBlockBlock
import org.veriblock.spv.model.Transaction

class TxStatusEmitter {
    fun onConfirmation(tx: Transaction, confirmations: Int) {

    }
}

class TxStatusCollector {
    suspend fun onConfirmation(handler: suspend () -> Unit): TxStatusCollector {
        return this
    }
    suspend fun onCompleted(handler: suspend () -> Unit): TxStatusCollector {
        return this
    }
    suspend fun onInvalid(handler: suspend () -> Unit): TxStatusCollector {
        return this
    }
}

data class TxBlockConfirmations(
    val block: VeriBlockBlock,
    val confirmations: Int = -1
)

data class MempoolEntry(
    val tx: Transaction,
    // timestamp, ms
    val addedTime: Long,
    val statusCollector: TxStatusCollector
)

data class MempoolSettings(
    val finalityConfirmations: Int = 10
)

class TxManager(
    private val settings: MempoolSettings = MempoolSettings()
) {
    private val mempool: MutableSet<MempoolEntry> = ConcurrentHashSet()

    fun addTransaction(transaction: Transaction): TxStatusCollector {
        val collector = TxStatusCollector()

        mempool.add(
            element = MempoolEntry(
                tx = transaction,
                addedTime = System.currentTimeMillis(),
                statusCollector = collector
            )
        )

        return collector
    }

    private suspend fun sendTx() {}
}
