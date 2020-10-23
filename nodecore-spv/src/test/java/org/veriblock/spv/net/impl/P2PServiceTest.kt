package org.veriblock.spv.net.impl

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import nodecore.api.grpc.VeriBlockMessages
import org.junit.Before
import org.junit.Test
import org.veriblock.core.Context
import org.veriblock.core.crypto.Sha256Hash
import org.veriblock.core.params.defaultTestNetParameters
import org.veriblock.sdk.models.asCoin
import org.veriblock.spv.SpvConfig
import org.veriblock.spv.SpvContext
import org.veriblock.spv.model.Output
import org.veriblock.spv.model.StandardTransaction
import org.veriblock.spv.model.asStandardAddress
import org.veriblock.spv.net.P2PService
import org.veriblock.spv.net.SpvPeer
import org.veriblock.spv.service.PendingTransactionContainer

class P2PServiceTest {
    private val spvContext = SpvContext()
    private lateinit var pendingTransactionContainer: PendingTransactionContainer
    private lateinit var peer: SpvPeer
    private lateinit var p2PService: P2PService

    @Before
    fun setUp() {
        Context.set(defaultTestNetParameters)
        spvContext.init(SpvConfig("testnet", connectDirectlyTo = listOf("localhost")))
        pendingTransactionContainer = mockk(relaxed = true)
        peer = mockk(relaxed = true)
        p2PService = P2PService(pendingTransactionContainer, spvContext.networkParameters)
    }

    @Test
    fun onTransactionRequestWhenNotFindTx() {
        val txIds = listOf(Sha256Hash.ZERO_HASH)

        every { pendingTransactionContainer.getTransaction(any()) } returns null
        every { peer.sendMessage((any<VeriBlockMessages.Event>())) } returns Unit

        p2PService.onTransactionRequest(txIds, peer)

        verify(exactly = 1 ) { pendingTransactionContainer.getTransaction(any()) }
        verify(exactly = 1 ) { peer.sendMessage(any<VeriBlockMessages.Event>()) }
    }

    @Test
    fun onTransactionRequestWhenFindTx() {
        val txIds = listOf(Sha256Hash.ZERO_HASH)
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
        every { peer.sendMessage((any<VeriBlockMessages.Event>())) } returns Unit

        p2PService.onTransactionRequest(txIds, peer)

        verify(exactly = 1 ) { pendingTransactionContainer.getTransaction(txIds[0]) }
        verify(exactly = 1 ) { peer.sendMessage(any<VeriBlockMessages.Event>()) }
    }
}
