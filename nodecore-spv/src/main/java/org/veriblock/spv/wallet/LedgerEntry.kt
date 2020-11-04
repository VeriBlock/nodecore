// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.spv.wallet

import org.veriblock.core.utilities.SerializerUtility
import org.veriblock.sdk.models.Coin
import org.veriblock.core.crypto.Sha256Hash
import org.veriblock.core.crypto.asBtcHash
import org.veriblock.sdk.services.SerializeDeserializeService
import org.veriblock.spv.model.asLightAddress
import java.io.ByteArrayOutputStream
import java.io.IOException

class LedgerEntry(
    val address: String,
    val txId: Sha256Hash,
    val debitAmount: Coin,
    val creditAmount: Coin,
    val signatureIndex: Long,
    val positionIndex: Int,
    var status: Status
) {
    val key: Sha256Hash = generateKey(address, txId, debitAmount, creditAmount, positionIndex)

    enum class Status(val value: Int) {
        PENDING(0),
        CONFIRMED(1),
        FINALIZED(2);

        companion object {
            fun forNumber(value: Int): Status? {
                return when (value) {
                    0 -> PENDING
                    1 -> CONFIRMED
                    2 -> FINALIZED
                    else -> null
                }
            }
        }

    }

    companion object {
        fun generateKey(
            address: String,
            txId: Sha256Hash,
            debitAmount: Coin,
            creditAmount: Coin,
            positionIndex: Int
        ): Sha256Hash {
            val a = address.asLightAddress()
            try {
                ByteArrayOutputStream().use { stream ->
                    a.serializeToStream(stream)
                    SerializerUtility.writeSingleByteLengthValueToStream(stream, txId.bytes)
                    SerializeDeserializeService.serialize(debitAmount, stream)
                    SerializeDeserializeService.serialize(creditAmount, stream)
                    SerializerUtility.writeVariableLengthValueToStream(stream, positionIndex)
                    return stream.toByteArray().asBtcHash()
                }
            } catch (e: IOException) {
                // Should not happen
            }
            return Sha256Hash.ZERO_HASH
        }
    }
}
