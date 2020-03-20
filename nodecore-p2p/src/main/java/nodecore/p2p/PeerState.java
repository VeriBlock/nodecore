// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.p2p;

import org.veriblock.core.utilities.Utility;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class PeerState {
    private int lastMessageReceivedAt;
    public int getLastMessageReceivedAt() {
        return lastMessageReceivedAt;
    }
    public void setLastMessageReceivedAt(int value) {
        this.lastMessageReceivedAt = value;
    }

    private int lastKeystoneQueryRequestTimestamp;
    public int getLastKeystoneQueryRequestTimestamp() {
        return lastKeystoneQueryRequestTimestamp;
    }
    public void setLastKeystoneQueryRequestTimestamp(int value) {
        this.lastKeystoneQueryRequestTimestamp = value;
    }

    private int lastKeystoneQueryReceivedTimestamp;
    public int getLastKeystoneQueryReceivedTimestamp() {
        return lastKeystoneQueryReceivedTimestamp;
    }
    public void setLastKeystoneQueryReceivedTimestamp(int value) {
        this.lastKeystoneQueryReceivedTimestamp = value;
    }

    private boolean announced = false;
    public boolean hasAnnounced() {
        return announced;
    }
    public void setAnnounced(boolean value) {
        announced = value;
    }

    private AtomicLong bytesSent = new AtomicLong(0);
    public long getBytesSent() {
        return bytesSent.get();
    }
    public void recordBytesSent(long size) {
        bytesSent.addAndGet(size);
    }

    private AtomicLong bytesReceived = new AtomicLong(0);
    public long getBytesReceived() {
        return bytesReceived.get();
    }
    public void recordBytesReceived(long size) {
        bytesReceived.addAndGet(size);
    }

    private AtomicInteger unfulfilledRequests = new AtomicInteger(0);
    public int getUnfulfilledRequestCount() {
        return unfulfilledRequests.get();
    }
    public int incrementUnfulfilledRequests() {
        return unfulfilledRequests.incrementAndGet();
    }
    public int decrementUnfulfilledRequests() {
        return unfulfilledRequests.decrementAndGet();
    }

    private AtomicInteger blockCounter = new AtomicInteger(0);
    private ConcurrentHashMap<String, Integer> sentBlocks = new ConcurrentHashMap<>();
    public boolean addSentBlock(String hash, int timestamp) {
        if (sentBlocks.containsKey(hash)) return false;

        sentBlocks.put(hash, timestamp);
        if (blockCounter.incrementAndGet() >= 10) {
            purgeExpired(sentBlocks, 600);
            blockCounter.set(0);
        }
        return true;
    }
    public void removeSentBlock(String hash) {
        sentBlocks.remove(hash);
    }

    private AtomicInteger txCounter = new AtomicInteger(0);
    private ConcurrentHashMap<String, Integer> sentTransactions = new ConcurrentHashMap<>();
    public boolean addSentTransaction(String txId, int timestamp) {
        if (sentTransactions.containsKey(txId)) return false;

        sentTransactions.put(txId, timestamp);
        if (txCounter.incrementAndGet() >= 100) {
            purgeExpired(sentTransactions, 600);
            txCounter.set(0);
        }
        return true;
    }

    private AtomicInteger blockBroadcastCounter = new AtomicInteger(0);
    private ConcurrentHashMap<String, Integer> blockBroadcastHistory = new ConcurrentHashMap<>();
    public boolean addSeenBlock(String hash, int timestamp) {
        if (blockBroadcastHistory.containsKey(hash)) return false;

        blockBroadcastHistory.put(hash, timestamp);
        if (blockBroadcastCounter.incrementAndGet() >= 40) {
            purgeExpired(blockBroadcastHistory, 600);
            blockBroadcastCounter.set(0);
        }
        return true;
    }
    public boolean hasSeenBlock(String hash) {
        return blockBroadcastHistory.containsKey(hash);
    }

    private AtomicInteger txBroadcastCounter = new AtomicInteger(0);
    private ConcurrentHashMap<String, Integer> txBroadcastHistory = new ConcurrentHashMap<>();
    public boolean addSeenTransaction(String txId, int timestamp) {
        if (txBroadcastHistory.containsKey(txId)) return false;

        txBroadcastHistory.put(txId, timestamp);
        if (txBroadcastCounter.incrementAndGet() >= 500) {
            purgeExpired(txBroadcastHistory, 600);
            txBroadcastCounter.set(0);
        }
        return true;
    }
    public boolean hasSeenTransaction(String txId) {
        return txBroadcastHistory.containsKey(txId);
    }


    private static void purgeExpired(ConcurrentHashMap<String, Integer> map, int period) {
        for (String key : map.keySet()) {
            Integer origin = map.get(key);
            if (origin != null && Utility.hasElapsed(map.get(key), period)) {
                map.remove(key);
            }
        }
    }

    public PeerState() {
        setLastMessageReceivedAt(Utility.getCurrentTimeSeconds());
    }
}
