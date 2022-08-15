package org.veriblock.spv.net.impl

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import nodecore.api.grpc.RpcTransactionAnnounce
import nodecore.api.grpc.RpcTransactionRequest
import nodecore.api.grpc.utilities.extensions.toByteString
import nodecore.p2p.Peer
import nodecore.p2p.PeerTable
import nodecore.p2p.event.P2pEvent
import org.junit.Before
import org.junit.Test
import org.veriblock.core.Context
import org.veriblock.core.crypto.EMPTY_VBK_TX
import org.veriblock.core.crypto.VbkTxId
import org.veriblock.core.params.defaultTestNetParameters
import org.veriblock.sdk.models.asCoin
import org.veriblock.spv.SpvConfig
import org.veriblock.spv.SpvContext
import org.veriblock.spv.model.asStandardAddress
import org.veriblock.spv.net.PeerEventListener
import org.veriblock.spv.service.PendingTransactionContainer

class PeerEventListenerTest {
    private lateinit var spvContext: SpvContext
    private lateinit var pendingTransactionContainer: PendingTransactionContainer
    private lateinit var peerTable: PeerTable
    private lateinit var peer: Peer
    private lateinit var peerEventListener: PeerEventListener

    @Before
    fun setUp() {
        Context.set(defaultTestNetParameters)
        spvContext = SpvContext(SpvConfig(defaultTestNetParameters, connectDirectlyTo = listOf("localhost")))
        pendingTransactionContainer = mockk(relaxed = true)
        peerTable = mockk(relaxed = true)
        peer = mockk(relaxed = true)
        peerEventListener = PeerEventListener(spvContext, peerTable, mockk(relaxed = true), pendingTransactionContainer)
    }

    @Test
    fun onTransactionRequestWhenNotFindTx() {
        val txIds = listOf(EMPTY_VBK_TX)

        every { pendingTransactionContainer.getTransaction(any()) } returns null
        every { peer.send((any())) } returns true

        peerEventListener.onTransactionRequest(createTxRequest(txIds))

        verify(exactly = 1 ) { pendingTransactionContainer.getTransaction(any()) }
        verify(exactly = 1 ) { peer.send(any()) }
    }

    private fun createTxRequest(txIds: List<VbkTxId>) = P2pEvent(
        peer, "", false,
        RpcTransactionRequest.newBuilder().apply {
            addAllTransactions(txIds.map {
                RpcTransactionAnnounce.newBuilder().apply {
                    type = RpcTransactionAnnounce.Type.NORMAL
                    txId = it.bytes.toByteString()
                }.build()
            })
        }.build()
    )

    @Test
    fun onTransactionRequestWhenFindTx() {
        val txIds = listOf(EMPTY_VBK_TX)
        val outputs = listOf(
            Output("V7GghFKRA6BKqtHD7LTdT2ao93DRNA".asStandardAddress(), 3499999999L.asCoin())
        )
        val standardTransaction = StandardTransaction(
            "V8dy5tWcP7y36kxiJwxKPKUrWAJbjs", 3500000000L, outputs, 5904L, spvContext.networkParameters
        )
        val pub = byteArrayOf(1, 2, 3)
        val sign = byteArrayOf(3, 2, 1)
        standardTransaction.addSignature(sign, pub)

        every { pendingTransactionContainer.getTransaction(txIds[0]) } returns standardTransaction
        every { peer.send((any())) } returns true

        peerEventListener.onTransactionRequest(createTxRequest(txIds))

        verify(exactly = 1 ) { pendingTransactionContainer.getTransaction(txIds[0]) }
        verify(exactly = 1 ) { peer.send(any()) }
    }
}
