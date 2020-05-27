// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.sdk.services

import org.veriblock.core.crypto.Sha256Hash
import org.veriblock.core.crypto.VBlakeHash
import org.veriblock.core.utilities.Preconditions
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
import org.veriblock.sdk.util.BytesUtility
import org.veriblock.sdk.util.StreamUtils
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object SerializeDeserializeService {
    @Throws(IOException::class)
    fun serialize(veriBlockPoPTransaction: VeriBlockPopTransaction, stream: OutputStream) {
        val rawTransaction = serializeTransactionEffects(veriBlockPoPTransaction)
        StreamUtils.writeVariableLengthValueToStream(stream, rawTransaction)
        StreamUtils.writeSingleByteLengthValueToStream(stream, veriBlockPoPTransaction.signature)
        StreamUtils.writeSingleByteLengthValueToStream(stream, veriBlockPoPTransaction.publicKey)
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

    @Throws(IOException::class)
    private fun serializeTransactionEffects(tx: VeriBlockPopTransaction, stream: OutputStream) {
        if (tx.networkByte != null) {
            // Replay protection versus mainnet network
            stream.write(tx.networkByte.toInt())
        }

        // Write type
        stream.write(BlockType.VERIBLOCK_POP_TX.id.toInt())
        serialize(tx.address, stream)

        // Write size (in bytes) of endorsed VeriBlock block header (will always be 64 bytes)
        serialize(tx.publishedBlock, stream)

        // Write the Bitcoin transaction
        serialize(tx.bitcoinTransaction, stream)

        // write Merkle path
        serialize(tx.merklePath, stream)

        // Write Bitcoin block header of proof
        serialize(tx.blockOfProof, stream)

        // Write number of context Bitcoin block headers (can be 0)
        StreamUtils.writeSingleByteLengthValueToStream(stream, tx.blockOfProofContext.size)
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

    @Throws(IOException::class)
    fun serialize(veriBlockPublication: VeriBlockPublication, stream: OutputStream) {
        serialize(veriBlockPublication.transaction, stream)
        serialize(veriBlockPublication.merklePath, stream)
        serialize(veriBlockPublication.containingBlock, stream)

        // Write number of context Bitcoin block headers (can be 0)
        StreamUtils.writeSingleByteLengthValueToStream(stream, veriBlockPublication.context.size)
        for (block in veriBlockPublication.context) {
            serialize(block, stream)
        }
    }

    // VeriBlockTransaction
    @Throws(IOException::class)
    fun serialize(veriBlockTransaction: VeriBlockTransaction, stream: OutputStream) {
        val rawTransaction = serializeTransactionEffects(veriBlockTransaction)
        StreamUtils.writeVariableLengthValueToStream(stream, rawTransaction)
        StreamUtils.writeSingleByteLengthValueToStream(stream, veriBlockTransaction.signature)
        StreamUtils.writeSingleByteLengthValueToStream(stream, veriBlockTransaction.publicKey)
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

    @Throws(IOException::class)
    fun serializeTransactionEffects(veriBlockTransaction: VeriBlockTransaction, stream: OutputStream) {
        if (veriBlockTransaction.networkByte != null) {
            stream.write(veriBlockTransaction.networkByte.toInt())
        }
        stream.write(veriBlockTransaction.type.toInt())
        serialize(veriBlockTransaction.sourceAddress, stream)
        serialize(veriBlockTransaction.sourceAmount, stream)
        stream.write(veriBlockTransaction.outputs.size.toByte().toInt())
        for (o in veriBlockTransaction.outputs) {
            serialize(o, stream)
        }
        val publicationDataBytes = serialize(veriBlockTransaction.publicationData)
        StreamUtils.writeSingleByteLengthValueToStream(stream, veriBlockTransaction.signatureIndex)
        StreamUtils.writeVariableLengthValueToStream(stream, publicationDataBytes)
    }

    fun getId(veriBlockTransaction: VeriBlockTransaction): Sha256Hash {
        return Sha256Hash.of(serializeTransactionEffects(veriBlockTransaction))
    }

    // VeriBlockBlock
    fun parseVeriBlockBlock(buffer: ByteBuffer): VeriBlockBlock {
        val raw = StreamUtils.getSingleByteLengthValue(
            buffer, Constants.HEADER_SIZE_VeriBlockBlock, Constants.HEADER_SIZE_VeriBlockBlock
        )
        return parseVeriBlockBlock(raw)
    }

    fun parseVeriBlockBlock(raw: ByteArray): VeriBlockBlock {
        Preconditions.notNull(raw, "VeriBlock raw data cannot be null")
        Preconditions.argument<Any>(
            raw.size == Constants.HEADER_SIZE_VeriBlockBlock
        ) { "Invalid VeriBlock raw data: " + Utility.bytesToHex(raw) }
        val buffer = ByteBuffer.allocateDirect(raw.size)
        buffer.put(raw)
        buffer.flip()
        val height = BytesUtility.readBEInt32(buffer)
        val version = BytesUtility.readBEInt16(buffer)
        val previousBlock = VBlakeHash.extract(buffer, VBlakeHash.PREVIOUS_BLOCK_LENGTH)
        val previousKeystone = VBlakeHash.extract(buffer, VBlakeHash.PREVIOUS_KEYSTONE_LENGTH)
        val secondPreviousKeystone = VBlakeHash.extract(buffer, VBlakeHash.PREVIOUS_KEYSTONE_LENGTH)
        val merkleRoot = Sha256Hash.extract(
            buffer, Sha256Hash.VERIBLOCK_MERKLE_ROOT_LENGTH, ByteOrder.BIG_ENDIAN
        )
        val timestamp = BytesUtility.readBEInt32(buffer)
        val difficulty = BytesUtility.readBEInt32(buffer)
        val nonce = BytesUtility.readBEInt32(buffer)
        return VeriBlockBlock(
            height, version, previousBlock, previousKeystone, secondPreviousKeystone, merkleRoot,
            timestamp, difficulty, nonce
        )
    }

    @Throws(IOException::class)
    fun serialize(veriBlockBlock: VeriBlockBlock, stream: OutputStream?) {
        StreamUtils.writeSingleByteLengthValueToStream(stream, serializeHeaders(veriBlockBlock))
    }

    fun serialize(veriBlockBlock: VeriBlockBlock): ByteArray {
        try {
            ByteArrayOutputStream().use { stream ->
                serialize(veriBlockBlock, stream)
                return stream.toByteArray()
            }
        } catch (ignore: IOException) {
            // Should not happen
        }
        return byteArrayOf()
    }

    fun serializeHeaders(veriBlockBlock: VeriBlockBlock): ByteArray {
        val buffer = ByteBuffer.allocateDirect(Constants.HEADER_SIZE_VeriBlockBlock)
        BytesUtility.putBEInt32(buffer, veriBlockBlock.height)
        BytesUtility.putBEInt16(buffer, veriBlockBlock.version)
        BytesUtility.putBEBytes(buffer, veriBlockBlock.previousBlock.bytes)
        BytesUtility.putBEBytes(buffer, veriBlockBlock.previousKeystone.bytes)
        BytesUtility.putBEBytes(buffer, veriBlockBlock.secondPreviousKeystone.bytes)
        BytesUtility.putBEBytes(buffer, veriBlockBlock.merkleRoot.bytes)
        BytesUtility.putBEInt32(buffer, veriBlockBlock.timestamp)
        BytesUtility.putBEInt32(buffer, veriBlockBlock.difficulty)
        BytesUtility.putBEInt32(buffer, veriBlockBlock.nonce)
        buffer.flip()
        val bytes = ByteArray(Constants.HEADER_SIZE_VeriBlockBlock)
        buffer[bytes, 0, Constants.HEADER_SIZE_VeriBlockBlock]
        return bytes
    }

    //MerklePath
    @Throws(IOException::class)
    fun serialize(merklePath: MerklePath, stream: OutputStream?) {
        StreamUtils.writeVariableLengthValueToStream(stream, serializeComponents(merklePath))
    }

    private fun serializeComponents(merklePath: MerklePath): ByteArray {
        try {
            ByteArrayOutputStream().use { stream ->
                serializeComponentsToStream(merklePath, stream)
                return stream.toByteArray()
            }
        } catch (ignore: IOException) {
            // Should not happen
        }
        return byteArrayOf()
    }

    @Throws(IOException::class)
    fun serializeComponentsToStream(merklePath: MerklePath, stream: OutputStream) {
        // Index
        StreamUtils.writeSingleIntLengthValueToStream(stream, merklePath.index)

        // Layer size
        StreamUtils.writeSingleIntLengthValueToStream(stream, merklePath.layers.size)
        val sizeBottomData = Utility.toByteArray(merklePath.subject.length)

        // Write size of the int describing the size of the bottom layer of data
        StreamUtils.writeSingleIntLengthValueToStream(stream, sizeBottomData.size)
        stream.write(sizeBottomData)
        for (hash in merklePath.layers) {
            val layer = hash.bytes
            StreamUtils.writeSingleByteLengthValueToStream(stream, layer)
        }
    }

    // VeriBlockMerklePath
    @Throws(IOException::class)
    fun serialize(blockMerklePath: VeriBlockMerklePath, stream: OutputStream) {
        // Tree index
        StreamUtils.writeSingleIntLengthValueToStream(stream, blockMerklePath.treeIndex)

        // Index
        StreamUtils.writeSingleIntLengthValueToStream(stream, blockMerklePath.index)

        // Subject
        val subjectBytes = blockMerklePath.subject.bytes
        StreamUtils.writeSingleByteLengthValueToStream(stream, subjectBytes)

        // Layer size
        StreamUtils.writeSingleIntLengthValueToStream(stream, blockMerklePath.layers.size)

        // Layers
        for (hash in blockMerklePath.layers) {
            val layer = hash.bytes
            StreamUtils.writeSingleByteLengthValueToStream(stream, layer)
        }
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

    @Throws(IOException::class)
    fun serialize(bitcoinBlock: BitcoinBlock, stream: OutputStream) {
        StreamUtils.writeSingleByteLengthValueToStream(
            stream, bitcoinBlock.raw
        )
    }

    @JvmStatic
    fun parseBitcoinBlockWithLength(buffer: ByteBuffer?): BitcoinBlock {
        val raw = StreamUtils.getSingleByteLengthValue(
            buffer, Constants.HEADER_SIZE_BitcoinBlock, Constants.HEADER_SIZE_BitcoinBlock
        )
        return parseBitcoinBlock(raw)
    }

    fun parseBitcoinBlock(bytes: ByteArray): BitcoinBlock {
        Preconditions.notNull(bytes, "Raw Bitcoin Block cannot be null")
        Preconditions.argument<Any>(
            bytes.size == Constants.HEADER_SIZE_BitcoinBlock,
            "Invalid raw Bitcoin Block: " + Utility.bytesToHex(bytes)
        )
        val buffer = ByteBuffer.allocateDirect(bytes.size)
        buffer.put(bytes)
        buffer.flip()
        val version = BytesUtility.readLEInt32(buffer)
        val previousBlock = Sha256Hash.extract(buffer)
        val merkleRoot = Sha256Hash.extract(buffer)
        val timestamp = BytesUtility.readLEInt32(buffer)
        val bits = BytesUtility.readLEInt32(buffer)
        val nonce = BytesUtility.readLEInt32(buffer)
        return BitcoinBlock(version, previousBlock, merkleRoot, timestamp, bits, nonce)
    }

    // Address
    fun serialize(address: Address): ByteArray {
        try {
            ByteArrayOutputStream().use { stream ->
                serialize(address, stream)
                return stream.toByteArray()
            }
        } catch (ignore: IOException) {
            // Should not happen
        }
        return byteArrayOf()
    }

    @Throws(IOException::class)
    fun serialize(address: Address, stream: OutputStream) {
        val bytes = address.bytes
        if (address.isMultisig) {
            stream.write(3)
        } else {
            stream.write(1)
        }
        StreamUtils.writeSingleByteLengthValueToStream(stream, bytes)
    }

    // Coin
    fun serialize(coin: Coin): ByteArray {
        try {
            ByteArrayOutputStream().use { stream ->
                serialize(coin, stream)
                return stream.toByteArray()
            }
        } catch (ignore: IOException) {
            // Should not happen
        }
        return byteArrayOf()
    }

    @Throws(IOException::class)
    fun serialize(coin: Coin, stream: OutputStream) {
        StreamUtils.writeSingleByteLengthValueToStream(stream, coin.atomicUnits)
    }

    // Output
    @Throws(IOException::class)
    fun serialize(output: Output, stream: OutputStream) {
        serialize(output.address, stream)
        serialize(output.amount, stream)
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

    @Throws(IOException::class)
    fun serialize(altPublication: AltPublication, stream: ByteArrayOutputStream) {
        serialize(altPublication.transaction, stream)
        serialize(altPublication.merklePath, stream)
        serialize(altPublication.containingBlock, stream)

        // Write number of context Bitcoin block headers (can be 0)
        StreamUtils.writeSingleByteLengthValueToStream(stream, altPublication.context.size)
        for (block in altPublication.context) {
            serialize(block, stream)
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

    @Throws(IOException::class)
    fun serialize(publicationData: PublicationData, stream: OutputStream) {
        StreamUtils.writeSingleByteLengthValueToStream(stream, publicationData.identifier)
        StreamUtils.writeVariableLengthValueToStream(stream, publicationData.header)
        StreamUtils.writeVariableLengthValueToStream(stream, publicationData.contextInfo)
        StreamUtils.writeVariableLengthValueToStream(stream, publicationData.payoutInfo)
    }

    @JvmStatic
    fun parsePublicationData(data: ByteArray): PublicationData {
        require (data.isNotEmpty()) {
            "Data cannot be empty"
        }
        val buffer = ByteBuffer.wrap(data)
        val identifierBytes = StreamUtils.getSingleByteLengthValue(buffer, 0, 8)
        val identifier = Utility.toLong(identifierBytes)
        val headerBytes = StreamUtils.getVariableLengthValue(
            buffer, 0, Constants.MAX_HEADER_SIZE_PUBLICATION_DATA
        )
        val contextInfoBytes = StreamUtils.getVariableLengthValue(
            buffer, 0, Constants.MAX_PAYOUT_SIZE_PUBLICATION_DATA
        )
        val payoutInfoBytes = StreamUtils.getVariableLengthValue(
            buffer, 0, Constants.MAX_CONTEXT_SIZE_PUBLICATION_DATA
        )
        return PublicationData(identifier, headerBytes, payoutInfoBytes, contextInfoBytes)
    }

    // BitcoinTransaction
    @Throws(IOException::class)
    fun serialize(bitcoinTransaction: BitcoinTransaction, stream: OutputStream) {
        StreamUtils.writeVariableLengthValueToStream(stream, bitcoinTransaction.rawBytes)
    }
}
