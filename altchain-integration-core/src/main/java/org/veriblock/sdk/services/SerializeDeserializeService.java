// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.sdk.services;

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
import org.veriblock.sdk.models.Sha256Hash;
import org.veriblock.sdk.models.VBlakeHash;
import org.veriblock.sdk.models.VeriBlockBlock;
import org.veriblock.sdk.models.VeriBlockMerklePath;
import org.veriblock.sdk.models.VeriBlockPoPTransaction;
import org.veriblock.sdk.models.VeriBlockPublication;
import org.veriblock.sdk.models.VeriBlockTransaction;
import org.veriblock.sdk.util.Base58;
import org.veriblock.sdk.util.Base59;
import org.veriblock.sdk.util.Preconditions;
import org.veriblock.sdk.util.StreamUtils;
import org.veriblock.sdk.util.Utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

public class SerializeDeserializeService {

    public static final int MAX_SIZE_PUBLICATION_DATA =
                    // identifier.size, identifier
                    9 +
                    // header.size.size, header.size, header
                    5 + Constants.MAX_HEADER_SIZE_PUBLICATION_DATA +
                    // payoutInfo.size.size, payoutInfo.size, payoutInfo
                    5 + Constants.MAX_PAYOUT_SIZE_PUBLICATION_DATA +
                    // contextInfo.size.size, contextInfo.size, contextInfo
                    5 + Constants.MAX_CONTEXT_SIZE_PUBLICATION_DATA;

    public static final int MAX_RAWTX_SIZE_VeriBlockPoPTransaction =
                    // network byte, type
                    1 + 1 +
                    // address.size, address
                    1 + Address.SIZE +
                    // publishedBlock.size, publishedBlock
                    1 + Constants.HEADER_SIZE_VeriBlockBlock +
                    // bitcoinTransaction.size.size, bitcoinTransaction.size, bitcoinTransaction
                    5 + Constants.MAX_RAWTX_SIZE + Constants.MAX_MERKLE_BYTES +
                    // blockOfProof.size, blockOfProof
                    1 + Constants.HEADER_SIZE_BitcoinBlock +
                    // blockOfProofContext.size.size, blockOfProofContext.size, blockOfProofContext
                    5 + (Constants.HEADER_SIZE_BitcoinBlock + 1) * Constants.MAX_CONTEXT_COUNT +
                    // signature.size, signature
                    1 + Constants.MAX_SIGNATURE_SIZE +
                    // publicKey.size, publicKey
                    1 + Constants.PUBLIC_KEY_SIZE +
                    // raw.size.size, raw.size
                    5;

    public static final int MAX_RAWTX_SIZE_VeriBlockTransaction =
                    // network byte, type
                    1 + 1 +
                    // sourceAddress.size, sourceAddress
                    1 + Address.SIZE +
                    // sourceAmount.size, sourceAmount
                    1 + 8 +
                    // outputs.size, outputs.size * (output.address.size + output.address + output.amount.size + output.amount)
                    1 + Constants.MAX_OUTPUTS_COUNT * (1 + Address.SIZE + 1 + 8) +
                    // signatureIndex.size, signatureIndex
                    1 + 8 +
                    // data.size.size, data.size, data
                    5 + MAX_SIZE_PUBLICATION_DATA +
                    // signature.size, signature
                    1 + Constants.MAX_SIGNATURE_SIZE +
                    // publicKey.size, publicKey
                    1 + Constants.PUBLIC_KEY_SIZE +
                    // raw.size.size, raw.size
                    5;




