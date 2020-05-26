// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.sdk.services;

import org.veriblock.core.crypto.Sha256Hash;
import org.veriblock.core.crypto.VBlakeHash;
import org.veriblock.core.utilities.Preconditions;
import org.veriblock.core.utilities.Utility;
import org.veriblock.sdk.models.Address;
import org.veriblock.sdk.models.AltPublication;
import org.veriblock.sdk.models.BitcoinBlock;
import org.veriblock.sdk.models.BitcoinTransaction;
import org.veriblock.sdk.models.BlockType;
import org.veriblock.sdk.models.Coin;
import org.veriblock.sdk.models.Constants;
import org.veriblock.sdk.models.MerklePath;
import org.veriblock.sdk.models.Output;
import org.veriblock.sdk.models.PublicationData;
import org.veriblock.sdk.models.VeriBlockBlock;
import org.veriblock.sdk.models.VeriBlockMerklePath;
import org.veriblock.sdk.models.VeriBlockPopTransaction;
import org.veriblock.sdk.models.VeriBlockPublication;
import org.veriblock.sdk.models.VeriBlockTransaction;
import org.veriblock.sdk.util.BytesUtility;
import org.veriblock.sdk.util.StreamUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class SerializeDeserializeService {

    public static void serialize(VeriBlockPopTransaction veriBlockPoPTransaction, OutputStream stream) throws IOException {
        byte[] rawTransaction = serializeTransactionEffects(veriBlockPoPTransaction);
        StreamUtils.writeVariableLengthValueToStream(stream, rawTransaction);

        StreamUtils.writeSingleByteLengthValueToStream(stream, veriBlockPoPTransaction.getSignature());
        StreamUtils.writeSingleByteLengthValueToStream(stream, veriBlockPoPTransaction.getPublicKey());
    }

    public static byte[] serializeTransactionEffects(VeriBlockPopTransaction veriBlockPoPTransaction) {
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
            serializeTransactionEffects(veriBlockPoPTransaction, stream);
            return stream.toByteArray();
        } catch (IOException ignore) {
            // Should not happen
        }
        return new byte[] {};
    }

    private static void serializeTransactionEffects(VeriBlockPopTransaction tx, OutputStream stream) throws IOException {
        if (tx.getNetworkByte() != null) {
            // Replay protection versus mainnet network
            stream.write(tx.getNetworkByte());
        }

        // Write type
        stream.write(BlockType.VERI_BLOCK_POP_TX.getId());

        serialize(tx.getAddress(), stream);

        // Write size (in bytes) of endorsed VeriBlock block header (will always be 64 bytes)
        serialize(tx.getPublishedBlock(), stream);

        // Write the Bitcoin transaction
        serialize(tx.getBitcoinTransaction(), stream);

        // write Merkle path
        serialize(tx.getMerklePath(), stream);

        // Write Bitcoin block header of proof
        serialize(tx.getBlockOfProof(), stream);

        // Write number of context Bitcoin block headers (can be 0)
        StreamUtils.writeSingleByteLengthValueToStream(stream, tx.getBlockOfProofContext().size());

        for (BitcoinBlock block : tx.getBlockOfProofContext()) {
            SerializeDeserializeService.serialize(block, stream);
        }
    }

    public static Sha256Hash getId(VeriBlockPopTransaction veriBlockPoPTransaction) {
        return Sha256Hash.of(serializeTransactionEffects(veriBlockPoPTransaction));
    }

    public static Sha256Hash getHash(VeriBlockPopTransaction veriBlockPoPTransaction) {
        return Sha256Hash.of(serializeTransactionEffects(veriBlockPoPTransaction));
    }

