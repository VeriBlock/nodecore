// VeriBlock NodeCore
// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.tests.p2p

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import nodecore.api.grpc.RpcBlockHeader
import nodecore.api.grpc.RpcTransactionAnnounce
import nodecore.p2p.BlockRequest
import nodecore.p2p.P2pConstants
import nodecore.p2p.P2pEventBus
import nodecore.p2p.Peer
import nodecore.p2p.PeerState
import nodecore.p2p.TrafficManager
import nodecore.p2p.TransactionRequest
import nodecore.p2p.event.PeerMisbehaviorEvent
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Ignore
import org.junit.Test
import org.veriblock.core.utilities.Utility

class TrafficManagerTests {

    companion object {
        private lateinit var sut: TrafficManager

        @JvmStatic
        @BeforeClass
        fun beforeClass() {
            sut = TrafficManager()
        }
    }

    @Before
    fun before() {
        sut.resetState()
    }

    @Test
    fun requestBlocksSingleBlock() {
        val peer: Peer = mockk(relaxed = true) {
            every { addressKey } returns "a1"
            every { state } returns PeerState()
        }
        val request = BlockRequest("test", RpcBlockHeader.newBuilder().build(), peer)
        sut.requestBlocks(listOf(request))
        sut.getBlockRequestLogSize().toLong() shouldBe 1
        verify(exactly = 1) {
            peer.send(any())
        }
        peer.state.getUnfulfilledRequestCount().toLong() shouldBe 1
    }

    @Test
    fun requestBlocksWhenPeerNotInGoodStanding() {
        val peer: Peer = mockk(relaxed = true) {
            every { addressKey } returns "a1"
            every { state } returns PeerState()
        }
        for (i in 0 until P2pConstants.PEER_MAX_ADVERTISEMENTS) {
            peer.state.incrementUnfulfilledRequests()
        }
        val request = BlockRequest("test", RpcBlockHeader.newBuilder().build(), peer)
        P2pEventBus.peerMisbehavior.register(this) {
            it.reason shouldBe PeerMisbehaviorEvent.Reason.UNFULFILLED_REQUEST_LIMIT
        }
        sut.requestBlocks(listOf(request))
        sut.getBlockRequestLogSize().toLong() shouldBe 0
        verify(exactly = 0) {
            peer.send(any())
        }
    }

    @Test
    fun requestBlocksWhenPending() {
        val peer: Peer = mockk(relaxed = true) {
            every { addressKey } returns "a1"
            every { state } returns PeerState()
        }
        val request = BlockRequest("test", RpcBlockHeader.newBuilder().build(), peer)
        val requests = ArrayList<BlockRequest>()
        requests.add(request)
        requests.add(request)
        sut.requestBlocks(requests)
        sut.getBlockRequestLogSize().toLong() shouldBe 1
        sut.getBlockRequestQueueSize().toLong() shouldBe 1
        verify(exactly = 1) {
            peer.send(any())
        }
        peer.state.getUnfulfilledRequestCount().toLong() shouldBe 1
    }

    @Test
    fun blockReceivedWhenNotRequested() {
        val result = sut.blockReceived("test", "p1")
        result shouldBe false
    }

    @Test
    fun blockReceivedWhenPeerDidNotRequest() {
        val peer: Peer = mockk(relaxed = true) {
            every { addressKey } returns "p1"
            every { state } returns PeerState()
        }
        val request = BlockRequest("test", RpcBlockHeader.newBuilder().build(), peer)
        sut.requestBlocks(listOf(request))
        val result = sut.blockReceived("test", "p2")
        result shouldBe false
        sut.getBlockRequestLogSize().toLong() shouldBe 1
        peer.state.getUnfulfilledRequestCount().toLong() shouldBe 1
    }

    @Test
    fun blockReceivedWhenOnePeerRequested() {
        val peer: Peer = mockk(relaxed = true) {
            every { addressKey } returns "p1"
            every { state } returns PeerState()
        }
        val request = BlockRequest("test", RpcBlockHeader.newBuilder().build(), peer)
        sut.requestBlocks(listOf(request))
        val result = sut.blockReceived("test", "p1")
        result shouldBe true
        sut.getBlockRequestLogSize().toLong() shouldBe 0
        peer.state.getUnfulfilledRequestCount().toLong() shouldBe 0
    }