    public static VeriBlockPoPTransaction parseVeriBlockPoPTx(ByteBuffer buffer) {
        byte[] rawTx = StreamUtils.getVariableLengthValue(buffer, 0, MAX_RAWTX_SIZE_VeriBlockPoPTransaction);
        byte[] signature = StreamUtils.getSingleByteLengthValue(buffer, 0, Constants.MAX_SIGNATURE_SIZE);
        byte[] publicKey = StreamUtils.getSingleByteLengthValue(buffer, Constants.PUBLIC_KEY_SIZE, Constants.PUBLIC_KEY_SIZE);

        ByteBuffer txBuffer = ByteBuffer.wrap(rawTx);

        Byte networkByte;
        byte networkOrType = txBuffer.get();
        if (networkOrType == BlockType.VERI_BLOCK_POP_TX.getId()) {
            networkByte = null;
        } else {
            networkByte = networkOrType;
            txBuffer.get();
        }

        Address address = parseAddress(txBuffer);
        VeriBlockBlock publishedBlock = parseVeriBlockBlock(txBuffer);
        BitcoinTransaction bitcoinTransaction = parseBitcoinTransaction(txBuffer);
        MerklePath merklePath = parseMerklePath(txBuffer, Sha256Hash.twiceOf(bitcoinTransaction.getRawBytes()));
        BitcoinBlock blockOfProof = parseBitcoinBlockWithLength(txBuffer);

        int contextCount = Utils.toInt(StreamUtils.getSingleByteLengthValue(txBuffer, 0, Constants.MAX_CONTEXT_COUNT));
        if (contextCount < 0 || contextCount > Constants.MAX_CONTEXT_COUNT) {
            throw new IllegalArgumentException("Unexpected context count: " + contextCount
                    + " (expected a value between 0 and " + Constants.MAX_CONTEXT_COUNT + ")");
        }

        List<BitcoinBlock> contextBlocks = new ArrayList<>(contextCount);
        for (int i = 0; i < contextCount; i++) {
            contextBlocks.add(parseBitcoinBlockWithLength(txBuffer));
        }

        return new VeriBlockPoPTransaction(address, publishedBlock, bitcoinTransaction, merklePath,
                blockOfProof, contextBlocks, signature, publicKey, networkByte);
    }

