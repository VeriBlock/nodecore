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
import nodecore.api.grpc.RpcAcknowledgement
import nodecore.api.grpc.RpcEvent
import nodecore.p2p.P2pEventBus
import nodecore.p2p.Peer
import nodecore.p2p.PeerState
import nodecore.p2p.createFakeParameters
import org.junit.BeforeClass
import org.junit.Test
import org.veriblock.core.Context
import java.math.BigInteger

class EventRegistrarTests {

    companion object {

        @JvmStatic
        @BeforeClass
        fun beforeClass() {
            Context.create(createFakeParameters("mainnet", BigInteger.ONE, 1))
        }
    }

    @Test
    fun newEventWhenNotAnnouncedAndNotAnnounceMessage() {
        val peer: Peer = mockk(relaxed = true) {
            every { state } returns PeerState().apply {
                setAnnounced(false)
            }
        }
        var misbehavior = false
        P2pEventBus.peerMisbehavior.register(this) {
            misbehavior = true
        }
        val event = RpcEvent.newBuilder()
            .setAcknowledgement(RpcAcknowledgement.newBuilder())
            .build()
        P2pEventBus.newEvent(event, peer)
        misbehavior shouldBe true
    }

    @Test
    fun newEventWhenHasAnnouncedButWrongVersion() {
        val peer: Peer = mockk(relaxed = true) {
            every { state } returns PeerState().apply {
                setAnnounced(true)
            }
            every { protocolVersion } returns -1
        }
        val event = RpcEvent.newBuilder()
            .setAcknowledgement(RpcAcknowledgement.newBuilder())
            .build()
        P2pEventBus.newEvent(event, peer)
        verify {
            peer.disconnect()
        }
    }
}
