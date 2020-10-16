package org.veriblock.spv.service

import io.kotest.matchers.shouldBe
import org.apache.commons.lang3.RandomStringUtils
import org.junit.*
import org.veriblock.core.Context
import org.veriblock.core.crypto.Sha256Hash
import org.veriblock.core.crypto.VBlakeHash
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
    private val spvContext = SpvContext()
    private lateinit var blockStore: BlockStore
    private lateinit var baseDir: File

    init {
        Context.set(defaultMainNetParameters)
        spvContext.init(SpvConfig(useLocalNode = true))
    }

    @Before
    fun beforeTest() {
        baseDir = File("blockStoreTests").also {
            it.mkdir()
        }
        blockStore = BlockStore(spvContext.networkParameters, baseDir)
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
                randomVBlakeHash()
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
            randomVBlakeHash()
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
            randomVBlakeHash()
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
            randomVBlakeHash()
        )
        // When
        blockStore.writeBlock(storedVeriBlockBlock)
        // Then
        blockStore.readBlock(randomVBlakeHash()) shouldBe null
    }

    @Ignore
    @Test
    fun `should restore the blocks which were stored by a previous block store object`() {
        // Given
        val storedVeriBlockBlocks = (1..100).map {
            StoredVeriBlockBlock(
                randomVeriBlockBlock(height = it),
                BigInteger.ONE,
                randomVBlakeHash()
            )
        }
        blockStore.writeBlocks(storedVeriBlockBlocks)
        // When
        val blockStore2 = BlockStore(spvContext.networkParameters, baseDir)
        // Then
        storedVeriBlockBlocks.forEach {
            blockStore2.readBlock(it.hash) shouldBe it
        }
    }
}

private var messageDigest = MessageDigest.getInstance("SHA-256")
private fun randomByteArray(size: Int): ByteArray = ByteArray(size) { randomInt(256).toByte() }
private fun randomInt(bound: Int) = Random.nextInt(bound)
private fun randomInt(min: Int, max: Int) = Random.nextInt(max - min) + min
private fun randomLong(min: Long, max: Long) = (Random.nextDouble() * (max - min)).toLong() + min
private fun randomAlphabeticString(length: Int = 10): String = RandomStringUtils.randomAlphabetic(length)
private fun randomVBlakeHash(): VBlakeHash = VBlakeHash.wrap(randomByteArray(VBlakeHash.VERIBLOCK_LENGTH))
private fun randomSha256Hash(): Sha256Hash = Sha256Hash.wrap(messageDigest.digest(randomAlphabeticString().toByteArray()))
private fun randomVeriBlockBlock(
    height: Int = randomInt(1, 65535),
    version: Short = randomInt(1, Short.MAX_VALUE.toInt()).toShort(),
    previousBlock: VBlakeHash = randomVBlakeHash(),
    previousKeystone: VBlakeHash = randomVBlakeHash(),
    secondPreviousKeystone: VBlakeHash = randomVBlakeHash(),
    merkleRoot: Sha256Hash = randomSha256Hash(),
    timestamp: Int = randomInt(1, 65535),
    difficulty: Int = randomInt(1, 65535),
    nonce: Long = randomLong(1, 35535)
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
        nonce
    )
}
