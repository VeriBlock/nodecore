// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.sdk.services

import org.veriblock.core.crypto.Sha256Hash
import org.veriblock.core.crypto.VbkHash
import org.veriblock.core.crypto.readVbkPreviousBlockHash
import org.veriblock.core.crypto.readVbkPreviousKeystoneHash
import org.veriblock.core.utilities.BlockUtility
import org.veriblock.core.utilities.Utility
import org.veriblock.sdk.models.Address
import org.veriblock.sdk.models.AltPublication
import org.veriblock.sdk.models.BitcoinBlock
import org.veriblock.sdk.models.BitcoinTransaction
import org.veriblock.sdk.models.BlockType
import org.veriblock.sdk.models.Coin
import org.veriblock.sdk.models.Constants
import org.veriblock.sdk.models.MerklePath
import org.veriblock.sdk.models.Output
import org.veriblock.sdk.models.PublicationData
import org.veriblock.sdk.models.VeriBlockBlock
import org.veriblock.sdk.models.VeriBlockMerklePath
import org.veriblock.sdk.models.VeriBlockPopTransaction
import org.veriblock.sdk.models.VeriBlockPublication
import org.veriblock.sdk.models.VeriBlockTransaction
import org.veriblock.sdk.models.parseAddress
import org.veriblock.sdk.models.parseCoin
import org.veriblock.sdk.util.getSingleByteLengthValue
import org.veriblock.sdk.util.getVariableLengthValue
import org.veriblock.sdk.util.putBEBytes
import org.veriblock.sdk.util.putBEInt16
import org.veriblock.sdk.util.putBEInt32
import org.veriblock.sdk.util.readBEInt16
import org.veriblock.sdk.util.readBEInt32
import org.veriblock.sdk.util.readLEInt32
import org.veriblock.sdk.util.writeSingleByteLengthValue
import org.veriblock.sdk.util.writeSingleIntLengthValue
import org.veriblock.sdk.util.writeVariableLengthValue
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

private const val SERIALIZATION_VERSION = 0x01
private val SERIALIZATION_VERSION_BYTES = Utility.toByteArray(SERIALIZATION_VERSION)

object SerializeDeserializeService {

    fun serialize(veriBlockPoPTransaction: VeriBlockPopTransaction, stream: OutputStream) {
        val rawTransaction = serializeTransactionEffects(veriBlockPoPTransaction)
        stream.writeVariableLengthValue(rawTransaction)
        stream.writeSingleByteLengthValue(veriBlockPoPTransaction.signature)
        stream.writeSingleByteLengthValue(veriBlockPoPTransaction.publicKey)
    }

    fun serializeTransactionEffects(veriBlockPoPTransaction: VeriBlockPopTransaction): ByteArray {
        try {
            ByteArrayOutputStream().use { stream ->
                serializeTransactionEffects(veriBlockPoPTransaction, stream)
                return stream.toByteArray()
            }
        } catch (ignore: IOException) {
            // Should not happen
        }
        return byteArrayOf()
    }

    private fun serializeTransactionEffects(tx: VeriBlockPopTransaction, stream: OutputStream) {
        if (tx.networkByte != null) {
            // Replay protection versus mainnet network
            stream.write(tx.networkByte.toInt())
        }

        // Write type
        stream.write(BlockType.VERIBLOCK_POP_TX.id.toInt())
        stream.serialize(tx.address)

        // Write size (in bytes) of endorsed VeriBlock block header (will always be 64 bytes)
        stream.serialize(tx.publishedBlock)

        // Write the Bitcoin transaction
        serialize(tx.bitcoinTransaction, stream)

        // write Merkle path
        stream.serialize(tx.merklePath)

        // Write Bitcoin block header of proof
        serialize(tx.blockOfProof, stream)

        // Write number of context Bitcoin block headers (can be 0)
        stream.writeSingleByteLengthValue(tx.blockOfProofContext.size)
        for (block in tx.blockOfProofContext) {
            serialize(block, stream)
        }
    }

    fun getId(veriBlockPoPTransaction: VeriBlockPopTransaction): Sha256Hash {
        return Sha256Hash.of(serializeTransactionEffects(veriBlockPoPTransaction))
    }

    fun getHash(veriBlockPoPTransaction: VeriBlockPopTransaction): Sha256Hash {
        return Sha256Hash.of(serializeTransactionEffects(veriBlockPoPTransaction))
    }