    @Test
    fun blockReceivedWhenSecondPeerQueued() {
        val peer: Peer = mockk(relaxed = true) {
            every { addressKey } returns "p1"
            every { state } returns PeerState()
        }
        val peer2: Peer = mockk(relaxed = true) {
            every { addressKey } returns "p2"
            every { state } returns PeerState()
        }
        val requests = ArrayList<BlockRequest>()
        requests.add(BlockRequest("test", RpcBlockHeader.newBuilder().build(), peer))
        requests.add(BlockRequest("test", RpcBlockHeader.newBuilder().build(), peer2))
        sut.requestBlocks(requests)
        val result = sut.blockReceived("test", "p1")
        result shouldBe true
        sut.getBlockRequestLogSize().toLong() shouldBe 0
        sut.getBlockRequestQueueSize().toLong() shouldBe 0
        peer.state.getUnfulfilledRequestCount().toLong() shouldBe 0
    }

    @Test
    fun blockReceivedWhenMultipleRequested() {
        val peer: Peer = mockk(relaxed = true) {
            every { addressKey } returns "p1"
            every { state } returns PeerState()
        }
        val peer2: Peer = mockk(relaxed = true) {
            every { addressKey } returns "p2"
            every { state } returns PeerState()
        }
        val request = BlockRequest("test", RpcBlockHeader.newBuilder().build(), peer)
        sut.requestBlocks(listOf(request))
        // Expire the request
        request.requestedAt =
            Utility.getCurrentTimeSeconds() - P2pConstants.PEER_REQUEST_TIMEOUT - 10
        sut.requestBlocks(listOf(BlockRequest("test", RpcBlockHeader.newBuilder().build(), peer2)))
        val result = sut.blockReceived("test", "p1")
        result shouldBe true
        sut.getBlockRequestLogSize().toLong() shouldBe 1
        sut.getBlockRequestLogSizeForBlock("test").toLong() shouldBe 1
        sut.getBlockRequestQueueSize().toLong() shouldBe 0
        peer.state.getUnfulfilledRequestCount().toLong() shouldBe 0
        peer2.state.getUnfulfilledRequestCount().toLong() shouldBe 1
    }

    @Test
    fun blockNotFoundWhenPeerDidNotRequest() {
        val peer: Peer = mockk(relaxed = true) {
            every { addressKey } returns "p1"
            every { state } returns PeerState()
        }
        val request = BlockRequest("test", RpcBlockHeader.newBuilder().build(), peer)
        sut.requestBlocks(listOf(request))
        sut.blockNotFound("test", "p2")
        sut.getBlockRequestLogSize().toLong() shouldBe 1
        peer.state.getUnfulfilledRequestCount().toLong() shouldBe 1
    }

    @Test
    fun blockNotFoundWhenOnePeerRequested() {
        val peer: Peer = mockk(relaxed = true) {
            every { addressKey } returns "p1"
            every { state } returns PeerState()
        }
        val request = BlockRequest("test", RpcBlockHeader.newBuilder().build(), peer)
        sut.requestBlocks(listOf(request))
        sut.blockNotFound("test", "p1")
        sut.getBlockRequestLogSize().toLong() shouldBe 0
        peer.state.getUnfulfilledRequestCount().toLong() shouldBe 0
    }

    @Test
    fun blockNotFoundWhenSecondPeerQueued() {
        val peer: Peer = mockk(relaxed = true) {
            every { addressKey } returns "p1"
            every { state } returns PeerState()
        }
        val peer2: Peer = mockk(relaxed = true) {
            every { addressKey } returns "p2"
            every { state } returns PeerState()
        }
        val requests = ArrayList<BlockRequest>()
        requests.add(BlockRequest("test", RpcBlockHeader.newBuilder().build(), peer))
        requests.add(BlockRequest("test", RpcBlockHeader.newBuilder().build(), peer2))
        sut.requestBlocks(requests)
        sut.blockNotFound("test", "p1")
        sut.getBlockRequestLogSize().toLong() shouldBe 0
        sut.getBlockRequestQueueSize().toLong() shouldBe 1
        peer.state.getUnfulfilledRequestCount().toLong() shouldBe 0
    }

