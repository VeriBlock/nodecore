// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.sdk.blockchain;

import org.veriblock.sdk.conf.VeriBlockNetworkParameters;
import org.veriblock.sdk.models.VeriBlockBlock;
import org.veriblock.sdk.util.BitcoinUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.List;

public strictfp class VeriBlockDifficultyCalculator {
    public static final int RETARGET_PERIOD = 100; // In blocks

    private static final int T = 30; // 30 seconds
    private static final int N = RETARGET_PERIOD;
    private static final BigInteger K = BigInteger.valueOf(N)
            .multiply(BigInteger.valueOf(N).add(BigInteger.valueOf(-1)))
            .multiply(BigInteger.valueOf(T))
            .divide(BigInteger.valueOf(2));

    public static BigInteger calculate(VeriBlockNetworkParameters networkParameters,
                                       VeriBlockBlock lastBlock, List<VeriBlockBlock> context) {
        if (lastBlock.getHeight() < N || networkParameters.getPowNoRetargeting()) {
            return BitcoinUtils.decodeCompactBits(lastBlock.getDifficulty());
        }

        BigDecimal sumTarget = BigDecimal.valueOf(0);

        long t = 0;
        long j = 0;

        if (context.size() > N) {
            context = context.subList(0, N);
        }

        for (int i = context.size() - 1; i > 0; i--) {
            int solveTime = context.get(i - 1).getTimestamp() - context.get(i).getTimestamp();
            if (solveTime > (T * 6)) {
                solveTime = T * 6;
            } else if (solveTime < -6 * T) {
                solveTime = -6 * T;
            }

            j++;
            t += solveTime * j;
            sumTarget = sumTarget.add(new BigDecimal(BitcoinUtils.decodeCompactBits(context.get(i).getDifficulty())));
        }

        sumTarget = sumTarget.divide(BigDecimal.valueOf(N - 1), 8, RoundingMode.HALF_UP);

        if (t < K.divide(BigInteger.valueOf(10)).intValue()) {
            t = K.divide(BigInteger.valueOf(10)).intValue();
        }

        BigInteger nextTarget = sumTarget.multiply(new BigDecimal(K).divide(BigDecimal.valueOf(t), 8, RoundingMode.HALF_UP)).toBigInteger();

        if (nextTarget.compareTo(networkParameters.getMinimumDifficulty()) < 0) {
            return networkParameters.getMinimumDifficulty();
        }

        return nextTarget;
    }
}
