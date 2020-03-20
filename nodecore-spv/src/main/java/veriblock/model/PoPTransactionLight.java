// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package veriblock.model;

import nodecore.api.grpc.VeriBlockMessages;
import org.veriblock.core.utilities.SerializerUtility;
import org.veriblock.sdk.models.BitcoinBlock;
import org.veriblock.sdk.models.BitcoinTransaction;
import org.veriblock.sdk.models.MerklePath;
import org.veriblock.sdk.models.Sha256Hash;
import org.veriblock.sdk.models.VeriBlockBlock;
import org.veriblock.sdk.services.SerializeDeserializeService;
import veriblock.conf.NetworkParameters;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class PoPTransactionLight extends StandardTransaction {

    private VeriBlockBlock endorsedBlock;
    private BitcoinTransaction bitcoinTx;
    private MerklePath bitcoinMerklePath;
    private BitcoinBlock blockOfProof;
    private final List<BitcoinBlock> blockOfProofContext = new ArrayList<>();

    public PoPTransactionLight(Sha256Hash txId) {
        super(txId);
    }

    @Override
    public TransactionTypeIdentifier getTransactionTypeIdentifier() {
        return TransactionTypeIdentifier.PROOF_OF_PROOF;
    }

    public VeriBlockBlock getEndorsedBlock() {
        return endorsedBlock;
    }
    public void setEndorsedBlock(VeriBlockBlock endorsedBlock) {
        this.endorsedBlock = endorsedBlock;
    }

    public BitcoinTransaction getBitcoinTx() {
        return bitcoinTx;
    }
    public void setBitcoinTx(BitcoinTransaction bitcoinTx) {
        this.bitcoinTx = bitcoinTx;
    }

    public MerklePath getBitcoinMerklePath() {
        return bitcoinMerklePath;
    }
    public void setBitcoinMerklePath(MerklePath bitcoinMerklePath) {
        this.bitcoinMerklePath = bitcoinMerklePath;
    }

    public BitcoinBlock getBlockOfProof() {
        return blockOfProof;
    }
    public void setBlockOfProof(BitcoinBlock blockOfProof) {
        this.blockOfProof = blockOfProof;
    }

    public List<BitcoinBlock> getContextBitcoinBlocks() {
        return this.blockOfProofContext;
    }
    public void addContextBitcoinBlocks(BitcoinBlock contextBitcoinBlock) {
        this.blockOfProofContext.add(contextBitcoinBlock);
    }

    @Override
    public byte[] toByteArray(NetworkParameters networkParameters) {
        return calculateHash();
    }

    @Override
    public VeriBlockMessages.SignedTransaction.Builder getSignedMessageBuilder(NetworkParameters networkParameters) {
        //TODO SPV-48
        return null;
    }

    private byte[] calculateHash() {
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
            serializeToStream(stream);

            return stream.toByteArray();
        } catch (IOException e) {
            // Should not happen
        }

        return null;
    }

    private void serializeToStream(OutputStream stream) throws IOException {
        stream.write(getTransactionTypeIdentifier().id());

        getInputAddress().serializeToStream(stream);
        SerializeDeserializeService.serialize(getEndorsedBlock(), stream);
        SerializeDeserializeService.serialize(getBitcoinTx(), stream);
        SerializeDeserializeService.serialize(getBitcoinMerklePath(), stream);
        SerializeDeserializeService.serialize(getBlockOfProof(), stream);

        SerializerUtility.writeVariableLengthValueToStream(stream, getContextBitcoinBlocks().size());
        for (BitcoinBlock block : getContextBitcoinBlocks()) {
            SerializeDeserializeService.serialize(block, stream);
        }
    }
}
