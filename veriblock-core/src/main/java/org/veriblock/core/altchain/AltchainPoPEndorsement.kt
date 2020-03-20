// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.core.altchain

import org.veriblock.core.TransactionConstants
import org.veriblock.core.utilities.Utility
import org.veriblock.core.utilities.extensions.toHex
import java.io.ByteArrayInputStream

/**
 * An Altchain PoP Endorsement is an endorsement of an altchain's state which an altchain PoP miner has published to
 * VeriBlock.
 *
 * These are created from specially-formatted data sections in standard transactions, and pair a network identifier
 * (each altchain adopts a different network ID) with an endorsed block. Because altchains can have a variety of
 * formats, the endorsed block data is free-form: PoW blockchains will likely publish just a standard block header, but
 * other consensus mechanisms like PoS may require additional data be published (such as that which proves that a PoS
 * block was created by the consumption of valid coinage).
 *
 * Nothing prevents PoP miners from using arbitrary identifiers, nor does anything prevent two altchains from using the
 * same identifier. The identifier is simply a convenience for easy parsing, and the endorsement data contains enough
 * information to perform preliminary validation (such as hashing the block header and seeing if it is the appropriate
 * difficulty).
 */
class AltchainPoPEndorsement(
    private val rawData: ByteArray
) {
    val identifier: Long
    private val header: ByteArray
    private val contextInfo: ByteArray
    private val payoutInfo: ByteArray

    fun getHeader() = header.clone()

    fun getContextInfo() = contextInfo.clone()

    fun getPayoutInfo() = payoutInfo.clone()

    fun getRawData() = rawData.clone()

    init {
        require(rawData.isNotEmpty()) {
            "An AltchainPoPEndorsement cannot be created with an empty data array!"
        }
        require(rawData.size <= TransactionConstants.MAX_TRANSACTION_DATA_SIZE_BYTES) {
            "An AltchainPoPEndorsement cannot be created with raw data that is longer than ${TransactionConstants.MAX_TRANSACTION_DATA_SIZE_BYTES}"
        }
        val endorsementDataStream = ByteArrayInputStream(rawData)
        val idSize = endorsementDataStream.read().toByte()
        require(idSize <= AltchainConstants.MAX_ALTCHAIN_IDENTIFIER_LENGTH) {
            "An AltchainPoPEndorsement cannot be created with an identifier that is more than 8 bytes long ($idSize)!"
        }
        require(idSize >= 1) {
            "An AltchainPoPEndorsement cannot be created with an identifier that is zero or negative ($idSize)!"
        }
        require(endorsementDataStream.available() >= idSize) {
            "An AltchainPoPEndorsement was attempted to be created which claimed to have $idSize bytes of ID data, but only had ${endorsementDataStream.available()} bytes left!"
        }
        this.identifier = endorsementDataStream.readVariableLengthInt("identifier", idSize)

        // Read the header data (size of header size, size of header, header)
        require(endorsementDataStream.available() != 0) {
            "An AltchainPoPEndorsement was attempted to be created with no header, context, or payout sections!"
        }
        this.header = endorsementDataStream.read("header", AltchainConstants.MAX_ALTCHAIN_ENDORSEMENT_INFO_SECTION_LENGTH)

        // Read the context info data (size of context info size, size of context info, context info)
        require(endorsementDataStream.available() != 0) {
            "An AltchainPoPEndorsement was attempted to be created with no context info, or payout sections!"
        }
        contextInfo = endorsementDataStream.read("context info", AltchainConstants.MAX_ALTCHAIN_ENDORSEMENT_INFO_SECTION_LENGTH, true)

        // Read the payout data (size of context info size, size of context info, context info)
        require(endorsementDataStream.available() != 0) {
            "An AltchainPoPEndorsement was attempted to be created with no or payout section!"
        }
        payoutInfo = endorsementDataStream.read("payout info", AltchainConstants.MAX_ALTCHAIN_ENDORSEMENT_INFO_SECTION_LENGTH, true)

        if (endorsementDataStream.available() > 0) {
            throw IllegalArgumentException("While creating an AltchainPoPEndorsement, unexpected additional data was present after the specified header, context info, and payout info sections!" +
                " Extra available bytes: ${Utility.bytesToHex(ByteArray(endorsementDataStream.available()).also { endorsementDataStream.read(it) })}!")
        }
    }

    private fun ByteArrayInputStream.read(name: String, maxLengthSize: Int, canBeOmitted: Boolean = false): ByteArray {
        // Read length of size
        val lengthOfSize = read().toByte()
        require(lengthOfSize <= maxLengthSize) {
            "An AltchainPoPEndorsement cannot be created with a $name size length that is more than $maxLengthSize bytes long ($lengthOfSize)!"
        }
        if (canBeOmitted) {
            require(lengthOfSize >= 0) {
                "An AltchainPoPEndorsement cannot be created with a $name size length that is negative ($lengthOfSize)!"
            }
            // If it can be omitted, a size of size equals to 0 means omission
            if (lengthOfSize.toInt() == 0) {
                return ByteArray(0)
            }
        }
        require(lengthOfSize > 0) {
            "An AltchainPoPEndorsement cannot be created with a $name size length that is zero or negative ($lengthOfSize)!"
        }
        val length = readVariableLengthInt(name, lengthOfSize).toInt()
        require(length >= 0) {
            "An AltchainPoPEndorsement was attempted to be created which contained a $name length that was negative ($length)!"
        }
        val data = ByteArray(length)
        if (data.isEmpty()) {
            return data
        }
        // Read actual data
        val readResult = read(data)
        require(readResult >= data.size) {
            "While creating an AltchainPoPEndorsement, the $name bytes were unable to be read in full from the endorsement: ${rawData.toHex()}"
        }
        return data
    }

    private fun ByteArrayInputStream.readVariableLengthInt(name: String, length: Byte): Long {
        val bytes = ByteArray(length.toInt())
        // Read actual size
        require(read(bytes) >= bytes.size) {
            "While creating an AltchainPoPEndorsement, the $name size was unable to be read in full from the endorsement: ${rawData.toHex()}"
        }
        var result = 0L
        for (i in bytes.indices) {
            result = (result shl 8) or (0xFFL and bytes[i].toLong())
        }
        return result
    }

    companion object {
        @JvmStatic
        fun isValidEndorsement(rawData: ByteArray): Boolean {
            return try {
                AltchainPoPEndorsement(rawData)
                true
            } catch (e: Exception) {
                false
            }
        }
    }
}

inline fun ByteArray.checkForValidEndorsement(errorHandler: (Exception) -> Unit) {
    try {
        AltchainPoPEndorsement(this)
    } catch (e: Exception) {
        errorHandler(e)
    }
}
