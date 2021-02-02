package org.veriblock.core.utilities

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.veriblock.core.contracts.TransactionAddress
import org.veriblock.core.contracts.TransactionAmount
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

class SerializerUtilityTests {
    @Test
    fun deserializeAddress() {
        val addrValue = "V5Ujv72h4jEBcKnALGc4fKqs6CDAPX"
        val addrBytes = Utility.hexToBytes("01166772F51AB208D32771AB1506970EEB664462730B838E")

        val addr = TransactionAddress.deserialize(ByteBuffer.wrap(addrBytes))
        addrValue shouldBe addr.toString()
        addr.isMultisig shouldBe false
    }

    @Test
    fun deserializeCoin() {
        val coinValue = TransactionAmount(3500000000L)
        val coinBytesExpected: ByteArray
        ByteArrayOutputStream().use { stream ->
            coinValue.serializeToStream(stream)
            coinBytesExpected = stream.toByteArray()
        }

        val coinBytes = Utility.hexToBytes("04d09dc300")
        val amount = TransactionAmount.deserialize(ByteBuffer.wrap(coinBytes))

        coinBytes.toList() shouldContainExactly coinBytesExpected.toList()
        amount.value() shouldBe coinValue.value()
    }
}
