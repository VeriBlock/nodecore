package org.veriblock.spv.lite.core

import nodecore.api.grpc.utilities.ByteStringUtility
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.veriblock.core.Context
import org.veriblock.core.params.defaultMainNetParameters
import org.veriblock.core.utilities.Utility
import org.veriblock.sdk.models.asCoin
import org.veriblock.spv.SpvConfig
import org.veriblock.spv.SpvContext
import org.veriblock.spv.model.Output
import org.veriblock.spv.model.StandardTransaction
import org.veriblock.spv.model.asStandardAddress
import java.io.File

class StandardTransactionTest {
    private lateinit var spvContext: SpvContext

    @Before
    fun setUp() {
        val dbFile = File("./data/database.db")
        if (dbFile.exists()) {
            dbFile.delete()
        }
        Context.set(defaultMainNetParameters)
        spvContext = SpvContext(
            SpvConfig(
                connectDirectlyTo = listOf("localhost")
            )
        )
    }

    @Test
    fun transactionMessageBuilder() {
        val outputs = listOf(
            Output(
                "V7GghFKRA6BKqtHD7LTdT2ao93DRNA".asStandardAddress(), 3499999999L.asCoin()
            )
        )
        val tx = StandardTransaction(
            "V8dy5tWcP7y36kxiJwxKPKUrWAJbjs", 3500000000L, outputs, 5904L, spvContext.networkParameters
        )
        val pub = byteArrayOf(1, 2, 3)
        val sign = byteArrayOf(3, 2, 1)
        tx.addSignature(sign, pub)
        val signedTransaction = tx.getSignedMessageBuilder(spvContext.networkParameters).build()
        Assert.assertEquals(5904L, signedTransaction.signatureIndex)
        Assert.assertEquals(3500000000L, signedTransaction.transaction.sourceAmount)
        Assert.assertEquals(
            "V8dy5tWcP7y36kxiJwxKPKUrWAJbjs", ByteStringUtility.byteStringToBase58(signedTransaction.transaction.sourceAddress)
        )
        Assert.assertEquals(1, signedTransaction.transaction.outputsList.size.toLong())
        Assert.assertEquals(
            "V7GghFKRA6BKqtHD7LTdT2ao93DRNA", ByteStringUtility.byteStringToBase58(signedTransaction.transaction.getOutputs(0).address)
        )
        Assert.assertEquals(3499999999L, signedTransaction.transaction.getOutputs(0).amount)
        Assert.assertEquals(pub, tx.publicKey)
        Assert.assertEquals(sign, tx.signature)
    }

    @Test
    fun serialize() {
        val outputs = listOf(
            Output(
                "V7GghFKRA6BKqtHD7LTdT2ao93DRNA".asStandardAddress(), 3499999999L.asCoin()
            )
        )
        val tx = StandardTransaction(
            "V8dy5tWcP7y36kxiJwxKPKUrWAJbjs", 3500000000L, outputs, 5904L, spvContext.networkParameters
        )
        val serialized = tx.toByteArray(spvContext.networkParameters)
        Assert.assertEquals(
            "01011667A654EE3E0C918D8652B63829D7F3BEF98524BF899604D09DC30001011667901A1E11C650509EFC46E09E81678054D8562AF02B04D09DC2FF0217100100",
            Utility.bytesToHex(serialized)
        )
    }
}