    public static byte[] serialize(VeriBlockPoPTransaction veriBlockPoPTransaction) {
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
            serialize(veriBlockPoPTransaction, stream);

            return stream.toByteArray();
        } catch (IOException ignore) {
            // Should not happen
        }
        return new byte[] {};
    }

    public static void serialize(VeriBlockPoPTransaction veriBlockPoPTransaction, OutputStream stream) throws IOException {
        byte[] rawTransaction = serializeTransactionEffects(veriBlockPoPTransaction);
        StreamUtils.writeVariableLengthValueToStream(stream, rawTransaction);

        StreamUtils.writeSingleByteLengthValueToStream(stream, veriBlockPoPTransaction.getSignature());
        StreamUtils.writeSingleByteLengthValueToStream(stream, veriBlockPoPTransaction.getPublicKey());
    }

    public static byte[] serializeTransactionEffects(VeriBlockPoPTransaction veriBlockPoPTransaction) {
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
            serializeTransactionEffects(veriBlockPoPTransaction, stream);
            return stream.toByteArray();
        } catch (IOException ignore) {
            // Should not happen
        }
        return new byte[] {};
    }

    private static void serializeTransactionEffects(VeriBlockPoPTransaction tx, OutputStream stream) throws IOException {
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

    public static Sha256Hash getId(VeriBlockPoPTransaction veriBlockPoPTransaction) {
        return Sha256Hash.of(serializeTransactionEffects(veriBlockPoPTransaction));
    }

    public static Sha256Hash getHash(VeriBlockPoPTransaction veriBlockPoPTransaction) {
        return Sha256Hash.of(serializeTransactionEffects(veriBlockPoPTransaction));
    }


// ----  - - - - -- -  VeriBlockPublication - - - -- - - - --

    public static VeriBlockPublication parseVeriBlockPublication(ByteBuffer buffer) {

        VeriBlockPoPTransaction transaction = parseVeriBlockPoPTx(buffer);
        VeriBlockMerklePath merklePath = parseVeriBlockMerklePath(buffer);
        VeriBlockBlock containingBlock = parseVeriBlockBlock(buffer);

        int contextCount = Utils.toInt(StreamUtils.getSingleByteLengthValue(buffer, 0, 4));

        if (contextCount < 0 || contextCount > Constants.MAX_CONTEXT_COUNT) {
            throw new IllegalArgumentException("Unexpected context count: " + contextCount
                    + " (expected a value between 0 and " + Constants.MAX_CONTEXT_COUNT + ")");
        }

        List<VeriBlockBlock> contextBlocks = new ArrayList<>(contextCount);
        for (int i = 0; i < contextCount; i++) {
            contextBlocks.add(parseVeriBlockBlock(buffer));
        }

        return new VeriBlockPublication(transaction, merklePath, containingBlock, contextBlocks);
    }

    public static VeriBlockPublication parseVeriBlockPublication(byte[] raw)
    {
        ByteBuffer buffer = ByteBuffer.wrap(raw);
        return parseVeriBlockPublication(buffer);
    }

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


//VeriBlockTransaction

    public static VeriBlockTransaction parseVeriBlockTransaction(ByteBuffer buffer) {
        byte[] rawTx = StreamUtils.getVariableLengthValue(buffer, 0, MAX_RAWTX_SIZE_VeriBlockTransaction);
        byte[] signature = StreamUtils.getSingleByteLengthValue(buffer, 0, Constants.MAX_SIGNATURE_SIZE);
        byte[] publicKey = StreamUtils.getSingleByteLengthValue(buffer, Constants.PUBLIC_KEY_SIZE, Constants.PUBLIC_KEY_SIZE);

        ByteBuffer txBuffer = ByteBuffer.wrap(rawTx);

        Byte networkByte;
        byte typeId;
        byte networkOrType = txBuffer.get();
        if (networkOrType == BlockType.VERI_BLOCK_TX.getId()) {
            networkByte = null;
            typeId = networkOrType;
        } else {
            networkByte = networkOrType;
            typeId = txBuffer.get();
        }

        Address sourceAddress = SerializeDeserializeService.parseAddress(txBuffer);
        Coin sourceAmount = Coin.parse(txBuffer);

        int outputSize = txBuffer.get();

        if (outputSize < 0 || outputSize > Constants.MAX_OUTPUTS_COUNT) {
            throw new IllegalArgumentException("Unexpected outputs count: " + outputSize
                    + " (expected a value between 0 and " + Constants.MAX_OUTPUTS_COUNT + ")");
        }

        List<Output> outputs = new ArrayList<>(outputSize);
        for (int i = 0; i < outputSize; i++) {
            outputs.add(SerializeDeserializeService.parseOutput(txBuffer));
        }

        long signatureIndex = Utils.toLong(StreamUtils.getSingleByteLengthValue(txBuffer, 0, 8));
        byte[] publicationDataBytes = StreamUtils.getVariableLengthValue(txBuffer, 0, MAX_SIZE_PUBLICATION_DATA);
        PublicationData publicationData = SerializeDeserializeService.parsePublicationData(publicationDataBytes);

        return new VeriBlockTransaction(typeId, sourceAddress, sourceAmount, outputs,
                signatureIndex, publicationData, signature, publicKey, networkByte);
    }

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
        if (veriBlockTransaction.getId() == null) {
            veriBlockTransaction.setId(Sha256Hash.of(serializeTransactionEffects(veriBlockTransaction)));
        }
        return veriBlockTransaction.getId();
    }

    public static byte[] serialize(VeriBlockTransaction veriBlockTransaction) {
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
            serialize(veriBlockTransaction, stream);
            return stream.toByteArray();
        } catch (IOException ignore) {
            // Should not happen
            //TODO add logs
        }
        return new byte[] {};
    }

