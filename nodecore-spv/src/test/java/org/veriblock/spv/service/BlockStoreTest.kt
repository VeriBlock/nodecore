package org.veriblock.spv.service

import org.apache.commons.lang3.RandomStringUtils
import org.junit.Assert
import org.junit.Test
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

    init {
        Context.set(defaultMainNetParameters)
        spvContext.init(SpvConfig(useLocalNode = true))
    }

    @Test
    fun `should store and restore the given list of blocks`() {
        // Given
        val blockStore = BlockStore(spvContext.networkParameters, File("."))
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
            Assert.assertEquals(blockStore.readBlock(it.hash), it)
        }
    }

    @Test
    fun `should store and restore the given block`() {
        // Given
        val blockStore = BlockStore(spvContext.networkParameters, File("."))
        val storedVeriBlockBlock = StoredVeriBlockBlock(
            randomVeriBlockBlock(),
            BigInteger.ONE,
            randomVBlakeHash()
        )
        // When
        blockStore.writeBlock(storedVeriBlockBlock)
        // Then
        Assert.assertEquals(blockStore.readBlock(storedVeriBlockBlock.hash), storedVeriBlockBlock)
    }

    @Test
    fun `should properly update the block store tip`() {
        // Given
        val blockStore = BlockStore(spvContext.networkParameters, File("."))
        val storedVeriBlockBlock = StoredVeriBlockBlock(
            randomVeriBlockBlock(),
            BigInteger.ONE,
            randomVBlakeHash()
        )
        // When
        blockStore.setTip(storedVeriBlockBlock)
        // Then
        Assert.assertEquals(blockStore.getTip(), storedVeriBlockBlock)
    }

    @Test
    fun `shouldn't be able to restore a block with a wrong hash`() {
        // Given
        val blockStore = BlockStore(spvContext.networkParameters, File("."))
        val storedVeriBlockBlock = StoredVeriBlockBlock(
            randomVeriBlockBlock(),
            BigInteger.ONE,
            randomVBlakeHash()
        )
        // When
        blockStore.writeBlock(storedVeriBlockBlock)
        // Then
        Assert.assertEquals(blockStore.readBlock(VBlakeHash.EMPTY_HASH), null)
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
