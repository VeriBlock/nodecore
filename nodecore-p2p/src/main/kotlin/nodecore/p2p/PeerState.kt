// VeriBlock NodeCore
// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.p2p

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.ConcurrentHashMap
import org.veriblock.core.utilities.Utility
import java.util.concurrent.atomic.AtomicLong

class PeerState {
    var lastMessageReceivedAt = 0
    var lastKeystoneQueryRequestTimestamp = 0
    var lastKeystoneQueryReceivedTimestamp = 0

    init {
        lastMessageReceivedAt = Utility.getCurrentTimeSeconds()
    }

    private var announced = false
    fun hasAnnounced() = announced
    fun setAnnounced(value: Boolean) {
        announced = value
    }
    
    private val bytesSent = AtomicLong(0)
    fun getBytesSent(): Long = bytesSent.get()
    fun recordBytesSent(size: Long) {
        bytesSent.addAndGet(size)
    }
    
    private val bytesReceived = AtomicLong(0)
    fun getBytesReceived(): Long = bytesReceived.get()
    fun recordBytesReceived(size: Long) {
        bytesReceived.addAndGet(size)
    }
    
    private val unfulfilledRequests = AtomicInteger(0)
    fun getUnfulfilledRequestCount(): Int = unfulfilledRequests.get()
    fun incrementUnfulfilledRequests(): Int {
        return unfulfilledRequests.incrementAndGet()
    }
    fun decrementUnfulfilledRequests(): Int {
        return unfulfilledRequests.decrementAndGet()
    }
    
    private val blockCounter = AtomicInteger(0)
    private val sentBlocks = ConcurrentHashMap<String, Int>()
    fun addSentBlock(hash: String, timestamp: Int): Boolean {
        if (sentBlocks.containsKey(hash)) {
            return false
        }
        sentBlocks[hash] = timestamp
        if (blockCounter.incrementAndGet() >= 10) {
            sentBlocks.purgeExpired(600)
            blockCounter.set(0)
        }
        return true
    }
    
    fun removeSentBlock(hash: String) {
        sentBlocks.remove(hash)
    }
    
    private val txCounter = AtomicInteger(0)
    private val sentTransactions = ConcurrentHashMap<String, Int>()
    fun addSentTransaction(txId: String, timestamp: Int): Boolean {
        if (sentTransactions.containsKey(txId)) {
            return false
        }
        sentTransactions[txId] = timestamp
        if (txCounter.incrementAndGet() >= 100) {
            sentTransactions.purgeExpired(600)
            txCounter.set(0)
        }
        return true
    }
    
    private val blockBroadcastCounter = AtomicInteger(0)
    private val blockBroadcastHistory = ConcurrentHashMap<String, Int>()
    fun addSeenBlock(hash: String, timestamp: Int): Boolean {
        if (blockBroadcastHistory.containsKey(hash)) {
            return false
        }
        blockBroadcastHistory[hash] = timestamp
        if (blockBroadcastCounter.incrementAndGet() >= 40) {
            blockBroadcastHistory.purgeExpired(600)
            blockBroadcastCounter.set(0)
        }
        return true
    }
    
    fun hasSeenBlock(hash: String): Boolean {
        return blockBroadcastHistory.containsKey(hash)
    }
    
    private val txBroadcastCounter = AtomicInteger(0)
    private val txBroadcastHistory = ConcurrentHashMap<String, Int>()
    fun addSeenTransaction(txId: String, timestamp: Int): Boolean {
        if (txBroadcastHistory.containsKey(txId)) {
            return false
        }
        txBroadcastHistory[txId] = timestamp
        if (txBroadcastCounter.incrementAndGet() >= 500) {
            txBroadcastHistory.purgeExpired(600)
            txBroadcastCounter.set(0)
        }
        return true
    }

    fun hasSeenTransaction(txId: String): Boolean {
        return txBroadcastHistory.containsKey(txId)
    }
}

private fun ConcurrentHashMap<String, Int>.purgeExpired(period: Int) {
    values.removeIf {
        Utility.hasElapsed(it, period)
    }
}
