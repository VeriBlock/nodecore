package org.veriblock.core.contracts

import io.kotlintest.matchers.numerics.shouldBeGreaterThan
import io.kotlintest.shouldBe
import org.junit.Assert
import org.junit.Test
import org.veriblock.core.utilities.extensions.toHex
import kotlin.random.Random

class EgBlockHashTest {

    @Test
    fun `should return false when comparing two block hash with same length`() {
        // Given
        val blockHash1 = randomEgBlockHash(9)
        val blockHash2 = randomEgBlockHash(9)

        // When
        val isBetter = blockHash1.isBetterThan(blockHash2)

        // Then
        isBetter shouldBe false
        blockHash1.bytes.size shouldBe blockHash2.bytes.size
    }

    @Test
    fun `should return false when comparing a block hash with itself`() {
        // Given
        val blockHash1 = randomEgBlockHash()

        // When
        val isBetter = blockHash1.isBetterThan(blockHash1)

        // Then
        isBetter shouldBe false
    }

    @Test
    fun `should return true when comparing a block hash with another one with more length`() {
        // Given
        val hash = randomHex(9)
        val blockHash1 = hash.asEgBlockHash()
        val blockHash2 = (randomHex(9) + hash).asEgBlockHash()

        // When
        val isBetter = blockHash2.isBetterThan(blockHash1)

        // Then
        isBetter shouldBe true
        blockHash2.bytes.size shouldBeGreaterThan blockHash1.bytes.size
    }

    @Test
    fun `should return true when comparing a block hash with a null block hash`() {
        // Given
        val blockHash = randomEgBlockHash()

        // When
        val isBetter = blockHash.isBetterThan(null)

        // Then
        isBetter shouldBe true
    }

    @Test
    fun isBetterThan_WhenOtherIsFullHash() {
        val base = "EEEF26F89FEE8AE934".asEgBlockHash()
        val other = "0000000000035761E5F0C258E55832EEEF26F89FEE8AE934".asEgBlockHash()

        base.isBetterThan(other) shouldBe false
    }

    @Test
    fun equals_WhenPartial() {
        val base = "AFB99AB25A2212FE7C".asEgBlockHash()
        val other = "0000000000021AD1155C91DBEDD52AAFB99AB25A2212FE7C".asEgBlockHash()

        // Testing equals both ways
        base shouldBe other
        other shouldBe base
    }
}

fun randomHex(sizeInBytes: Int): String = Random.nextBytes(sizeInBytes).toHex()

fun randomEgBlockHash(
    lengthInBytes: Int = 9
): EgBlockHash {
    return Random.nextBytes(lengthInBytes).asEgBlockHash()
}