    @Test
    fun blockNotFoundWhenMultipleRequested() {
        val peer: Peer = mockk(relaxed = true) {
            every { addressKey } returns "p1"
            every { state } returns PeerState()
        }
        val peer2: Peer = mockk(relaxed = true) {
            every { addressKey } returns "p2"
            every { state } returns PeerState()
        }
        val request = BlockRequest("test", RpcBlockHeader.newBuilder().build(), peer)
        sut.requestBlocks(listOf(request))
        // Expire the request
        request.requestedAt =
            Utility.getCurrentTimeSeconds() - P2pConstants.PEER_REQUEST_TIMEOUT - 10
        sut.requestBlocks(listOf(BlockRequest("test", RpcBlockHeader.newBuilder().build(), peer2)))
        sut.blockNotFound("test", "p1")
        sut.getBlockRequestLogSize().toLong() shouldBe 1
        sut.getBlockRequestLogSizeForBlock("test").toLong() shouldBe 1
        sut.getBlockRequestQueueSize().toLong() shouldBe 0
        peer.state.getUnfulfilledRequestCount().toLong() shouldBe 0
        peer2.state.getUnfulfilledRequestCount().toLong() shouldBe 1
    }

    @Test
    fun blockHasBeenRequestedWhenNotRequested() {
        val result = sut.blockHasBeenRequested("test")
        result shouldBe false
    }

    @Test
    fun blockHasBeenRequestedWhenRequested() {
        val peer: Peer = mockk(relaxed = true) {
            every { addressKey } returns "p1"
            every { state } returns PeerState()
        }
        val request = BlockRequest("test", RpcBlockHeader.newBuilder().build(), peer)
        sut.requestBlocks(listOf(request))
        val result = sut.blockHasBeenRequested("test")
        result shouldBe true
    }

    @Test
    fun requestTransactionsSingleTransaction() {
        val peer: Peer = mockk(relaxed = true) {
            every { addressKey } returns "a1"
            every { state } returns PeerState()
        }
        val request = TransactionRequest("test", RpcTransactionAnnounce.newBuilder().build(), peer)
        sut.requestTransactions(listOf(request))
        sut.getTxRequestLogSize().toLong() shouldBe 1
        verify(exactly = 1) {
            peer.send(any())
        }
        peer.state.getUnfulfilledRequestCount().toLong() shouldBe 1
    }

    @Test
    fun requestTransactionsWhenPeerNotInGoodStanding() {
        val peer: Peer = mockk(relaxed = true) {
            every { addressKey } returns "a1"
            every { state } returns PeerState()
        }
        for (i in 0 until P2pConstants.PEER_MAX_ADVERTISEMENTS) {
            peer.state.incrementUnfulfilledRequests()
        }
        val request = TransactionRequest("test", RpcTransactionAnnounce.newBuilder().build(), peer)
        P2pEventBus.peerMisbehavior.register(this) {
            it.reason shouldBe PeerMisbehaviorEvent.Reason.UNFULFILLED_REQUEST_LIMIT
        }
        sut.requestTransactions(listOf(request))
        sut.getTxRequestLogSize().toLong() shouldBe 0
        verify(exactly = 0) {
            peer.send(any())
        }
    }

    @Test
    fun requestTransactionsWhenMoreThanConcurrentLimit() {
        val peers = ArrayList<Peer>(P2pConstants.CONCURRENT_TX_REQUESTS + 1)
        val requests = ArrayList<TransactionRequest>(P2pConstants.CONCURRENT_TX_REQUESTS + 1)
        for (i in 0..P2pConstants.CONCURRENT_TX_REQUESTS) {
            val peer: Peer = mockk(relaxed = true) {
                every { addressKey } returns i.toString()
                every { state } returns PeerState()
            }
            peers.add(peer)
            requests.add(TransactionRequest("test", RpcTransactionAnnounce.newBuilder().build(), peer))
        }
        sut.requestTransactions(requests)
        sut.getTxRequestLogSize().toLong() shouldBe 1
        sut.getTxRequestLogSizeForTx("test").toLong() shouldBe 3
        for (i in peers.indices) {
            if (i < peers.size - 1) {
                verify(exactly = 1) {
                    peers[i].send(any())
                }
                peers[i].state.getUnfulfilledRequestCount().toLong() shouldBe 1
            } else {
                verify(exactly = 0) {
                    peers[i].send(any())
                }
                peers[i].state.getUnfulfilledRequestCount().toLong() shouldBe 0
            }
        }
    }