// VeriBlockPublication
    public static byte[] serialize(VeriBlockPublication veriBlockPublication) {
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
            serialize(veriBlockPublication, stream);
            return stream.toByteArray();
        } catch (IOException ignore) {
            // Should not happen
        }
        return new byte[] {};
    }

    public static void serialize(VeriBlockPublication veriBlockPublication, OutputStream stream) throws IOException {
        serialize(veriBlockPublication.getTransaction(), stream);
        serialize(veriBlockPublication.getMerklePath(), stream);
        serialize(veriBlockPublication.getContainingBlock(), stream);

        // Write number of context Bitcoin block headers (can be 0)
        StreamUtils.writeSingleByteLengthValueToStream(stream, veriBlockPublication.getContext().size());
        for (VeriBlockBlock block : veriBlockPublication.getContext()) {
            SerializeDeserializeService.serialize(block, stream);
        }
    }

// VeriBlockTransaction
    public static void serialize(VeriBlockTransaction veriBlockTransaction, OutputStream stream) throws IOException {
        byte[] rawTransaction = serializeTransactionEffects(veriBlockTransaction);
        StreamUtils.writeVariableLengthValueToStream(stream, rawTransaction);

        StreamUtils.writeSingleByteLengthValueToStream(stream, veriBlockTransaction.getSignature());
        StreamUtils.writeSingleByteLengthValueToStream(stream, veriBlockTransaction.getPublicKey());
    }

    public static byte[] serializeTransactionEffects(VeriBlockTransaction veriBlockTransaction) {
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
            serializeTransactionEffects(veriBlockTransaction, stream);

            return stream.toByteArray();
        } catch (IOException ignore) {
            // Should not happen
        }

        return new byte[] {};
    }

    public static void serializeTransactionEffects(VeriBlockTransaction veriBlockTransaction, OutputStream stream) throws IOException {
        if (veriBlockTransaction.getNetworkByte() != null) {
            stream.write(veriBlockTransaction.getNetworkByte());
        }

        stream.write(veriBlockTransaction.getType());

        serialize(veriBlockTransaction.getSourceAddress(), stream);
        serialize(veriBlockTransaction.getSourceAmount(), stream);

        stream.write((byte)veriBlockTransaction.getOutputs().size());
        for (Output o : veriBlockTransaction.getOutputs()) {
            serialize(o, stream);
        }

        byte[] publicationDataBytes = SerializeDeserializeService.serialize(veriBlockTransaction.getPublicationData());

        StreamUtils.writeSingleByteLengthValueToStream(stream, veriBlockTransaction.getSignatureIndex());
        StreamUtils.writeVariableLengthValueToStream(stream, publicationDataBytes);
    }

    public static Sha256Hash getId(VeriBlockTransaction veriBlockTransaction) {
        return Sha256Hash.of(serializeTransactionEffects(veriBlockTransaction));
    }

