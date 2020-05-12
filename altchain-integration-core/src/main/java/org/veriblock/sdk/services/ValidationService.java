// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.sdk.services;

import org.veriblock.sdk.models.AltPublication;
import org.veriblock.sdk.models.BitcoinBlock;
import org.veriblock.sdk.models.Constants;
import org.veriblock.sdk.models.Sha256Hash;
import org.veriblock.sdk.models.VBlakeHash;
import org.veriblock.sdk.models.VeriBlockBlock;
import org.veriblock.sdk.models.VeriBlockPoPTransaction;
import org.veriblock.sdk.models.VeriBlockPublication;
import org.veriblock.sdk.models.VeriBlockTransaction;
import org.veriblock.sdk.models.VerificationException;
import org.veriblock.sdk.util.BitcoinUtils;
import org.veriblock.sdk.util.MerklePathUtil;
import org.veriblock.sdk.util.Utils;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Locale;

public class ValidationService {

    public static void verify(VeriBlockPoPTransaction veriBlockPoPTransaction) throws VerificationException {
        checkSignature(veriBlockPoPTransaction);
        checkBitcoinTransactionForPoPData(veriBlockPoPTransaction);
        checkBitcoinMerklePath(veriBlockPoPTransaction);
        checkBitcoinBlocks(veriBlockPoPTransaction);
    }

    public static void checkSignature(VeriBlockPoPTransaction tx) throws VerificationException {
        if (!tx.getAddress().isDerivedFromPublicKey(tx.getPublicKey())) {
            throw new VerificationException("VeriBlock PoP Transaction contains an invalid public key");
        }

        if (!Utils.verifySignature(SerializeDeserializeService.getHash(tx).getBytes(), tx.getSignature(), tx.getPublicKey())) {
            throw new VerificationException("VeriBlock PoP Transaction is incorrectly signed");
        }
    }

    public static void checkBitcoinTransactionForPoPData(VeriBlockPoPTransaction tx) throws VerificationException {
        ByteBuffer buffer = ByteBuffer.allocateDirect(80);
        buffer.put(SerializeDeserializeService.serializeHeaders(tx.getPublishedBlock()));
        buffer.put(tx.getAddress().getPoPBytes());
        buffer.flip();

        byte[] publicationData = new byte[80];
        buffer.get(publicationData);

        if (!tx.getBitcoinTransaction().contains(publicationData)) {
            throw new VerificationException("Bitcoin transaction does not contain PoP publication data");
        }
    }

    public static void checkBitcoinMerklePath(VeriBlockPoPTransaction tx) throws VerificationException {
        if (!tx.getMerklePath().getSubject().equals(Sha256Hash.twiceOf(tx.getBitcoinTransaction().getRawBytes()))) {
            throw new VerificationException("Bitcoin transaction cannot be proven by merkle path");
        }

        if (!MerklePathUtil.calculateMerkleRoot(tx.getMerklePath()).equals(tx.getBlockOfProof().getMerkleRootReversed())) {
            throw new VerificationException("Bitcoin transaction does not belong to block of proof");
        }
    }

    public static void checkBitcoinBlocks(VeriBlockPoPTransaction tx) throws VerificationException {
        Sha256Hash lastHash = null;
        for (BitcoinBlock block : tx.getBlocks()) {
            ValidationService.verify(block);

            if (lastHash != null) {
                // Check that it's the next height and affirms the previous hash
                if (!block.getPreviousBlock().equals(lastHash)) {
                    throw new VerificationException("Blocks are not contiguous");
                }
            }
            lastHash = block.getHash();
        }
    }


    // VeriBlockPublication

    public static void verify(VeriBlockPublication veriBlockPublication) throws VerificationException {
        ValidationService.verify(veriBlockPublication.getTransaction());
        checkMerklePath(veriBlockPublication);
        checkBlocks(veriBlockPublication);
    }


    public static void checkBlocks(VeriBlockPublication veriBlockPublication) throws VerificationException {
        Integer lastHeight = null;
        VBlakeHash lastHash = null;
        for (VeriBlockBlock block : veriBlockPublication.getBlocks()) {
            ValidationService.verify(block);

            if (lastHeight != null && lastHash != null) {
                // Check that it's the next height and affirms the previous hash
                if (block.getHeight() != lastHeight + 1 ||
                        !block.getPreviousBlock().equals(lastHash.trimToPreviousBlockSize())) {
                    throw new VerificationException("Blocks are not contiguous");
                }
            }
            lastHeight = block.getHeight();
            lastHash = block.getHash();
        }
    }

    public static void checkMerklePath(VeriBlockPublication veriBlockPublication) throws VerificationException {
        if (!veriBlockPublication.getMerklePath().getSubject().equals(SerializeDeserializeService.getId(veriBlockPublication.getTransaction()))) {
            throw new VerificationException("VeriBlock PoP Transaction cannot be proven by merkle path");
        }

        if (!MerklePathUtil.calculateVeriMerkleRoot(veriBlockPublication.getMerklePath()).trim(Sha256Hash.VERIBLOCK_MERKLE_ROOT_LENGTH).equals(veriBlockPublication.getContainingBlock().getMerkleRoot())) {
            throw new VerificationException("VeriBlock PoP transaction does not belong to containing block");
        }
    }

