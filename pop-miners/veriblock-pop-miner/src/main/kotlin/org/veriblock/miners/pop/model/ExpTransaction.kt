package org.veriblock.miners.pop.model

import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.core.Transaction
import org.bitcoinj.core.UnsafeByteArrayOutputStream
import java.io.ByteArrayOutputStream

class ExpTransaction(
    params: NetworkParameters?,
    payloadBytes: ByteArray?
) : Transaction(
    params, payloadBytes
) {
    fun getFilteredTransaction(): ByteArray {
        val stream: ByteArrayOutputStream = UnsafeByteArrayOutputStream(if (length < 32) 32 else length + 32)
        bitcoinSerializeToStream(stream, false)
        return stream.toByteArray()
    }
}