    // VeriBlockPublication
    fun serialize(veriBlockPublication: VeriBlockPublication): ByteArray {
        try {
            ByteArrayOutputStream().use { stream ->
                serialize(veriBlockPublication, stream)
                return stream.toByteArray()
            }
        } catch (ignore: IOException) {
            // Should not happen
        }
        return byteArrayOf()
    }

    fun serialize(veriBlockPublication: VeriBlockPublication, stream: OutputStream) {
        stream.write(SERIALIZATION_VERSION_BYTES)
        serialize(veriBlockPublication.transaction, stream)
        stream.serialize(veriBlockPublication.merklePath)
        stream.serialize(veriBlockPublication.containingBlock)

        // Write number of context Bitcoin block headers (can be 0)
        stream.writeSingleByteLengthValue(veriBlockPublication.context.size)
        for (block in veriBlockPublication.context) {
            stream.serialize(block)
        }
    }

    // VeriBlockTransaction
    fun serialize(veriBlockTransaction: VeriBlockTransaction, stream: OutputStream) {
        val rawTransaction = serializeTransactionEffects(veriBlockTransaction)
        stream.writeVariableLengthValue(rawTransaction)
        stream.writeSingleByteLengthValue(veriBlockTransaction.signature)
        stream.writeSingleByteLengthValue(veriBlockTransaction.publicKey)
    }

    fun serializeTransactionEffects(veriBlockTransaction: VeriBlockTransaction): ByteArray {
        try {
            ByteArrayOutputStream().use { stream ->
                serializeTransactionEffects(veriBlockTransaction, stream)
                return stream.toByteArray()
            }
        } catch (ignore: IOException) {
            // Should not happen
        }
        return byteArrayOf()
    }

    fun serializeTransactionEffects(veriBlockTransaction: VeriBlockTransaction, stream: OutputStream) {
        if (veriBlockTransaction.networkByte != null) {
            stream.write(veriBlockTransaction.networkByte.toInt())
        }
        stream.write(veriBlockTransaction.type.toInt())
        stream.serialize(veriBlockTransaction.sourceAddress)
        stream.serialize(veriBlockTransaction.sourceAmount)
        stream.write(veriBlockTransaction.outputs.size.toByte().toInt())
        for (o in veriBlockTransaction.outputs) {
            stream.serialize(o)
        }
        val publicationDataBytes = veriBlockTransaction.publicationData?.let { serialize(it) } ?: ByteArray(0)
        stream.writeSingleByteLengthValue(veriBlockTransaction.signatureIndex)
        stream.writeVariableLengthValue(publicationDataBytes)
    }

    fun getId(veriBlockTransaction: VeriBlockTransaction): Sha256Hash {
        return Sha256Hash.of(serializeTransactionEffects(veriBlockTransaction))
    }

    // BitcoinBlock
    fun serialize(bitcoinBlock: BitcoinBlock): ByteArray {
        try {
            ByteArrayOutputStream().use { stream ->
                serialize(bitcoinBlock, stream)
                return stream.toByteArray()
            }
        } catch (ignore: IOException) {
            // Should not happen
        }
        return byteArrayOf()
    }

    fun serialize(bitcoinBlock: BitcoinBlock, stream: OutputStream) {
        stream.writeSingleByteLengthValue(
            bitcoinBlock.raw
        )
    }

    @JvmStatic
    fun parseBitcoinBlockWithLength(buffer: ByteBuffer): BitcoinBlock {
        val raw = buffer.getSingleByteLengthValue(
            Constants.HEADER_SIZE_BitcoinBlock, Constants.HEADER_SIZE_BitcoinBlock
        )
        return parseBitcoinBlock(raw)
    }

    fun parseBitcoinBlock(bytes: ByteArray): BitcoinBlock {
        require(bytes.size == Constants.HEADER_SIZE_BitcoinBlock) {
            "Invalid raw Bitcoin Block: " + Utility.bytesToHex(bytes)
        }
        val buffer = ByteBuffer.allocateDirect(bytes.size)
        buffer.put(bytes)
        buffer.flip()
        val version = buffer.readLEInt32()
        val previousBlock = Sha256Hash.extract(buffer)
        val merkleRoot = Sha256Hash.extract(buffer)
        val timestamp = buffer.readLEInt32()
        val bits = buffer.readLEInt32()
        val nonce = buffer.readLEInt32()
        return BitcoinBlock(version, previousBlock, merkleRoot, timestamp, bits, nonce)
    }

    // AltPublication
    fun serialize(altPublication: AltPublication): ByteArray {
        try {
            ByteArrayOutputStream().use { stream ->
                serialize(altPublication, stream)
                return stream.toByteArray()
            }
        } catch (ignore: IOException) {
            // Should not happen
        }
        return byteArrayOf()
    }

