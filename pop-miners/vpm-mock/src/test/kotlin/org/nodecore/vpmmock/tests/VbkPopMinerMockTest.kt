package org.nodecore.vpmmock.tests

import io.kotlintest.shouldBe
import org.junit.Before
import org.junit.Test
import org.veriblock.core.Context
import org.veriblock.core.crypto.Sha256Hash
import org.veriblock.core.crypto.VBK_EMPTY_HASH
import org.veriblock.core.params.defaultRegTestParameters
import org.veriblock.miners.pop.service.mockmining.VeriBlockPopMinerMock
import org.veriblock.sdk.models.Address
import org.veriblock.sdk.models.VeriBlockBlock
import java.nio.ByteBuffer
import java.security.*
import java.security.spec.ECGenParameterSpec

class VbkPopMinerMockTest {
    private val mock = VeriBlockPopMinerMock()
    private val address = Address("VHoWCZrQB4kqLHm1EoNoU8rih7ohyG");


    @Throws(NoSuchAlgorithmException::class, InvalidAlgorithmParameterException::class)
    private fun generateKeyPair(): KeyPair {
        val keyPairGenerator = KeyPairGenerator.getInstance("EC")
        val random = SecureRandom.getInstance("SHA1PRNG")
        random.setSeed(1L)
        keyPairGenerator.initialize(ECGenParameterSpec("secp256k1"), random)
        return keyPairGenerator.generateKeyPair()
    }

    @Before
    fun bootstrap() {
        Context.create(defaultRegTestParameters)
    }

    @Test
    fun mineBlocksEmpty() {
        val tip = mock.mineBtcBlocks(50)
        val head = mock.bitcoinBlockchain.getChainHead()
        head shouldBe tip
    }

    @Test
    fun createVbkPopTx() {
        val key = generateKeyPair()
        val lastKnown = mock.bitcoinBlockchain.getChainHead()!!
        val vbk = VeriBlockBlock(
            1,
            2,
            VBK_EMPTY_HASH.trimToPreviousBlockSize(),
            VBK_EMPTY_HASH.trimToPreviousKeystoneSize(),
            VBK_EMPTY_HASH.trimToPreviousKeystoneSize(),
            Sha256Hash.ZERO_HASH,
            3,
            4,
            5
        )

        val btctx = mock.createBtcTx(vbk, address)
        btctx.rawBytes.size shouldBe 80

        // context will have these 10 blocks
        val ctxtip = mock.mineBtcBlocks(10)

        // add btctx to bitcoin mempool
        mock.bitcoinMempool.add(btctx)

        // block of proof should contain this btctx
        val blockOfProof = mock.mineBtcBlocks(1)!!
        // mempool is clear now
        mock.bitcoinMempool.isEmpty() shouldBe true
        val blockData = mock.bitcoinBlockchain.blockDataStore[blockOfProof.hash]!!
        blockData.size shouldBe 1

        val vbkpoptx = mock.createVbkPopTx(
            blockOfProof.hash,
            0, // tx at index [0]
            key,
            lastKnown
        )!!

        vbkpoptx.bitcoinTransaction shouldBe btctx
        vbkpoptx.blockOfProof shouldBe blockOfProof
        vbkpoptx.publishedBlock shouldBe vbk
        vbkpoptx.blockOfProofContext.size shouldBe 10
        vbkpoptx.blockOfProofContext.last() shouldBe ctxtip
    }
}
