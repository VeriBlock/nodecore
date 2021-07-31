// VeriBlock NodeCore
// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.p2p

import nodecore.api.grpc.RpcBlockRequest
import nodecore.api.grpc.RpcTransactionRequest
import nodecore.p2p.event.PeerMisbehaviorEvent
import org.veriblock.core.utilities.Utility
import org.veriblock.core.utilities.createLogger
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit


private val logger = createLogger {}

class TrafficManager {
    private val blockRequestLog: ConcurrentHashMap<String, ConcurrentHashMap<String, BlockRequest>> = ConcurrentHashMap()
    private val blockRequestQueue: ConcurrentHashMap<String, ConcurrentLinkedQueue<BlockRequest>> = ConcurrentHashMap()
    private val txRequestLog: ConcurrentHashMap<String, ConcurrentHashMap<String, TransactionRequest>> = ConcurrentHashMap()

    init {
        Threading.TRAFFIC_MANAGER_THREAD.scheduleWithFixedDelay({ manage() }, 5L, 5L, TimeUnit.SECONDS)
    }

    fun shutdown() {
        logger.info("Shutting down traffic manager")
        Threading.TRAFFIC_MANAGER_THREAD.safeShutdown()
    }

    fun requestBlocks(blockRequests: List<BlockRequest>) {
        if (blockRequests.isEmpty()) {
            return
        }

        val toBeSent: MutableList<BlockRequest> = ArrayList()
        for (request in blockRequests) {
            if (request.peer.isNotInGoodStanding()) {
                P2pEventBus.peerMisbehavior.trigger(PeerMisbehaviorEvent(
                    peer = request.peer,
                    reason = PeerMisbehaviorEvent.Reason.UNFULFILLED_REQUEST_LIMIT,
                    message = "The peer sent too many block requests that couldn't be fulfilled"
                ))
                continue
            }

            if (request.isPending()) {
                blockRequestQueue.getOrPut(request.hash) {
                    ConcurrentLinkedQueue()
                }.add(request)
            } else {
                toBeSent.add(request)
                request.requestedAt = Utility.getCurrentTimeSeconds()
                blockRequestLog.getOrPut(request.hash) {
                    ConcurrentHashMap()
                }[request.peer.addressKey] = request

                request.peer.state.incrementUnfulfilledRequests()
            }
        }

        requestBlocksInternal(toBeSent)
    }

    private fun requestBlocksInternal(requests: List<BlockRequest>) {
        if (requests.isEmpty()) {
            return
        }
        val peerEvents = HashMap<Peer, RpcBlockRequest.Builder>()
        for (req in requests) {
            peerEvents.getOrPut(req.peer) {
                RpcBlockRequest.newBuilder()
            }.addHeaders(req.header)
        }
        for ((peer, request) in peerEvents) {
            try {
                peer.send(
                    buildMessage {
                        blockRequest = request.build()
                    }
                )
            } catch (e: Exception) {
                logger.error("Unable to send block request", e)
            }
        }
    }

    fun requestTransactions(txRequests: List<TransactionRequest>) {
        if (txRequests.isEmpty()) {
            return
        }

        val peerEvents = HashMap<Peer, RpcTransactionRequest.Builder>()
        for (request in txRequests) {
            if (request.peer.isNotInGoodStanding()) {
                P2pEventBus.peerMisbehavior.trigger(PeerMisbehaviorEvent(
                    peer = request.peer,
                    reason = PeerMisbehaviorEvent.Reason.UNFULFILLED_REQUEST_LIMIT,
                    message = "The peer sent too many transaction requests that couldn't be fulfilled"
                ))
                continue
            }

            if (request.countPending() < P2pConstants.CONCURRENT_TX_REQUESTS) {
                peerEvents.getOrPut(request.peer) {
                    RpcTransactionRequest.newBuilder()
                }.addTransactions(request.transaction)
                request.requestedAt = Utility.getCurrentTimeSeconds()
                txRequestLog.getOrPut(request.txId) {
                    ConcurrentHashMap()
                }[request.peer.addressKey] = request

                request.peer.state.incrementUnfulfilledRequests()
            }
        }

        for ((peer, request) in peerEvents) {
            try {
                peer.send(
                    buildMessage {
                        txRequest = request.build()
                    }
                )
            } catch (e: Exception) {
                logger.error("Unable to send transaction request", e)
            }
        }
    }