    fun serialize(altPublication: AltPublication, stream: ByteArrayOutputStream) {
        stream.write(SERIALIZATION_VERSION_BYTES)
        serialize(altPublication.transaction, stream)
        stream.serialize(altPublication.merklePath)
        stream.serialize(altPublication.containingBlock)

        // Write number of context Bitcoin block headers (can be 0)
        stream.writeSingleByteLengthValue(altPublication.context.size)
        for (block in altPublication.context) {
            stream.serialize(block)
        }
    }

    // PublicationData
    fun serialize(publicationData: PublicationData): ByteArray {
        try {
            ByteArrayOutputStream().use { stream ->
                serialize(publicationData, stream)
                return stream.toByteArray()
            }
        } catch (ignore: IOException) {
            // Should not happen
        }
        return byteArrayOf()
    }

    fun serialize(publicationData: PublicationData, stream: OutputStream) {
        stream.writeSingleByteLengthValue(publicationData.identifier)
        stream.writeVariableLengthValue(publicationData.header)
        stream.writeVariableLengthValue(publicationData.contextInfo)
        stream.writeVariableLengthValue(publicationData.payoutInfo)
    }

    @JvmStatic
    fun parsePublicationData(data: ByteArray): PublicationData? {
        if (data.isEmpty()) {
            return null
        }
        val buffer = ByteBuffer.wrap(data)
        val identifierBytes = buffer.getSingleByteLengthValue(0, 8)
        val identifier = Utility.toLong(identifierBytes)
        val headerBytes = buffer.getVariableLengthValue(
            0, Constants.MAX_HEADER_SIZE_PUBLICATION_DATA
        )
        val contextInfoBytes = buffer.getVariableLengthValue(
            0, Constants.MAX_PAYOUT_SIZE_PUBLICATION_DATA
        )
        val payoutInfoBytes = buffer.getVariableLengthValue(
            0, Constants.MAX_CONTEXT_SIZE_PUBLICATION_DATA
        )
        return PublicationData(identifier, headerBytes, payoutInfoBytes, contextInfoBytes)
    }

    // BitcoinTransaction
    fun serialize(bitcoinTransaction: BitcoinTransaction, stream: OutputStream) {
        stream.writeVariableLengthValue(bitcoinTransaction.rawBytes)
    }
}

// VeriBlockBlock
fun ByteBuffer.parseVeriBlockBlock(precomputedHash: VbkHash? = null): VeriBlockBlock {
    val raw = getSingleByteLengthValue(
        Constants.HEADER_SIZE_VeriBlockBlock_VBlake, Constants.HEADER_SIZE_VeriBlockBlock
    )
    return raw.parseVeriBlockBlock(precomputedHash)
}

fun ByteArray.parseVeriBlockBlock(precomputedHash: VbkHash? = null): VeriBlockBlock {
    require(size == Constants.HEADER_SIZE_VeriBlockBlock || size == Constants.HEADER_SIZE_VeriBlockBlock_VBlake) {
        "Invalid VeriBlock raw data: " + Utility.bytesToHex(this)
    }
    val buffer = ByteBuffer.allocateDirect(size)
    buffer.put(this)
    buffer.flip()
    return buffer.parseVeriBlockBlockStream(precomputedHash)
}

fun ByteBuffer.parseVeriBlockBlockStream(precomputedHash: VbkHash? = null): VeriBlockBlock {
    require(remaining() >= Constants.HEADER_SIZE_VeriBlockBlock_VBlake) {
        "Invalid VeriBlock raw data"
    }
    val height = readBEInt32()
    val version = readBEInt16()
    val previousBlock = readVbkPreviousBlockHash()
    val previousKeystone = readVbkPreviousKeystoneHash()
    val secondPreviousKeystone = readVbkPreviousKeystoneHash()
    val merkleRoot = Sha256Hash.extract(
        this, Sha256Hash.VERIBLOCK_MERKLE_ROOT_LENGTH, ByteOrder.BIG_ENDIAN
    )
    val timestamp = readBEInt32()
    val difficulty = readBEInt32()
    var nonce = readBEInt32().toLong()
    if (BlockUtility.isProgPow(height)) {
        val nonceExtraByte = get()
        nonce = (nonce shl 8) or (nonceExtraByte.toLong() and 0xFF)
    }
    return VeriBlockBlock(
        height, version, previousBlock, previousKeystone, secondPreviousKeystone, merkleRoot,
        timestamp, difficulty, nonce, precomputedHash
    )
}