    // VeriBlockTransaction

    public static void verify(VeriBlockTransaction veriBlockTransaction) {
        checkSignature(veriBlockTransaction);
    }

    public static void checkSignature(VeriBlockTransaction veriBlockTransaction) throws VerificationException {
        if (!veriBlockTransaction.getSourceAddress().isDerivedFromPublicKey(veriBlockTransaction.getPublicKey())) {
            throw new VerificationException("VeriBlock transaction contains an invalid public key");
        }

        if (!Utils.verifySignature(SerializeDeserializeService.getId(veriBlockTransaction).getBytes(), veriBlockTransaction.getSignature(), veriBlockTransaction.getPublicKey())) {
            throw new VerificationException("VeriBlock transaction is incorrectly signed");
        }
    }

// VeriBlockBlock

    public static void verify(VeriBlockBlock veriBlockBlock) throws VerificationException {
        checkProofOfWork(veriBlockBlock);
        checkMaximumDrift(veriBlockBlock);
    }

    public static BigInteger getEmbeddedTarget(VeriBlockBlock block) {
        BigInteger embeddedDifficulty = BitcoinUtils.decodeCompactBits(block.getDifficulty());
        return Constants.MAXIMUM_DIFFICULTY.divide(embeddedDifficulty);
    }

    public static boolean isProofOfWorkValid(VeriBlockBlock block) {
        BigInteger hash = block.getHash().toBigInteger();

        return hash.compareTo(getEmbeddedTarget(block)) <= 0;
    }

    public static void checkProofOfWork(VeriBlockBlock block) {
        if (!isProofOfWorkValid(block)) {
            throw new VerificationException(
                    String.format(Locale.US, "Block hash is higher than target: %s vs %s",
                            block.getHash().toString(),
                            getEmbeddedTarget(block).toString(16)));
        }
    }

    public static void checkMaximumDrift(VeriBlockBlock veriBlockBlock) {
        int currentTime = Utils.getCurrentTimestamp();
        if (veriBlockBlock.getTimestamp() > currentTime + Constants.ALLOWED_TIME_DRIFT) {
            throw new VerificationException("Block is too far in the future");
        }
    }

// BitcoinBlock

    public static void verify(BitcoinBlock bitcoinBlock) throws VerificationException {
        checkProofOfWork(bitcoinBlock);
        checkMaximumDrift(bitcoinBlock);
    }

    public static boolean isProofOfWorkValid(BitcoinBlock block) throws VerificationException {
        BigInteger embeddedTarget = BitcoinUtils.decodeCompactBits(block.getBits());
        BigInteger hash = block.getHash().toBigInteger();

        return hash.compareTo(embeddedTarget) <= 0;
    }

    public static void checkProofOfWork(BitcoinBlock block) throws VerificationException {
        if (!isProofOfWorkValid(block)) {
            BigInteger embeddedTarget = BitcoinUtils.decodeCompactBits(block.getBits());

            throw new VerificationException(
                    String.format(Locale.US, "Block hash is higher than target: %s vs %s",
                            block.getHash().toString(),
                            embeddedTarget.toString(16)));
        }
    }

    public static void checkMaximumDrift(BitcoinBlock bitcoinBlock) throws VerificationException {
        int currentTime = Utils.getCurrentTimestamp();
        if (bitcoinBlock.getTimestamp() > currentTime + Constants.ALLOWED_TIME_DRIFT) {
            throw new VerificationException("Block is too far in the future");
        }
    }

// AltPublication

    public static void verify(AltPublication altPublication) throws VerificationException {
        ValidationService.verify(altPublication.getTransaction());
        checkMerklePath(altPublication);
        checkBlocks(altPublication);
    }

    public static void checkMerklePath(AltPublication altPublication) throws VerificationException {
        if (!altPublication.getMerklePath().getSubject().equals(SerializeDeserializeService.getId(altPublication.getTransaction()))) {
            throw new VerificationException("VeriBlock transaction cannot be proven by merkle path");
        }

        if (!MerklePathUtil.calculateVeriMerkleRoot(altPublication.getMerklePath()).trim(Sha256Hash.VERIBLOCK_MERKLE_ROOT_LENGTH).equals(altPublication.getContainingBlock().getMerkleRoot())) {
            throw new VerificationException("VeriBlock transaction does not belong to containing block");
        }
    }

    public static void checkBlocks(AltPublication altPublication) throws VerificationException {
        Integer lastHeight = null;
        VBlakeHash lastHash = null;
        for (VeriBlockBlock block : altPublication.getBlocks()) {
            ValidationService.verify(block);

            if (lastHeight != null && lastHash != null) {
                // Check that it's the next height and affirms the previous hash
                if (block.getHeight() != lastHeight + 1 ||
                        !block.getPreviousBlock().equals(lastHash.trimToPreviousBlockSize())) {
                    throw new VerificationException("Blocks are not contiguous");
                }
            }
            lastHeight = block.getHeight();
            lastHash = block.getHash();
        }
    }
}
