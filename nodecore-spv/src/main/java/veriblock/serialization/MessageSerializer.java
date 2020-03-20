// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package veriblock.serialization;

import com.google.protobuf.InvalidProtocolBufferException;
import nodecore.api.grpc.VeriBlockMessages;
import nodecore.api.grpc.utilities.ByteStringAddressUtility;
import nodecore.api.grpc.utilities.ByteStringUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.veriblock.sdk.models.BitcoinTransaction;
import org.veriblock.sdk.models.Coin;
import org.veriblock.sdk.models.MerklePath;
import org.veriblock.sdk.models.Sha256Hash;
import org.veriblock.sdk.models.VBlakeHash;
import org.veriblock.sdk.models.VeriBlockBlock;
import org.veriblock.sdk.services.SerializeDeserializeService;
import veriblock.model.AddressFactory;
import veriblock.model.BlockMetaPackage;
import veriblock.model.FullBlock;
import veriblock.model.MultisigTransaction;
import veriblock.model.OutputFactory;
import veriblock.model.PoPTransactionLight;
import veriblock.model.StandardTransaction;

import java.util.stream.Collectors;

public class MessageSerializer {
    private static final Logger logger = LoggerFactory.getLogger(MessageSerializer.class);

    public static VeriBlockBlock deserialize(VeriBlockMessages.BlockHeader blockHeaderMessage) {
        return SerializeDeserializeService.parseVeriBlockBlock(blockHeaderMessage.getHeader().toByteArray());
    }

    public static StandardTransaction deserializeNormalTransaction(VeriBlockMessages.TransactionUnion transactionUnionMessage) {
        switch (transactionUnionMessage.getTransactionCase()) {
            case SIGNED:
                return deserializeStandardTransaction(transactionUnionMessage.getSigned());
            case SIGNED_MULTISIG:
                return deserializeMultisigTransaction(transactionUnionMessage.getSignedMultisig());
            default:
                // Should be impossible
                return null;
        }
    }

    public static PoPTransactionLight deserializePoPTransaction(VeriBlockMessages.TransactionUnion transactionUnionMessage) {
        VeriBlockMessages.SignedTransaction signed = transactionUnionMessage.getSigned();
        VeriBlockMessages.Transaction txMessage = signed.getTransaction();

        PoPTransactionLight tx = new PoPTransactionLight(Sha256Hash.wrap(txMessage.getTxId().toByteArray()));
        tx.setInputAddress(AddressFactory.create(ByteStringAddressUtility.parseProperAddressTypeAutomatically(txMessage.getSourceAddress())));
        tx.setEndorsedBlock(SerializeDeserializeService.parseVeriBlockBlock(txMessage.getEndorsedBlockHeader().toByteArray()));
        tx.setBitcoinTx(new BitcoinTransaction(txMessage.getBitcoinTransaction().toByteArray()));
        tx.setBitcoinMerklePath(new MerklePath(txMessage.getMerklePath()));
        tx.setBlockOfProof(SerializeDeserializeService.parseBitcoinBlock(txMessage.getBitcoinBlockHeaderOfProof().getHeader().toByteArray()));
        txMessage.getContextBitcoinBlockHeadersList().stream()
                .map(header -> SerializeDeserializeService.parseBitcoinBlock(header.getHeader().toByteArray()))
                .forEach(tx::addContextBitcoinBlocks);
        return tx;
    }

    public static FullBlock deserialize(VeriBlockMessages.Block blockMessage) {
        FullBlock block = new FullBlock(
                blockMessage.getNumber(),
                (short)blockMessage.getVersion(),
                VBlakeHash.wrap(ByteStringUtility.byteStringToHex(blockMessage.getPreviousHash())),
                VBlakeHash.wrap(ByteStringUtility.byteStringToHex(blockMessage.getSecondPreviousHash())),
                VBlakeHash.wrap(ByteStringUtility.byteStringToHex(blockMessage.getThirdPreviousHash())),
                Sha256Hash.wrap(ByteStringUtility.byteStringToHex(blockMessage.getMerkleRoot())),
                blockMessage.getTimestamp(),
                blockMessage.getEncodedDifficulty(),
                blockMessage.getWinningNonce());

        block.setNormalTransactions(blockMessage.getRegularTransactionsList().stream()
                .map(MessageSerializer::deserializeNormalTransaction)
                .collect(Collectors.toList()));
        block.setPoPTransactions(blockMessage.getPopTransactionsList().stream()
                .map(MessageSerializer::deserializePoPTransaction)
                .collect(Collectors.toList()));
        block.setMetaPackage(new BlockMetaPackage(Sha256Hash.wrap(blockMessage.getBlockContentMetapackage().getHash().toByteArray())));

        return block;
    }

    public static StandardTransaction deserializeStandardTransaction(VeriBlockMessages.SignedTransaction signedTransaction) {
        VeriBlockMessages.Transaction txMessage = signedTransaction.getTransaction();
        StandardTransaction tx = new StandardTransaction(Sha256Hash.wrap(txMessage.getTxId().toByteArray()));
        tx.setInputAddress(AddressFactory.create(ByteStringAddressUtility.parseProperAddressTypeAutomatically(txMessage.getSourceAddress())));
        tx.setInputAmount(Coin.valueOf(txMessage.getSourceAmount()));
        txMessage.getOutputsList().stream()
                .map(o -> OutputFactory.create(ByteStringAddressUtility.parseProperAddressTypeAutomatically(o.getAddress()),
                        o.getAmount()))
                .forEach(tx::addOutput);
        tx.setSignatureIndex(signedTransaction.getSignatureIndex());
        tx.setData(txMessage.getData().toByteArray());
        return tx;
    }

    public static StandardTransaction deserializeMultisigTransaction(VeriBlockMessages.SignedMultisigTransaction signedTransaction) {
        VeriBlockMessages.Transaction txMessage = signedTransaction.getTransaction();
        StandardTransaction tx = new MultisigTransaction(Sha256Hash.wrap(txMessage.getTxId().toByteArray()));
        tx.setInputAddress(AddressFactory.create(ByteStringAddressUtility.parseProperAddressTypeAutomatically(txMessage.getSourceAddress())));
        tx.setInputAmount(Coin.valueOf(txMessage.getSourceAmount()));
        txMessage.getOutputsList().stream()
                .map(o -> OutputFactory.create(ByteStringAddressUtility.parseProperAddressTypeAutomatically(o.getAddress()),
                        o.getAmount()))
                .forEach(tx::addOutput);
        tx.setSignatureIndex(signedTransaction.getSignatureIndex());
        tx.setData(txMessage.getData().toByteArray());
        return tx;
    }

    public static VeriBlockMessages.Event deserialize(byte[] raw) {
        try {
            return VeriBlockMessages.Event.parseFrom(raw);
        } catch (InvalidProtocolBufferException e) {
            logger.error("Unable to parse message", e);
        }

        return null;
    }
}