    @Test
    fun transactionReceivedWhenNotRequested() {
        val result = sut.transactionReceived("test", "p1")
        result shouldBe false
    }

    @Test
    fun transactionReceivedWhenPeerDidNotRequest() {
        val peer: Peer = mockk(relaxed = true) {
            every { addressKey } returns "p1"
            every { state } returns PeerState()
        }
        val request = TransactionRequest("test", RpcTransactionAnnounce.newBuilder().build(), peer)
        sut.requestTransactions(listOf(request))
        val result = sut.transactionReceived("test", "p2")
        result shouldBe false
        sut.getTxRequestLogSize().toLong() shouldBe 1
        peer.state.getUnfulfilledRequestCount().toLong() shouldBe 1
    }

    @Test
    fun transactionReceivedWhenOnePeerRequested() {
        val peer: Peer = mockk(relaxed = true) {
            every { addressKey } returns "p1"
            every { state } returns PeerState()
        }
        val request = TransactionRequest("test", RpcTransactionAnnounce.newBuilder().build(), peer)
        sut.requestTransactions(listOf(request))
        val result = sut.transactionReceived("test", "p1")
        result shouldBe true
        sut.getTxRequestLogSize().toLong() shouldBe 0
        peer.state.getUnfulfilledRequestCount().toLong() shouldBe 0
    }

    @Test
    fun transactionReceivedWhenSecondPeerRequested() {
        val peer: Peer = mockk(relaxed = true) {
            every { addressKey } returns "p1"
            every { state } returns PeerState()
        }
        val peer2: Peer = mockk(relaxed = true) {
            every { addressKey } returns "p2"
            every { state } returns PeerState()
        }
        val requests = ArrayList<TransactionRequest>()
        requests.add(TransactionRequest("test", RpcTransactionAnnounce.newBuilder().build(), peer))
        requests.add(TransactionRequest("test", RpcTransactionAnnounce.newBuilder().build(), peer2))
        sut.requestTransactions(requests)
        val result = sut.transactionReceived("test", "p1")
        result shouldBe true
        sut.getTxRequestLogSize().toLong() shouldBe 1
        sut.getTxRequestLogSizeForTx("test").toLong() shouldBe 1
        peer.state.getUnfulfilledRequestCount().toLong() shouldBe 0
        peer2.state.getUnfulfilledRequestCount().toLong() shouldBe 1
    }

    @Test
    fun transactionNotFoundWhenPeerDidNotRequest() {
        val peer: Peer = mockk(relaxed = true) {
            every { addressKey } returns "p1"
            every { state } returns PeerState()
        }
        val request = TransactionRequest("test", RpcTransactionAnnounce.newBuilder().build(), peer)
        sut.requestTransactions(listOf(request))
        sut.transactionNotFound("test", "p2")
        sut.getTxRequestLogSize().toLong() shouldBe 1
        peer.state.getUnfulfilledRequestCount().toLong() shouldBe 1
    }

    @Test
    fun transactionNotFoundWhenOnePeerRequested() {
        val peer: Peer = mockk(relaxed = true) {
            every { addressKey } returns "p1"
            every { state } returns PeerState()
        }
        val request = TransactionRequest("test", RpcTransactionAnnounce.newBuilder().build(), peer)
        sut.requestTransactions(listOf(request))
        sut.transactionNotFound("test", "p1")
        sut.getTxRequestLogSize().toLong() shouldBe 0
        peer.state.getUnfulfilledRequestCount().toLong() shouldBe 0
    }

