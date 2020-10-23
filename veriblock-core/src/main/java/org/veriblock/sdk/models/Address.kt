// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.sdk.models

import org.veriblock.core.AddressConstants.*
import org.veriblock.core.SharedConstants
import org.veriblock.core.bitcoinj.Base58
import org.veriblock.core.bitcoinj.Base59
import org.veriblock.core.crypto.Sha256Hash
import org.veriblock.core.utilities.Preconditions
import org.veriblock.sdk.util.getSingleByteLengthValue
import org.veriblock.sdk.util.writeSingleByteLengthValue
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.*

/*
 * To make the addresses 'human-readable' we add 1 to the decoded value (1 in Base58 is 0,
 * but we want an address with a '1' in the m slot to represent m=1, for example).
 * this allows addresses with m and n both <= 9 to be easily recognized. Additionally,
 * an m or n value of 0 makes no sense, so this allows multisig to range from 1 to 58,
 * rather than what would have otherwise been 0 to 57.
 */
class Address(
    val address: String
) {
    val data: String
    val checksum: String
    val multisig: Boolean

    @Deprecated("See Address.multisig", ReplaceWith("multisig"))
    val isMultisig
        get() = multisig

    @Deprecated("See Address.getBytes()", ReplaceWith("getBytes()"))
    val bytes: ByteArray
        get() = getBytes()

    @Deprecated("See Address.getPoPBytes()", ReplaceWith("getPoPBytes()"))
    val poPBytes: ByteArray
        get() = getPoPBytes()

    init {
        Preconditions.notNull(address, "Address cannot be null")
        Preconditions.argument<Any>(
            address.length == ADDRESS_LENGTH && address[0] == ADDRESS_STARTING_CHAR,
            "The address $address is not a valid VBK address"
        )
        this.data = getDataPortionFromAddress(address)
        this.multisig = (address[ADDRESS_LENGTH - 1] == ADDRESS_MULTISIG_ENDING_CHAR)
        this.checksum = getChecksumPortionFromAddress(address, multisig)
        if (multisig) {
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
                Base58.isBase58String(address.substring(0, ADDRESS_LENGTH - 1)),
                "The address $address's remainder is not a base58 string"
            )
        } else {
            Preconditions.argument<Any>(
                Base58.isBase58String(address),
                "The address $address is not a base58 string"
            )
        }
        require(calculateChecksum(data, multisig) == checksum) {
            "Address checksum does not match"
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val obj = other as Address
        return address == obj.address
    }

    override fun hashCode(): Int {
        return address.hashCode()
    }

    override fun toString(): String {
        return address
    }

    fun isDerivedFromPublicKey(publicKey: ByteArray): Boolean {
        return try {
            val hash = Sha256Hash.of(publicKey)
            val data = "V" + Base58.encode(hash.bytes).substring(0, 24)
            val checksum = calculateChecksum(data, multisig)
            this.data == data && this.checksum == checksum
        } catch (e: Exception) {
            false
        }
    }

    @JvmName("getBytesJava")
    fun getBytes(): ByteArray {
        return if (multisig) Base59.decode(address) else Base58.decode(address)
    }

    @Deprecated("See Address.multisig", ReplaceWith("multisig"))
    @JvmName("isMultisigJava")
    fun isMultisig(): Boolean {
        return multisig
    }

    @JvmName("getPoPBytesJava")
    fun getPoPBytes(): ByteArray {
        val bytes = Base58.decode(address.substring(1))
        return Arrays.copyOfRange(bytes, 0, 16)
    }

    fun serialize(): ByteArray {
        ByteArrayOutputStream().use { stream ->
            serialize(stream)
            return stream.toByteArray()
        }
    }

    fun serialize(stream: OutputStream) {
        if (multisig) {
            stream.write(SharedConstants.MULTISIG_ADDRESS_ID.toInt())
        } else {
            stream.write(SharedConstants.STANDARD_ADDRESS_ID.toInt())
        }
        val bytes = getBytes()
        stream.writeSingleByteLengthValue(bytes)
    }

    companion object {
        @JvmStatic
        fun parse(buffer: ByteBuffer): Address {
            val type = buffer.get()
            require(type == SharedConstants.MULTISIG_ADDRESS_ID || type == SharedConstants.STANDARD_ADDRESS_ID) {
                "Address type ($type) is neither multisig, nor standard"
            }
            val bytes = buffer.getSingleByteLengthValue(0, ADDRESS_LENGTH)
            return if (type == SharedConstants.STANDARD_ADDRESS_ID) {
                Address(Base58.encode(bytes))
            } else {
                Address(Base59.encode(bytes))
            }
        }

        // TODO: remove hardcoded values - use AddressConstants instead
        @JvmStatic
        fun fromPublicKey(publicKey: ByteArray): Address {
            val keyHash = Sha256Hash.of(publicKey).bytes
            val data = "V" + Base58.encode(keyHash).substring(0, 24)
            val hash = Sha256Hash.of(data.toByteArray(StandardCharsets.UTF_8))
            val checksum = Base58.encode(hash.bytes).substring(0, 4 + 1)
            return Address(data + checksum)
        }

        @JvmStatic
        fun fromPoPBytes(buf: ByteBuffer): Address {
            val bytes = ByteArray(16)
            buf.get(bytes)
            return Address(ADDRESS_STARTING_CHAR + Base58.encode(bytes))
        }

        @JvmStatic
        private fun getDataPortionFromAddress(address: String): String {
            Preconditions.notNull(address, "The address cannot be null")
            Preconditions.argument<Any>(
                address.length == ADDRESS_LENGTH,
                "The address $address should be of size $ADDRESS_LENGTH"
            )

            return address.substring(0, 24 + 1)
        }

        @JvmStatic
        private fun getChecksumPortionFromAddress(address: String, multisig: Boolean): String {
            Preconditions.notNull(address, "The address cannot be null")
            Preconditions.argument<Any>(
                address.length == ADDRESS_LENGTH,
                "The address $address should be of size $ADDRESS_LENGTH"
            )

            if (multisig) {
                return address.substring(25, 28 + 1)
            }
            return address.substring(25)
        }

        @JvmStatic
        private fun calculateChecksum(data: String, multisig: Boolean): String {
            val hash = Sha256Hash.of(data.toByteArray(StandardCharsets.UTF_8))
            if (multisig) {
                val checksum = Base58.encode(hash.bytes)
                return checksum.substring(0, 3 + 1)
            }
            val checksum = Base58.encode(hash.bytes)
            return checksum.substring(0, 4 + 1)
        }
    }
}
