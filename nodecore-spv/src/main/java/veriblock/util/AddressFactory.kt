package veriblock.util

import org.veriblock.sdk.models.Address
import org.veriblock.sdk.util.Base58

object AddressFactory {
    fun build(bytes: ByteArray?): Address {
        return Address(Base58.encode(bytes))
    }
}