    @Test
    fun transactionNotFoundWhenMultipleRequested() {
        val peer: Peer = mockk(relaxed = true) {
            every { addressKey } returns "p1"
            every { state } returns PeerState()
        }
        val peer2: Peer = mockk(relaxed = true) {
            every { addressKey } returns "p2"
            every { state } returns PeerState()
        }
        val request = TransactionRequest("test", RpcTransactionAnnounce.newBuilder().build(), peer)
        sut.requestTransactions(listOf(request))
        // Expire the request
        request.requestedAt =
            Utility.getCurrentTimeSeconds() - P2pConstants.PEER_REQUEST_TIMEOUT - 10
        sut.requestTransactions(
            listOf(
                TransactionRequest(
                    "test",
                    RpcTransactionAnnounce.newBuilder().build(),
                    peer2
                )
            )
        )
        sut.transactionNotFound("test", "p1")
        sut.getTxRequestLogSize().toLong() shouldBe 1
        sut.getTxRequestLogSizeForTx("test").toLong() shouldBe 1
        peer.state.getUnfulfilledRequestCount().toLong() shouldBe 0
        peer2.state.getUnfulfilledRequestCount().toLong() shouldBe 1
    }

    @Test
    fun manageWhenFirstExpiredAndSecondQueued() {
        val peer: Peer = mockk(relaxed = true) {
            every { addressKey } returns "p1"
            every { state } returns PeerState()
        }
        val peer2: Peer = mockk(relaxed = true) {
            every { addressKey } returns "p2"
            every { state } returns PeerState()
        }
        val request = BlockRequest("test", RpcBlockHeader.newBuilder().build(), peer)
        sut.requestBlocks(listOf(request))
        sut.requestBlocks(listOf(BlockRequest("test", RpcBlockHeader.newBuilder().build(), peer2)))
        request.requestedAt =
            Utility.getCurrentTimeSeconds() - P2pConstants.PEER_REQUEST_TIMEOUT - 10
        sut.manage()
        sut.getBlockRequestLogSize().toLong() shouldBe 1
        sut.getBlockRequestLogSizeForBlock("test").toLong() shouldBe 2
        sut.getBlockRequestQueueSize().toLong() shouldBe 0
        peer.state.getUnfulfilledRequestCount().toLong() shouldBe 1
        peer2.state.getUnfulfilledRequestCount().toLong() shouldBe 1
    }

    @Test
    fun manageWhenFirstExpiredAndSecondNotInGoodStanding() {
        val peer: Peer = mockk(relaxed = true) {
            every { addressKey } returns "p1"
            every { state } returns PeerState()
        }
        val peer2: Peer = mockk(relaxed = true) {
            every { addressKey } returns "p2"
            every { state } returns PeerState()
        }
        P2pEventBus.peerMisbehavior.register(this) {
            it.reason shouldBe PeerMisbehaviorEvent.Reason.UNFULFILLED_REQUEST_LIMIT
        }
        val request = BlockRequest("test", RpcBlockHeader.newBuilder().build(), peer)
        sut.requestBlocks(listOf(request))
        sut.requestBlocks(listOf(BlockRequest("test", RpcBlockHeader.newBuilder().build(), peer2)))
        for (i in 0 until P2pConstants.PEER_MAX_ADVERTISEMENTS) {
            peer2.state.incrementUnfulfilledRequests()
        }
        request.requestedAt = Utility.getCurrentTimeSeconds() - P2pConstants.PEER_REQUEST_TIMEOUT - 10
        sut.manage()
        sut.getBlockRequestLogSize().toLong() shouldBe 1
        sut.getBlockRequestLogSizeForBlock("test").toLong() shouldBe 1
        sut.getBlockRequestQueueSize().toLong() shouldBe 0
        peer.state.getUnfulfilledRequestCount().toLong() shouldBe 1
        verify(exactly = 1) {
            peer.send(any())
        }
        verify(exactly = 0) {
            peer2.send(any())
        }
    }

    @Ignore // TODO Threading should not be static for this to be testable without breaking other tests
    @Test
    fun shutdown() {
        sut.shutdown()
        sut.getBlockRequestLogSize().toLong() shouldBe 0
        sut.getBlockRequestQueueSize().toLong() shouldBe 0
        sut.getTxRequestLogSize().toLong() shouldBe 0
    }
}
