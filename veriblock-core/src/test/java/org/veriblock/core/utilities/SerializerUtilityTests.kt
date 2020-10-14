package org.veriblock.core.utilities

import org.junit.Assert
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
        Assert.assertEquals(addr.toString(), addrValue)
        Assert.assertFalse(addr.isMultisig)
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

        Assert.assertArrayEquals(coinBytesExpected, coinBytes)
        Assert.assertEquals(coinValue.value(), amount.value())
    }
}