fun OutputStream.serialize(veriBlockBlock: VeriBlockBlock) {
    writeSingleByteLengthValue(veriBlockBlock.serializeHeaders())
}

fun VeriBlockBlock.serialize(): ByteArray {
    try {
        ByteArrayOutputStream().use { stream ->
            stream.serialize(this)
            return stream.toByteArray()
        }
    } catch (ignore: IOException) {
        // Should not happen
    }
    return byteArrayOf()
}

fun VeriBlockBlock.serializeHeaders(): ByteArray {
    // Hardcode 0 length hash size for network param serialization
    val headerSize = if (height == 0) {
        Constants.HEADER_SIZE_VeriBlockBlock_VBlake
    } else {
        BlockUtility.getBlockHeaderLength(height)
    }
    val buffer = ByteBuffer.allocateDirect(headerSize)
    buffer.putBEInt32(height)
    buffer.putBEInt16(version)
    buffer.putBEBytes(previousBlock.bytes)
    buffer.putBEBytes(previousKeystone.bytes)
    buffer.putBEBytes(secondPreviousKeystone.bytes)
    buffer.putBEBytes(merkleRoot.bytes)
    buffer.putBEInt32(timestamp)
    buffer.putBEInt32(difficulty)
    if (height > 0 && BlockUtility.isProgPow(height)) {
        buffer.put((nonce shr 32).toByte())
    }
    buffer.putBEInt32(nonce.toInt())
    buffer.flip()
    val bytes = ByteArray(headerSize)
    buffer.get(bytes, 0, headerSize)
    return bytes
}

//MerklePath
fun OutputStream.serialize(merklePath: MerklePath) {
    writeVariableLengthValue(merklePath.serializeComponents())
}

fun OutputStream.serializeComponentsToStream(merklePath: MerklePath) {
    // Index
    writeSingleIntLengthValue(merklePath.index)

    // Layer size
    writeSingleIntLengthValue(merklePath.layers.size)
    val sizeBottomData = Utility.toByteArray(merklePath.subject.length)

    // Write size of the int describing the size of the bottom layer of data
    writeSingleIntLengthValue(sizeBottomData.size)
    write(sizeBottomData)
    for (hash in merklePath.layers) {
        val layer = hash.bytes
        writeSingleByteLengthValue(layer)
    }
}

private fun MerklePath.serializeComponents(): ByteArray {
    try {
        ByteArrayOutputStream().use { stream ->
            stream.serializeComponentsToStream(this)
            return stream.toByteArray()
        }
    } catch (ignore: IOException) {
        // Should not happen
    }
    return byteArrayOf()
}

// VeriBlockMerklePath
fun OutputStream.serialize(blockMerklePath: VeriBlockMerklePath) {
    // Tree index
    writeSingleIntLengthValue(blockMerklePath.treeIndex)

    // Index
    writeSingleIntLengthValue(blockMerklePath.index)

    // Subject
    val subjectBytes = blockMerklePath.subject.bytes
    writeSingleByteLengthValue(subjectBytes)

    // Layer size
    writeSingleIntLengthValue(blockMerklePath.layers.size)

    // Layers
    for (hash in blockMerklePath.layers) {
        val layer = hash.bytes
        writeSingleByteLengthValue(layer)
    }
}

// Coin
fun Coin.serialize(): ByteArray {
    try {
        ByteArrayOutputStream().use { stream ->
            stream.serialize(this)
            return stream.toByteArray()
        }
    } catch (ignore: IOException) {
        // Should not happen
    }
    return byteArrayOf()
}

fun OutputStream.serialize(coin: Coin) {
    writeSingleByteLengthValue(coin.atomicUnits)
}

// Output
fun OutputStream.serialize(output: Output) {
    this.serialize(output.address)
    serialize(output.amount)
}

fun Output.serialize(): ByteArray {
    ByteArrayOutputStream().use { stream ->
        stream.serialize(this)
        return stream.toByteArray()
    }
}

fun ByteBuffer.parseOutput(): Output {
    val address = parseAddress()
    val amount = parseCoin()
    return Output(address, amount)
}

// Address
fun Address.serialize(): ByteArray {
    try {
        ByteArrayOutputStream().use { stream ->
            stream.serialize(this)
            return stream.toByteArray()
        }
    } catch (ignore: IOException) {
        // Should not happen
    }
    return byteArrayOf()
}

fun OutputStream.serialize(address: Address) {
    val bytes = address.getBytes()
    if (address.isMultisig) {
        write(3)
    } else {
        write(1)
    }
    writeSingleByteLengthValue(bytes)
}