// VeriBlockBlock

    public static VeriBlockBlock parseVeriBlockBlock(ByteBuffer buffer) {
        byte[] raw = StreamUtils.getSingleByteLengthValue(buffer, Constants.HEADER_SIZE_VeriBlockBlock, Constants.HEADER_SIZE_VeriBlockBlock);
        return parseVeriBlockBlock(raw);
    }

    public static VeriBlockBlock parseVeriBlockBlock(byte[] raw){
        Preconditions.notNull(raw, "VeriBlock raw data cannot be null");
        Preconditions.argument(raw.length == Constants.HEADER_SIZE_VeriBlockBlock, () -> "Invalid VeriBlock raw data: " + Utils.encodeHex(raw));

        ByteBuffer buffer = ByteBuffer.allocateDirect(raw.length);

        buffer.put(raw);
        buffer.flip();

        int height = Utils.Bytes.readBEInt32(buffer);
        short version = Utils.Bytes.readBEInt16(buffer);
        VBlakeHash previousBlock = VBlakeHash.extract(buffer, VBlakeHash.PREVIOUS_BLOCK_LENGTH);
        VBlakeHash previousKeystone = VBlakeHash.extract(buffer, VBlakeHash.PREVIOUS_KEYSTONE_LENGTH);
        VBlakeHash secondPreviousKeystone = VBlakeHash.extract(buffer, VBlakeHash.PREVIOUS_KEYSTONE_LENGTH);
        Sha256Hash merkleRoot = Sha256Hash.extract(buffer, Sha256Hash.VERIBLOCK_MERKLE_ROOT_LENGTH, ByteOrder.BIG_ENDIAN);
        int timestamp = Utils.Bytes.readBEInt32(buffer);
        int difficulty = Utils.Bytes.readBEInt32(buffer);
        int nonce = Utils.Bytes.readBEInt32(buffer);

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
        Utils.Bytes.putBEInt32(buffer, veriBlockBlock.getHeight());
        Utils.Bytes.putBEInt16(buffer, veriBlockBlock.getVersion());
        Utils.Bytes.putBEBytes(buffer, veriBlockBlock.getPreviousBlock().getBytes());
        Utils.Bytes.putBEBytes(buffer, veriBlockBlock.getPreviousKeystone().getBytes());
        Utils.Bytes.putBEBytes(buffer, veriBlockBlock.getSecondPreviousKeystone().getBytes());
        Utils.Bytes.putBEBytes(buffer, veriBlockBlock.getMerkleRoot().getBytes());
        Utils.Bytes.putBEInt32(buffer, veriBlockBlock.getTimestamp());
        Utils.Bytes.putBEInt32(buffer, veriBlockBlock.getDifficulty());
        Utils.Bytes.putBEInt32(buffer, veriBlockBlock.getNonce());

        buffer.flip();
        byte[] bytes = new byte[Constants.HEADER_SIZE_VeriBlockBlock];
        buffer.get(bytes, 0, Constants.HEADER_SIZE_VeriBlockBlock);

        return bytes;
    }


//MerklePath

    // Unfortunately, the serialized MerklePath coming from NodeCore does not contain
    // the subject data, so it must be supplied.
    public static MerklePath parseMerklePath(ByteBuffer buffer, Sha256Hash subject) {
        byte[] merkleBytes = StreamUtils.getVariableLengthValue(buffer, 0, Constants.MAX_MERKLE_BYTES);
        ByteBuffer localBuffer = ByteBuffer.wrap(merkleBytes);

        int index = StreamUtils.getSingleIntValue(localBuffer);
        int numLayers = StreamUtils.getSingleIntValue(localBuffer);
        int sizeOfSizeBottomData = StreamUtils.getSingleIntValue(localBuffer);
        byte[] sizeBottomData = new byte[sizeOfSizeBottomData];
        localBuffer.get(sizeBottomData);

        if (Utils.toInt(sizeBottomData) != Sha256Hash.BITCOIN_LENGTH) {
            throw new IllegalArgumentException("Unexpected sizeBottomData: " + Utils.toInt(sizeBottomData)
                    + " (expected value: " + Sha256Hash.BITCOIN_LENGTH + ")");
        }

        if (numLayers < 0 || numLayers > Constants.MAX_LAYER_COUNT_MERKLE) {
            throw new IllegalArgumentException("Unexpected layer count: " + numLayers
                    + " (expected a value between 0 and " + Constants.MAX_LAYER_COUNT_MERKLE + ")");
        }

        List<Sha256Hash> layers = new ArrayList<>(numLayers);
        for (int i = 0; i < numLayers; i++) {
            layers.add(Sha256Hash.wrap(StreamUtils.getSingleByteLengthValue(localBuffer, Sha256Hash.BITCOIN_LENGTH, Sha256Hash.BITCOIN_LENGTH)));
        }

        return new MerklePath(index, subject, layers);
    }


    public static byte[] serialize(MerklePath merklePath) {
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
            serialize(merklePath, stream);
            return stream.toByteArray();
        } catch (IOException ignore) {
            // Should not happen
        }
        return new byte[] {};
    }

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

        byte[] sizeBottomData = Utils.toByteArray(merklePath.getSubject().length);

        // Write size of the int describing the size of the bottom layer of data
        StreamUtils.writeSingleIntLengthValueToStream(stream, sizeBottomData.length);

        stream.write(sizeBottomData);

        for (Sha256Hash hash : merklePath.getLayers()) {
            byte[] layer = hash.getBytes();
            StreamUtils.writeSingleByteLengthValueToStream(stream, layer);
        }
    }

