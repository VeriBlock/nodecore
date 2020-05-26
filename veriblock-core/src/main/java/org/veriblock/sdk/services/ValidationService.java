// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.sdk.services;

import org.veriblock.core.bitcoinj.BitcoinUtilities;
import org.veriblock.core.utilities.Utility;
import org.veriblock.sdk.models.BitcoinBlock;
import org.veriblock.sdk.models.Constants;
import org.veriblock.sdk.models.VeriBlockBlock;
import org.veriblock.sdk.models.VerificationException;

import java.math.BigInteger;
import java.util.Locale;

public class ValidationService {

// VeriBlockBlock

    public static void verify(VeriBlockBlock veriBlockBlock) throws VerificationException {
        checkProofOfWork(veriBlockBlock);
        checkMaximumDrift(veriBlockBlock);
    }

    public static BigInteger getEmbeddedTarget(VeriBlockBlock block) {
        BigInteger embeddedDifficulty = BitcoinUtilities.decodeCompactBits(block.getDifficulty());
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
        int currentTime = Utility.getCurrentTimestamp();
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
        BigInteger embeddedTarget = BitcoinUtilities.decodeCompactBits(block.getDifficulty());
        BigInteger hash = block.getHash().toBigInteger();

        return hash.compareTo(embeddedTarget) <= 0;
    }

    public static void checkProofOfWork(BitcoinBlock block) throws VerificationException {
        if (!isProofOfWorkValid(block)) {
            BigInteger embeddedTarget = BitcoinUtilities.decodeCompactBits(block.getDifficulty());

            throw new VerificationException(
                    String.format(Locale.US, "Block hash is higher than target: %s vs %s",
                            block.getHash().toString(),
                            embeddedTarget.toString(16)));
        }
    }

    public static void checkMaximumDrift(BitcoinBlock bitcoinBlock) throws VerificationException {
        int currentTime = Utility.getCurrentTimestamp();
        if (bitcoinBlock.getTimestamp() > currentTime + Constants.ALLOWED_TIME_DRIFT) {
            throw new VerificationException("Block is too far in the future");
        }
    }
}