// VeriBlockBlock
    public static VeriBlockBlock parseVeriBlockBlock(ByteBuffer buffer) {
        byte[] raw = StreamUtils.getSingleByteLengthValue(buffer, Constants.HEADER_SIZE_VeriBlockBlock, Constants.HEADER_SIZE_VeriBlockBlock);
        return parseVeriBlockBlock(raw);
    }

    public static VeriBlockBlock parseVeriBlockBlock(byte[] raw){
        Preconditions.notNull(raw, "VeriBlock raw data cannot be null");
        Preconditions.argument(raw.length == Constants.HEADER_SIZE_VeriBlockBlock, () -> "Invalid VeriBlock raw data: " + Utility.bytesToHex(raw));

        ByteBuffer buffer = ByteBuffer.allocateDirect(raw.length);

        buffer.put(raw);
        buffer.flip();

        int height = BytesUtility.readBEInt32(buffer);
        short version = BytesUtility.readBEInt16(buffer);
        VBlakeHash previousBlock = VBlakeHash.extract(buffer, VBlakeHash.PREVIOUS_BLOCK_LENGTH);
        VBlakeHash previousKeystone = VBlakeHash.extract(buffer, VBlakeHash.PREVIOUS_KEYSTONE_LENGTH);
        VBlakeHash secondPreviousKeystone = VBlakeHash.extract(buffer, VBlakeHash.PREVIOUS_KEYSTONE_LENGTH);
        Sha256Hash merkleRoot = Sha256Hash.extract(buffer, Sha256Hash.VERIBLOCK_MERKLE_ROOT_LENGTH, ByteOrder.BIG_ENDIAN);
        int timestamp = BytesUtility.readBEInt32(buffer);
        int difficulty = BytesUtility.readBEInt32(buffer);
        int nonce = BytesUtility.readBEInt32(buffer);

        VeriBlockBlock veriBlockBlock = new VeriBlockBlock(
                height, version, previousBlock, previousKeystone, secondPreviousKeystone, merkleRoot,
                timestamp, difficulty, nonce);

        return veriBlockBlock;
    }

    public static void serialize(VeriBlockBlock veriBlockBlock, OutputStream stream) throws IOException {
        StreamUtils.writeSingleByteLengthValueToStream(stream, serializeHeaders(veriBlockBlock));
    }

    public static byte[] serialize(VeriBlockBlock veriBlockBlock) {
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
            serialize(veriBlockBlock, stream);

            return stream.toByteArray();
        } catch (IOException ignore) {
            // Should not happen
        }

        return new byte[] {};
    }

    public static byte[] serializeHeaders(VeriBlockBlock veriBlockBlock) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(Constants.HEADER_SIZE_VeriBlockBlock);
        BytesUtility.putBEInt32(buffer, veriBlockBlock.getHeight());
        BytesUtility.putBEInt16(buffer, veriBlockBlock.getVersion());
        BytesUtility.putBEBytes(buffer, veriBlockBlock.getPreviousBlock().getBytes());
        BytesUtility.putBEBytes(buffer, veriBlockBlock.getPreviousKeystone().getBytes());
        BytesUtility.putBEBytes(buffer, veriBlockBlock.getSecondPreviousKeystone().getBytes());
        BytesUtility.putBEBytes(buffer, veriBlockBlock.getMerkleRoot().getBytes());
        BytesUtility.putBEInt32(buffer, veriBlockBlock.getTimestamp());
        BytesUtility.putBEInt32(buffer, veriBlockBlock.getDifficulty());
        BytesUtility.putBEInt32(buffer, veriBlockBlock.getNonce());

        buffer.flip();
        byte[] bytes = new byte[Constants.HEADER_SIZE_VeriBlockBlock];
        buffer.get(bytes, 0, Constants.HEADER_SIZE_VeriBlockBlock);

        return bytes;
    }

//MerklePath
    public static void serialize(MerklePath merklePath, OutputStream stream) throws IOException {
        StreamUtils.writeVariableLengthValueToStream(stream, serializeComponents(merklePath));
    }

    private static byte[] serializeComponents(MerklePath merklePath) {
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
            serializeComponentsToStream(merklePath, stream);

            return stream.toByteArray();
        } catch (IOException ignore) {
            // Should not happen
        }

        return new byte[] {};
    }

    public static void serializeComponentsToStream(MerklePath merklePath, OutputStream stream) throws IOException {
        // Index
        StreamUtils.writeSingleIntLengthValueToStream(stream, merklePath.getIndex());

        // Layer size
        StreamUtils.writeSingleIntLengthValueToStream(stream, merklePath.getLayers().size());

        byte[] sizeBottomData = Utility.toByteArray(merklePath.getSubject().length);

        // Write size of the int describing the size of the bottom layer of data
        StreamUtils.writeSingleIntLengthValueToStream(stream, sizeBottomData.length);

        stream.write(sizeBottomData);

        for (Sha256Hash hash : merklePath.getLayers()) {
            byte[] layer = hash.getBytes();
            StreamUtils.writeSingleByteLengthValueToStream(stream, layer);
        }
    }

