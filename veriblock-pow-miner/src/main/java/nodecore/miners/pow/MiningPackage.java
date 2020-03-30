// VeriBlock PoW CPU Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pow;

import nodecore.api.ucp.commands.client.MiningJob;

import java.math.BigInteger;

class MiningPackage {
    private final int blockHeight;
    private final short version;
    private final String previousBlockHash;
    private final String secondPreviousBlockHash;
    private final String thirdPreviousBlockHash;
    private final String merkleRoot;
    private final int lowestTimestamp;
    private final int blockchainDifficulty;
    private final BigInteger poolTarget;
    private final int jobId;

    MiningPackage(
            int blockHeight,
            short version,
            String previousBlockHash,
            String secondPreviousBlockHash,
            String thirdPreviousBlockHash,
            String merkleRoot,
            int lowestTimestamp,
            int blockchainDifficulty,
            BigInteger poolTarget,
            int jobId) {
        this.blockHeight = blockHeight;
        this.version = version;
        this.previousBlockHash = previousBlockHash;
        this.secondPreviousBlockHash = secondPreviousBlockHash;
        this.thirdPreviousBlockHash = thirdPreviousBlockHash;
        this.merkleRoot = merkleRoot;
        this.lowestTimestamp = lowestTimestamp;
        this.blockchainDifficulty = blockchainDifficulty;
        this.poolTarget = poolTarget;
        this.jobId = jobId;
    }

    MiningPackage(MiningJob miningJob) {
        this.blockHeight = miningJob.getBlockIndex();
        this.version = (short)miningJob.getBlockVersion();
        this.previousBlockHash = miningJob.getPreviousBlockHash();
        this.secondPreviousBlockHash = miningJob.getSecondPreviousBlockHash();
        this.thirdPreviousBlockHash = miningJob.getThirdPreviousBlockHash();
        this.merkleRoot = miningJob.getMerkleRoot();
        this.lowestTimestamp = miningJob.getTimestamp();
        this.blockchainDifficulty = miningJob.getDifficulty();
        this.poolTarget = new BigInteger(miningJob.getMiningTarget(), 16);
        this.jobId = miningJob.getJobId();
    }

    int getBlockHeight() {
        return blockHeight;
    }

    short getVersion() {
        return version;
    }

    String getPreviousBlockHash() {
        return previousBlockHash;
    }

    String getSecondPreviousBlockHash() {
        return secondPreviousBlockHash;
    }

    String getThirdPreviousBlockHash() {
        return thirdPreviousBlockHash;
    }

    String getMerkleRoot() {
        return merkleRoot;
    }

    int getLowestTimestamp() {
        return lowestTimestamp;
    }

    int getBlockchainDifficulty() {
        return blockchainDifficulty;
    }

    BigInteger getPoolTarget() {
        return poolTarget;
    }

    int getJobId() {
        return jobId;
    }
}