// VeriBlockMerklePath

    public static byte[] serialize(VeriBlockMerklePath merklePath) {
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
            serialize(merklePath, stream);

            return stream.toByteArray();
        } catch (IOException ignore) {
            // Should not happen
        }

        return new byte[] {};
    }

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

    public static VeriBlockMerklePath parseVeriBlockMerklePath(ByteBuffer buffer) {
        int treeIndex = StreamUtils.getSingleIntValue(buffer);
        int index = StreamUtils.getSingleIntValue(buffer);
        Sha256Hash subject = Sha256Hash.wrap(StreamUtils.getSingleByteLengthValue(buffer, Sha256Hash.BITCOIN_LENGTH, Sha256Hash.BITCOIN_LENGTH));
        int numLayers = StreamUtils.getSingleIntValue(buffer);

        if (numLayers < 0 || numLayers > Constants.MAX_LAYER_COUNT_MERKLE) {
            throw new IllegalArgumentException("Unexpected layer count: " + numLayers
                    + " (expected a value between 0 and " + Constants.MAX_LAYER_COUNT_MERKLE + ")");
        }

        List<Sha256Hash> layers = new ArrayList<>(numLayers);
        for (int i = 0; i < numLayers; i++) {
            layers.add(Sha256Hash.wrap(StreamUtils.getSingleByteLengthValue(buffer, Sha256Hash.BITCOIN_LENGTH, Sha256Hash.BITCOIN_LENGTH)));
        }

        return new VeriBlockMerklePath(treeIndex, index, subject, layers);
    }

