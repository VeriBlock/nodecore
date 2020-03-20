// VeriBlock PoW CPU Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pow;

import org.veriblock.core.crypto.Crypto;
import org.veriblock.core.utilities.BlockUtility;
import org.veriblock.core.utilities.Utility;

import java.math.BigInteger;
import java.time.Instant;

public class MinerThread extends Thread {
    private static final int NUM_HASHES_PER_CYCLE = 100_000;

    private MiningPackage miningPackage;
    private int localTimeOffset = 0;

    private final Object lock;
    private final int threadNum;
    private final int incrementOffset;
    private final long extraNonce;

    private final ShareRepo shareRepo;

    private double speed;

    private boolean shouldContinue = true;

    MinerThread(MiningPackage miningPackage, int threadNum, int incrementOffset, long extraNonce, ShareRepo shareRepo) {
        this.miningPackage = miningPackage;
        this.threadNum = threadNum;
        this.incrementOffset = incrementOffset;
        this.shareRepo = shareRepo;
        this.extraNonce = extraNonce;
        lock = new Object();
    }

    public void run() {
        this.localTimeOffset = (int)Instant.now().getEpochSecond() - miningPackage.getLowestTimestamp();
        int workingTimestamp;
        int nonce = threadNum;

        Crypto c = new Crypto();

        long lastUpdate = System.currentTimeMillis();
        int lastUpdateNonce = nonce;

        System.out.println("Starting thread " + threadNum + "...");

        while (shouldContinue) {
            synchronized (lock) {
                workingTimestamp = (int)Instant.now().getEpochSecond();// + localTimeOffset;

                if (nonce < 0 || nonce + NUM_HASHES_PER_CYCLE * incrementOffset < 0) {
                    nonce = threadNum;
                }

                for (int i = 0; i < NUM_HASHES_PER_CYCLE; i++) {
                    byte[] header = BlockUtility.assembleBlockHeader(
                            miningPackage.getBlockHeight(),
                            miningPackage.getVersion(),
                            miningPackage.getPreviousBlockHash(),
                            miningPackage.getSecondPreviousBlockHash(),
                            miningPackage.getThirdPreviousBlockHash(),
                            miningPackage.getMerkleRoot(),
                            workingTimestamp,
                            miningPackage.getBlockchainDifficulty(),
                            nonce);

                    String blockHash = c.vBlakeReturnHex(header);

                    BigInteger blockHashNumber = new BigInteger(blockHash, 16);

                    if (blockHashNumber.compareTo(miningPackage.getPoolTarget()) < 0) {
                        shareRepo.addShare(new FoundSharePackage(
                                miningPackage.getJobId(),
                                workingTimestamp,
                                nonce,
                                extraNonce,
                                blockHash,
                                miningPackage.getPreviousBlockHash()));

                        System.out.println("Share on thread " + threadNum + " on block " + Utility.zeroPad(miningPackage.getPreviousBlockHash(), 48));
                    }

                    nonce += incrementOffset;
                }

                if (nonce > lastUpdateNonce) {
                    double difference = (nonce - lastUpdateNonce) / incrementOffset;
                    long timeDiff = System.currentTimeMillis() - lastUpdate;
                    lastUpdate = System.currentTimeMillis();
                    lastUpdateNonce = nonce;
                    this.speed = (difference / timeDiff) * 1000; // hashes per second

                } else {
                    // Account for resets
                    lastUpdateNonce = nonce;
                }
            }
        }
    }

    public void shutdown() {
        this.shouldContinue = false;
    }

    double getSpeed() {
        return speed;
    }

    void updateMiningPackage(MiningPackage updatedPackage) {
        this.miningPackage = updatedPackage;
        this.localTimeOffset = (int)Instant.now().getEpochSecond() - this.miningPackage.getLowestTimestamp();
    }
}
