package nodecore.miners.pop.model

import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.core.Transaction
import org.bitcoinj.core.UnsafeByteArrayOutputStream
import java.io.ByteArrayOutputStream
import java.io.IOException

class ExpTransaction(params: NetworkParameters?, payloadBytes: ByteArray?) : Transaction(
    params, payloadBytes
) {
    fun getFilteredTransaction(): ByteArray {
        val stream: ByteArrayOutputStream = UnsafeByteArrayOutputStream(if (length < 32) 32 else length + 32)
        try {
            bitcoinSerializeToStream(stream, false)
        } catch (e: IOException) {
            throw RuntimeException(e) // cannot happen
        }
        return stream.toByteArray()
    }
}