// BitcoinBlock

    public static byte[] getHeaderBytesBitcoinBlock(BitcoinBlock bitcoinBlock) {
        if (bitcoinBlock.getRaw() != null && bitcoinBlock.getRaw().length == Constants.HEADER_SIZE_BitcoinBlock){
            return bitcoinBlock.getRaw();
        }

        ByteBuffer buffer = ByteBuffer.allocateDirect(Constants.HEADER_SIZE_BitcoinBlock);
        Utils.Bytes.putLEInt32(buffer, bitcoinBlock.getVersion());
        Utils.Bytes.putLEBytes(buffer, bitcoinBlock.getPreviousBlock().getBytes());
        Utils.Bytes.putLEBytes(buffer, bitcoinBlock.getMerkleRoot().getBytes());
        Utils.Bytes.putLEInt32(buffer, bitcoinBlock.getTimestamp());
        Utils.Bytes.putLEInt32(buffer, bitcoinBlock.getBits());
        Utils.Bytes.putLEInt32(buffer, bitcoinBlock.getNonce());

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
        Preconditions.argument(bytes.length == Constants.HEADER_SIZE_BitcoinBlock, "Invalid raw Bitcoin Block: " + Utils.encodeHex(bytes));

        ByteBuffer buffer = ByteBuffer.allocateDirect(bytes.length);

        buffer.put(bytes);
        buffer.flip();

        Integer version = Utils.Bytes.readLEInt32(buffer);
        Sha256Hash previousBlock = Sha256Hash.extract(buffer);
        Sha256Hash merkleRoot = Sha256Hash.extract(buffer);
        Integer timestamp = Utils.Bytes.readLEInt32(buffer);
        Integer bits = Utils.Bytes.readLEInt32(buffer);
        Integer nonce = Utils.Bytes.readLEInt32(buffer);

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

    public static Address parseAddress(ByteBuffer buffer) {
        int addressType = buffer.get();
        byte[] addressBytes = StreamUtils.getSingleByteLengthValue(buffer, 0, Constants.SIZE_ADDRESS);
        if (addressType == 1) {
            return new Address(Base58.encode(addressBytes));
        } else {
            return new Address(Base59.encode(addressBytes));
        }
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
    public static byte[] serialize(Output output) {
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
            serialize(output, stream);
            return stream.toByteArray();
        } catch (IOException ignore) {
            // Should not happen
        }
        return new byte[] {};
    }

    public static void serialize(Output output, OutputStream stream) throws IOException {
        serialize(output.getAddress(), stream);
        serialize(output.getAmount(), stream);
    }

    public static Output parseOutput(ByteBuffer txBuffer) {
        Address address = parseAddress(txBuffer);
        Coin amount = Coin.parse(txBuffer);

        return new Output(address, amount);
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

    public static AltPublication parseAltPublication(byte[] raw) {
        ByteBuffer buffer = ByteBuffer.wrap(raw);

        VeriBlockTransaction transaction = parseVeriBlockTransaction(buffer);
        VeriBlockMerklePath merklePath = parseVeriBlockMerklePath(buffer);
        VeriBlockBlock containingBlock = parseVeriBlockBlock(buffer);

        int contextCount = Utils.toInt(StreamUtils.getSingleByteLengthValue(buffer, 0, 4));

        if (contextCount < 0 || contextCount > Constants.MAX_CONTEXT_COUNT_ALT_PUBLICATION) {
            throw new IllegalArgumentException("Unexpected context count: " + contextCount
                    + " (expected a value between 0 and " + Constants.MAX_CONTEXT_COUNT_ALT_PUBLICATION + ")");
        }

        List<VeriBlockBlock> contextBlocks = new ArrayList<>(contextCount);
        for (int i = 0; i < contextCount; i++) {
            contextBlocks.add(SerializeDeserializeService.parseVeriBlockBlock(buffer));
        }

        return new AltPublication(transaction, merklePath, containingBlock, contextBlocks);
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
        long identifier = Utils.toLong(identifierBytes);
        byte[] headerBytes = StreamUtils.getVariableLengthValue(buffer, 0, Constants.MAX_HEADER_SIZE_PUBLICATION_DATA);
        byte[] contextInfoBytes = StreamUtils.getVariableLengthValue(buffer, 0, Constants.MAX_PAYOUT_SIZE_PUBLICATION_DATA);
        byte[] payoutInfoBytes = StreamUtils.getVariableLengthValue(buffer, 0, Constants.MAX_CONTEXT_SIZE_PUBLICATION_DATA);

        return new PublicationData(identifier, headerBytes, payoutInfoBytes, contextInfoBytes);
    }

// BitcoinTransaction
    public static byte[] serialize(BitcoinTransaction bitcoinTransaction) {
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
            serialize(bitcoinTransaction, stream);
            return stream.toByteArray();
        } catch (IOException ignore) {
            // Should not happen
        }
        return new byte[] {};
    }

    public static void serialize(BitcoinTransaction bitcoinTransaction, OutputStream stream) throws IOException {
        StreamUtils.writeVariableLengthValueToStream(stream, bitcoinTransaction.getRawBytes());
    }

    public static BitcoinTransaction parseBitcoinTransaction(ByteBuffer buffer) {
        byte[] raw = StreamUtils.getVariableLengthValue(buffer, 0, Constants.MAX_RAWTX_SIZE);
        return new BitcoinTransaction(raw);
    }
}