// VeriBlockMerklePath
    public static void serialize(VeriBlockMerklePath blockMerklePath, OutputStream stream) throws IOException {
    	// Tree index
        StreamUtils.writeSingleIntLengthValueToStream(stream, blockMerklePath.getTreeIndex());

        // Index
        StreamUtils.writeSingleIntLengthValueToStream(stream, blockMerklePath.getIndex());

        // Subject
        byte[] subjectBytes = blockMerklePath.getSubject().getBytes();
        StreamUtils.writeSingleByteLengthValueToStream(stream, subjectBytes);

        // Layer size
        StreamUtils.writeSingleIntLengthValueToStream(stream, blockMerklePath.getLayers().size());

        // Layers
        for (Sha256Hash hash : blockMerklePath.getLayers()) {
            byte[] layer = hash.getBytes();
            StreamUtils.writeSingleByteLengthValueToStream(stream, layer);
        }
    }

// BitcoinBlock
    public static byte[] getHeaderBytesBitcoinBlock(BitcoinBlock bitcoinBlock) {
        if (bitcoinBlock.getRaw() != null && bitcoinBlock.getRaw().length == Constants.HEADER_SIZE_BitcoinBlock){
            return bitcoinBlock.getRaw();
        }

        ByteBuffer buffer = ByteBuffer.allocateDirect(Constants.HEADER_SIZE_BitcoinBlock);
        BytesUtility.putLEInt32(buffer, bitcoinBlock.getVersion());
        BytesUtility.putLEBytes(buffer, bitcoinBlock.getPreviousBlock().getBytes());
        BytesUtility.putLEBytes(buffer, bitcoinBlock.getMerkleRoot().getBytes());
        BytesUtility.putLEInt32(buffer, bitcoinBlock.getTimestamp());
        BytesUtility.putLEInt32(buffer, bitcoinBlock.getDifficulty());
        BytesUtility.putLEInt32(buffer, bitcoinBlock.getNonce());

        buffer.flip();
        byte[] bytes = new byte[Constants.HEADER_SIZE_BitcoinBlock];
        buffer.get(bytes);

        return bytes;
    }

    public static byte[] serialize(BitcoinBlock bitcoinBlock) {
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
            serialize(bitcoinBlock, stream);
            return stream.toByteArray();
        } catch (IOException ignore) {
            // Should not happen
        }
        return new byte[] {};
    }

    public static void serialize(BitcoinBlock bitcoinBlock, OutputStream stream) throws IOException {
        StreamUtils.writeSingleByteLengthValueToStream(stream, getHeaderBytesBitcoinBlock(bitcoinBlock));
    }

    public static BitcoinBlock parseBitcoinBlockWithLength(ByteBuffer buffer) {
        byte[] raw = StreamUtils.getSingleByteLengthValue(buffer, Constants.HEADER_SIZE_BitcoinBlock, Constants.HEADER_SIZE_BitcoinBlock);
        return parseBitcoinBlock(raw);
    }

    public static BitcoinBlock parseBitcoinBlock(byte[] bytes) {
        Preconditions.notNull(bytes, "Raw Bitcoin Block cannot be null");
        Preconditions.argument(bytes.length == Constants.HEADER_SIZE_BitcoinBlock, "Invalid raw Bitcoin Block: " + Utility.bytesToHex(bytes));

        ByteBuffer buffer = ByteBuffer.allocateDirect(bytes.length);

        buffer.put(bytes);
        buffer.flip();

        Integer version = BytesUtility.readLEInt32(buffer);
        Sha256Hash previousBlock = Sha256Hash.extract(buffer);
        Sha256Hash merkleRoot = Sha256Hash.extract(buffer);
        Integer timestamp = BytesUtility.readLEInt32(buffer);
        Integer bits = BytesUtility.readLEInt32(buffer);
        Integer nonce = BytesUtility.readLEInt32(buffer);

        BitcoinBlock bitcoinBlock = new BitcoinBlock(version,previousBlock, merkleRoot, timestamp, bits, nonce);

        return bitcoinBlock;
    }

