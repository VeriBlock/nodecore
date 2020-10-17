package org.veriblock.spv.service

import io.kotest.matchers.shouldBe
import org.apache.commons.lang3.RandomStringUtils
import org.junit.*
import org.veriblock.core.Context
import org.veriblock.core.crypto.PreviousBlockVbkHash
import org.veriblock.core.crypto.PreviousKeystoneVbkHash
import org.veriblock.core.crypto.Sha256Hash
import org.veriblock.core.crypto.VBK_HASH_LENGTH
import org.veriblock.core.crypto.VBK_PREVIOUS_BLOCK_HASH_LENGTH
import org.veriblock.core.crypto.VBK_PREVIOUS_KEYSTONE_HASH_LENGTH
import org.veriblock.core.crypto.VbkHash
import org.veriblock.core.crypto.asVbkHash
import org.veriblock.core.crypto.asVbkPreviousBlockHash
import org.veriblock.core.crypto.asVbkPreviousKeystoneHash
import org.veriblock.core.params.defaultMainNetParameters
import org.veriblock.sdk.blockchain.store.StoredVeriBlockBlock
import org.veriblock.sdk.models.VeriBlockBlock
import org.veriblock.spv.SpvConfig
import org.veriblock.spv.SpvContext
import java.io.File
import java.math.BigInteger
import java.security.MessageDigest
import kotlin.random.Random

class BlockStoreTest {
    private lateinit var blockStore: BlockStore
    private lateinit var baseDir: File

    init {
        Context.set(defaultMainNetParameters)
    }

    @Before
    fun beforeTest() {
        baseDir = File("blockStoreTests").also {
            it.mkdir()
        }
        blockStore = BlockStore(defaultMainNetParameters, baseDir)
    }

    @After
    fun after() {
        baseDir.deleteRecursively()
    }

    @Test
    fun `should store and restore the given list of blocks`() {
        // Given
        val storedVeriBlockBlocks = (1..100).map {
            StoredVeriBlockBlock(
                randomVeriBlockBlock(height = it),
                BigInteger.ONE,
                randomVbkHash()
            )
        }
        // When
        blockStore.writeBlocks(storedVeriBlockBlocks)
        // Then
        storedVeriBlockBlocks.forEach {
            blockStore.readBlock(it.hash) shouldBe it
        }
    }

    @Test
    fun `should store and restore the given block`() {
        // Given
        val storedVeriBlockBlock = StoredVeriBlockBlock(
            randomVeriBlockBlock(),
            BigInteger.ONE,
            randomVbkHash()
        )
        // When
        blockStore.writeBlock(storedVeriBlockBlock)
        // Then
        blockStore.readBlock(storedVeriBlockBlock.hash) shouldBe storedVeriBlockBlock
    }

    @Test
    fun `should properly update the block store tip`() {
        // Given
        val storedVeriBlockBlock = StoredVeriBlockBlock(
            randomVeriBlockBlock(),
            BigInteger.ONE,
            randomVbkHash()
        )
        // When
        blockStore.setTip(storedVeriBlockBlock)
        // Then
        blockStore.getTip() shouldBe storedVeriBlockBlock
    }

    @Test
    fun `shouldn't be able to restore a block with a wrong hash`() {
        // Given
        val storedVeriBlockBlock = StoredVeriBlockBlock(
            randomVeriBlockBlock(),
            BigInteger.ONE,
            randomVbkHash()
        )
        // When
        blockStore.writeBlock(storedVeriBlockBlock)
        // Then
        blockStore.readBlock(randomVbkHash()) shouldBe null
    }

    @Test
    fun `should restore the blocks which were stored by a previous block store object`() {
        // Given
        val storedVeriBlockBlocks = (1..100).map {
            StoredVeriBlockBlock(
                randomVeriBlockBlock(height = it),
                BigInteger.ONE,
                randomVbkHash()
            )
        }
        // When
        blockStore.writeBlocks(storedVeriBlockBlocks)
        blockStore = BlockStore(defaultMainNetParameters, baseDir)
        // Then
        storedVeriBlockBlocks.forEach {
            blockStore.readBlock(it.hash) shouldBe it
        }
    }
}

private var messageDigest = MessageDigest.getInstance("SHA-256")
private fun randomByteArray(size: Int): ByteArray = ByteArray(size) { randomInt(256).toByte() }
private fun randomInt(bound: Int) = Random.nextInt(bound)
private fun randomInt(min: Int, max: Int) = Random.nextInt(max - min) + min
private fun randomLong(min: Long, max: Long) = (Random.nextDouble() * (max - min)).toLong() + min
private fun randomAlphabeticString(length: Int = 10): String = RandomStringUtils.randomAlphabetic(length)
private fun randomVbkHash(): VbkHash = randomByteArray(VBK_HASH_LENGTH).asVbkHash()
private fun randomPreviousBlockVbkHash(): PreviousBlockVbkHash = randomByteArray(VBK_PREVIOUS_BLOCK_HASH_LENGTH).asVbkPreviousBlockHash()
private fun randomPreviousKeystoneVbkHash(): PreviousKeystoneVbkHash = randomByteArray(VBK_PREVIOUS_KEYSTONE_HASH_LENGTH).asVbkPreviousKeystoneHash()
private fun randomSha256Hash(): Sha256Hash = Sha256Hash.wrap(messageDigest.digest(randomAlphabeticString().toByteArray()))
private fun randomVeriBlockBlock(
    height: Int = randomInt(1, 65535),
    version: Short = randomInt(1, Short.MAX_VALUE.toInt()).toShort(),
    previousBlock: PreviousBlockVbkHash = randomPreviousBlockVbkHash(),
    previousKeystone: PreviousKeystoneVbkHash = randomPreviousKeystoneVbkHash(),
    secondPreviousKeystone: PreviousKeystoneVbkHash = randomPreviousKeystoneVbkHash(),
    merkleRoot: Sha256Hash = randomSha256Hash(),
    timestamp: Int = randomInt(1, 65535),
    difficulty: Int = randomInt(1, 65535),
    nonce: Long = randomLong(1, 35535),
    precomputedHash: VbkHash = randomVbkHash()
): VeriBlockBlock {
    return VeriBlockBlock(
        height,
        version,
        previousBlock,
        previousKeystone,
        secondPreviousKeystone,
        merkleRoot,
        timestamp,
        difficulty,
        nonce,
        precomputedHash
    )
}