    fun blockReceived(hash: String, peerIdentifier: String): Boolean {
        val requested = blockRequestLog[hash]
            ?: return false

        val request = requested.remove(peerIdentifier)
        if (request != null) {
            request.peer.state.decrementUnfulfilledRequests()

            // Clear the queue
            blockRequestQueue.remove(hash)
            if (requested.isEmpty()) {
                blockRequestLog.remove(hash)
            }

            return true
        }

        return false
    }

    fun transactionReceived(txId: String, peerIdentifier: String): Boolean {
        val requested = txRequestLog[txId]
            ?: return false

        val request = requested.remove(peerIdentifier)
        if (request != null) {
            request.peer.state.decrementUnfulfilledRequests()

            if (requested.isEmpty()) {
                txRequestLog.remove(txId)
            }

            return true
        }

        return false
    }

    fun blockNotFound(hash: String, peerIdentifier: String?) {
        val requested = blockRequestLog[hash]
            ?: return

        val request = requested.remove(peerIdentifier)
        if (request != null) {
            request.peer.state.decrementUnfulfilledRequests()

            if (requested.isEmpty()) {
                blockRequestLog.remove(hash)
            }
        }
    }

    fun transactionNotFound(txId: String, peerIdentifier: String?) {
        val requested = txRequestLog[txId]
            ?: return

        val request = requested.remove(peerIdentifier)
        if (request != null) {
            request.peer.state.decrementUnfulfilledRequests()

            if (requested.isEmpty()) {
                txRequestLog.remove(txId)
            }
        }
    }

    fun blockHasBeenRequested(hash: String): Boolean {
        return blockRequestLog.containsKey(hash)
    }

    fun manage() {
        val blockRequests: MutableList<BlockRequest> = ArrayList()

        for (key in blockRequestQueue.keys) {
            // All requests have reached their expiration
            val allBlockRequests = blockRequestLog[key]
            if (allBlockRequests != null && allBlockRequests.values.anyExpired()) {
                val queue = blockRequestQueue[key]
                val nextRequest = queue?.poll()
                if (nextRequest != null) {
                    if (nextRequest.peer.isNotInGoodStanding()) {
                        P2pEventBus.peerMisbehavior.trigger(PeerMisbehaviorEvent(
                            peer = nextRequest.peer,
                            reason = PeerMisbehaviorEvent.Reason.UNFULFILLED_REQUEST_LIMIT,
                            message = "The peer sent too many block requests that couldn't be fulfilled"
                        ))

                        if (queue.isEmpty()) {
                            blockRequestQueue.remove(key)
                        }

                        continue
                    }

                    nextRequest.requestedAt = Utility.getCurrentTimeSeconds()
                    blockRequests.add(nextRequest)

                    blockRequestLog.putIfAbsent(nextRequest.hash, ConcurrentHashMap())
                    blockRequestLog.getOrPut(nextRequest.hash) {
                        ConcurrentHashMap()
                    }[nextRequest.peer.addressKey] = nextRequest

                    nextRequest.peer.state.incrementUnfulfilledRequests()

                    if (queue.isEmpty()) {
                        blockRequestQueue.remove(key)
                    }
                }
            }
        }

        requestBlocksInternal(blockRequests)
    }

    private fun BlockRequest.isPending(): Boolean {
        val requestLogs = blockRequestLog[hash]
        return requestLogs != null && !requestLogs.values.anyExpired()
    }

    private fun TransactionRequest.countPending(): Int {
        val logged = txRequestLog[txId]
            ?: return 0

        return logged.values.count { !it.expired() }
    }

    private fun PeerRequest.expired(): Boolean {
        return Utility.hasElapsed(requestedAt, P2pConstants.PEER_REQUEST_TIMEOUT)
    }

    private fun Collection<BlockRequest>.anyExpired(): Boolean {
        return any { it.expired() }
    }

    private fun Peer.isNotInGoodStanding(): Boolean {
        return state.getUnfulfilledRequestCount() >= P2pConstants.PEER_MAX_ADVERTISEMENTS
    }

    fun getBlockRequestLogSize(): Int = blockRequestLog.size

    fun getBlockRequestQueueSize(): Int = blockRequestQueue.size

    fun getTxRequestLogSize(): Int = txRequestLog.size

    fun getTxRequestLogSizeForTx(key: String): Int {
        return txRequestLog[key]?.size ?: 0
    }

    fun getBlockRequestLogSizeForBlock(key: String): Int {
        return blockRequestLog[key]?.size ?: 0
    }

    fun resetState() {
        blockRequestLog.clear()
        blockRequestQueue.clear()
        txRequestLog.clear()
    }
}