// Address
    public static byte[] serialize(Address address) {
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
            serialize(address, stream);
            return stream.toByteArray();
        } catch (IOException ignore) {
            // Should not happen
        }
        return new byte[] {};
    }

    public static void serialize(Address address, OutputStream stream) throws IOException {
        byte[] bytes = address.getBytes();
        if (address.isMultisig()) {
            stream.write(3);
        } else {
            stream.write(1);
        }

        StreamUtils.writeSingleByteLengthValueToStream(stream, bytes);
    }

// Coin
    public static byte[] serialize(Coin coin) {
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
            serialize(coin, stream);
            return stream.toByteArray();
        } catch (IOException ignore) {
            // Should not happen
        }
        return new byte[] {};
    }

    public static void serialize(Coin coin, OutputStream stream) throws IOException {
        StreamUtils.writeSingleByteLengthValueToStream(stream, coin.getAtomicUnits());
    }

// Output
    public static void serialize(Output output, OutputStream stream) throws IOException {
        serialize(output.getAddress(), stream);
        serialize(output.getAmount(), stream);
    }

// AltPublication
    public static byte[] serialize(AltPublication altPublication) {
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
            serialize(altPublication, stream);
            return stream.toByteArray();
        } catch (IOException ignore) {
            // Should not happen
        }
        return new byte[] {};
    }

    public static void serialize(AltPublication altPublication, ByteArrayOutputStream stream) throws IOException {
        serialize(altPublication.getTransaction(), stream);
        serialize(altPublication.getMerklePath(), stream);
        serialize(altPublication.getContainingBlock(), stream);

        // Write number of context Bitcoin block headers (can be 0)
        StreamUtils.writeSingleByteLengthValueToStream(stream, altPublication.getContext().size());
        for (VeriBlockBlock block : altPublication.getContext()) {
            serialize(block, stream);
        }
    }


// PublicationData
    public static byte[] serialize(PublicationData publicationData) {
        if(publicationData == null)
            return new byte [] {};
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
            serialize(publicationData, stream);
            return stream.toByteArray();
        } catch (IOException ignore) {
            // Should not happen
        }
        return new byte[] {};
    }

    public static void serialize(PublicationData publicationData, OutputStream stream) throws IOException {
        StreamUtils.writeSingleByteLengthValueToStream(stream, publicationData.getIdentifier());
        StreamUtils.writeVariableLengthValueToStream(stream, publicationData.getHeader());
        StreamUtils.writeVariableLengthValueToStream(stream, publicationData.getContextInfo());
        StreamUtils.writeVariableLengthValueToStream(stream, publicationData.getPayoutInfo());
    }

    public static PublicationData parsePublicationData(byte[] data) {
        if (data == null || data.length == 0) return null;

        ByteBuffer buffer = ByteBuffer.wrap(data);
        byte[] identifierBytes = StreamUtils.getSingleByteLengthValue(buffer, 0, 8);
        long identifier = Utility.toLong(identifierBytes);
        byte[] headerBytes = StreamUtils.getVariableLengthValue(buffer, 0, Constants.MAX_HEADER_SIZE_PUBLICATION_DATA);
        byte[] contextInfoBytes = StreamUtils.getVariableLengthValue(buffer, 0, Constants.MAX_PAYOUT_SIZE_PUBLICATION_DATA);
        byte[] payoutInfoBytes = StreamUtils.getVariableLengthValue(buffer, 0, Constants.MAX_CONTEXT_SIZE_PUBLICATION_DATA);

        return new PublicationData(identifier, headerBytes, payoutInfoBytes, contextInfoBytes);
    }

// BitcoinTransaction

    public static void serialize(BitcoinTransaction bitcoinTransaction, OutputStream stream) throws IOException {
        StreamUtils.writeVariableLengthValueToStream(stream, bitcoinTransaction.getRawBytes());
    }
}
