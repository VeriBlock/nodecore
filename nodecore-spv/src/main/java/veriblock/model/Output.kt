// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package veriblock.model

import org.veriblock.sdk.models.Coin
import org.veriblock.sdk.services.SerializeDeserializeService
import java.io.OutputStream
import java.util.Objects

class Output(
    val address: AddressLight,
    val amount: Coin
) {
    fun serializeToStream(stream: OutputStream) {
        address.serializeToStream(stream)
        SerializeDeserializeService.serialize(amount, stream)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is Output) {
            return false
        }
        return address == other.address &&
            amount == other.amount
    }

    override fun hashCode(): Int {
        return Objects.hash(address, amount)
    }
}
