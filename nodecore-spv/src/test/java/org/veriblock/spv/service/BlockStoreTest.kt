package org.veriblock.spv.service

import io.kotest.matchers.shouldBe
import org.apache.commons.lang3.RandomStringUtils
import org.junit.*
import org.veriblock.core.Context
import org.veriblock.core.crypto.PreviousBlockVbkHash
import org.veriblock.core.crypto.PreviousKeystoneVbkHash
import org.veriblock.core.crypto.Sha256Hash
import org.veriblock.core.crypto.VbkHash
import org.veriblock.core.crypto.MerkleRoot
import org.veriblock.core.crypto.TruncatedMerkleRoot
import org.veriblock.core.crypto.asVbkHash
import org.veriblock.core.crypto.asVbkPreviousBlockHash
import org.veriblock.core.crypto.asVbkPreviousKeystoneHash
import org.veriblock.core.crypto.asBtcHash
import org.veriblock.core.miner.randomTruncatedMerkleRoot
import org.veriblock.core.params.defaultRegTestParameters
import org.veriblock.sdk.models.VeriBlockBlock
import org.veriblock.spv.model.StoredVeriBlockBlock
import java.io.File
import java.math.BigInteger
import java.security.MessageDigest
import kotlin.random.Random

class BlockStoreTest {
    private val regtest = defaultRegTestParameters

    init {
        Context.create(regtest)
    }

    private val baseDir: File = createTempDir()
    private var blockStore = BlockStore(regtest, baseDir)


    @After
    fun after() {
        baseDir.deleteRecursively()
    }

    @Test
    fun `should store and restore the given list of blocks`() {
        // Given
        (1..100).map {
            StoredVeriBlockBlock(
                randomVeriBlockBlock(height = it),
                BigInteger.ONE,
                randomVbkHash()
            )
        }
            // When
            .map {
                blockStore.appendBlock(it)
            }
        // Then
        var count = 0
        blockStore.forEach { _, _ ->
            count++
            true
        }

        count shouldBe 100
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
        blockStore.writeBlock(0, storedVeriBlockBlock)
        // Then
        blockStore.readBlock(0) shouldBe storedVeriBlockBlock
    }

    @Test
    fun `should restore the blocks which were stored by a previous block store object`() {
        // Given
        val list = (1..100).map {
            StoredVeriBlockBlock(
                randomVeriBlockBlock(height = it),
                BigInteger.ONE,
                randomVbkHash()
            )
        }

        val pos = list.map {
            blockStore.appendBlock(it)
        }
        // When
        blockStore = BlockStore(regtest, baseDir)
        // Then
        list.zip(pos).forEach { (block, position) ->
            blockStore.readBlock(position) shouldBe block
        }
    }
}

private var messageDigest = MessageDigest.getInstance("SHA-256")
private fun randomByteArray(size: Int): ByteArray = ByteArray(size) { randomInt(256).toByte() }
private fun randomInt(bound: Int) = Random.nextInt(bound)
private fun randomInt(min: Int, max: Int) = Random.nextInt(max - min) + min
private fun randomLong(min: Long, max: Long) = (Random.nextDouble() * (max - min)).toLong() + min
private fun randomAlphabeticString(length: Int = 10): String = RandomStringUtils.randomAlphabetic(length)
private fun randomVbkHash(): VbkHash = randomByteArray(VbkHash.HASH_LENGTH).asVbkHash()
private fun randomPreviousBlockVbkHash(): PreviousBlockVbkHash = randomByteArray(PreviousBlockVbkHash.HASH_LENGTH).asVbkPreviousBlockHash()
private fun randomPreviousKeystoneVbkHash(): PreviousKeystoneVbkHash = randomByteArray(PreviousKeystoneVbkHash.HASH_LENGTH).asVbkPreviousKeystoneHash()
private fun randomSha256Hash(): Sha256Hash = messageDigest.digest(randomAlphabeticString().toByteArray()).asBtcHash()
private fun randomVeriBlockBlock(
    height: Int = randomInt(1, 65535),
    version: Short = randomInt(1, Short.MAX_VALUE.toInt()).toShort(),
    previousBlock: PreviousBlockVbkHash = randomPreviousBlockVbkHash(),
    previousKeystone: PreviousKeystoneVbkHash = randomPreviousKeystoneVbkHash(),
    secondPreviousKeystone: PreviousKeystoneVbkHash = randomPreviousKeystoneVbkHash(),
    merkleRoot: TruncatedMerkleRoot = randomTruncatedMerkleRoot(),
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
