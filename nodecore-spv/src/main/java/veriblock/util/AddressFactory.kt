package veriblock.util

import org.veriblock.core.bitcoinj.Base58
import org.veriblock.sdk.models.Address

object AddressFactory {
    fun build(bytes: ByteArray?): Address {
        return Address(Base58.encode(bytes))
    }
}
