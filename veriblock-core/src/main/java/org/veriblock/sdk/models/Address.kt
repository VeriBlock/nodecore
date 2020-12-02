// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.sdk.models

import org.veriblock.core.bitcoinj.Base58
import org.veriblock.core.bitcoinj.Base59
import org.veriblock.core.crypto.sha256HashOf
import org.veriblock.core.utilities.Preconditions
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.Arrays

class Address(address: String) {
    val address: String
    val data: String
    val checksum: String
    val isMultisig: Boolean
    fun isDerivedFromPublicKey(publicKey: ByteArray): Boolean {
        return try {
            val hash = sha256HashOf(publicKey)
            val data = "V" + Base58.encode(hash).substring(0, 24)
            val checksum = calculateChecksum(data, isMultisig)
            this.data == data && this.checksum == checksum
        } catch (e: Exception) {
            false
        }
    }

    val bytes: ByteArray
        get() = if (isMultisig) Base59.decode(address) else Base58.decode(address)

    override fun toString(): String {
        return address
    }

    override fun hashCode(): Int {
        return address.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return other is Address && address == other.address
    }

    fun getPopBytes(isProgPow: Boolean): ByteArray {
        val bytes = Base58.decode(address.substring(1))
        val size = if(isProgPow) 15 else 16
        return Arrays.copyOfRange(bytes, 0, size)
    }

    companion object {
        const val SIZE = 30
        const val STARTING_CHAR = 'V'
        private const val MULTISIG_ENDING_CHAR = '0'
        private const val MULTISIG_ADDRESS_M_VALUE = 1
        private const val MULTISIG_ADDRESS_N_VALUE = 2
        private const val MULTISIG_ADDRESS_MIN_N_VALUE = 2
        private const val MULTISIG_ADDRESS_MAX_M_VALUE = 58
        private const val MULTISIG_ADDRESS_MAX_N_VALUE = 58
        fun fromPublicKey(publicKey: ByteArray?): Address {
            val keyHash = sha256HashOf(publicKey!!)
            val data = "V" + Base58.encode(keyHash).substring(0, 24)
            val hash = sha256HashOf(data.toByteArray(StandardCharsets.UTF_8))
            val checksum = Base58.encode(hash).substring(0, 4 + 1)
            return Address(data + checksum)
        }

        private fun calculateChecksum(data: String, multisig: Boolean): String {
            val hash = sha256HashOf(data.toByteArray(StandardCharsets.UTF_8))
            return if (multisig) {
                val checksum = Base58.encode(hash)
                checksum.substring(0, 3 + 1)
            } else {
                val checksum = Base58.encode(hash)
                checksum.substring(0, 4 + 1)
            }
        }

        private fun getDataPortionFromAddress(address: String): String {
            require(address.length == SIZE) {
                "The address $address should be of size $SIZE"
            }
            return address.substring(0, 24 + 1)
        }

        private fun getChecksumPortionFromAddress(address: String, multisig: Boolean): String {
            require(address.length == SIZE) {
                "The address $address should be of size $SIZE"
            }
            return if (multisig) {
                address.substring(25, 28 + 1)
            } else {
                address.substring(25)
            }
        }

        fun fromPoPBytes(buf: ByteBuffer): Address {
            val bytes = ByteArray(16)
            buf[bytes]
            return Address(STARTING_CHAR.toString() + Base58.encode(bytes))
        }
    }

    init {
        require(address.length == SIZE && address[0] == STARTING_CHAR) {
            "The address $address is not a valid VBK address"
        }
        this.address = address
        data = getDataPortionFromAddress(address)
        isMultisig = address[SIZE - 1] == MULTISIG_ENDING_CHAR
        checksum = getChecksumPortionFromAddress(address, isMultisig)
        if (isMultisig) {
            Preconditions.argument<Any>(
                Base59.isBase59String(address),
                "The address $address is not a base59 string"
            )

            /* To make the addresses 'human-readable' we add 1 to the decoded value (1 in Base58 is 0,
             * but we want an address with a '1' in the m slot to represent m=1, for example).
             * this allows addresses with m and n both <= 9 to be easily recognized. Additionally,
             * an m or n value of 0 makes no sense, so this allows multisig to range from 1 to 58,
             * rather than what would have otherwise been 0 to 57. */
            val m = Base58.decode("" + address[MULTISIG_ADDRESS_M_VALUE])[0] + 1
            val n = Base58.decode("" + address[MULTISIG_ADDRESS_N_VALUE])[0] + 1
            Preconditions.argument<Any>(
                n >= MULTISIG_ADDRESS_MIN_N_VALUE,
                "The address $address does not have enough addresses to be multisig"
            )
            Preconditions.argument<Any>(
                m <= n,
                "The address $address has more signatures than addresses"
            )
            Preconditions.argument<Any>(
                n <= MULTISIG_ADDRESS_MAX_N_VALUE && m <= MULTISIG_ADDRESS_MAX_M_VALUE,
                "The address $address has too many addresses/signatures"
            )
            Preconditions.argument<Any>(
                Base58.isBase58String(address.substring(0, SIZE - 1)),
                "The address $address's remainder is not a base58 string"
            )
        } else {
            Preconditions.argument<Any>(
                Base58.isBase58String(address),
                "The address $address is not a base58 string"
            )
        }
        require(calculateChecksum(data, isMultisig) == checksum) { "Address checksum does not match" }
    }
}
