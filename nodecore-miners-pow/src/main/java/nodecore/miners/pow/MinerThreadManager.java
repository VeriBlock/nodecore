// VeriBlock PoW CPU Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pow;

import nodecore.api.ucp.commands.UCPClientCommand;
import nodecore.api.ucp.commands.client.MiningJob;
import nodecore.api.ucp.commands.client.MiningMempoolUpdate;
import org.veriblock.core.utilities.Utility;

import java.util.LinkedList;

public class MinerThreadManager extends Thread {
    private MiningPackage miningPackage;
    private MiningPackage pendingMiningPackage;
    private MinerThread[] miners;
    private final Object lock = new Object();

    boolean shouldContinue = true;

    private final LinkedList<UCPClientCommand> queue = new LinkedList<>();

    MinerThreadManager(
            int numThreads,
            MiningJob miningJob,
            ShareRepo shareRepo) {
        this.miningPackage = new MiningPackage(miningJob);

        miners = new MinerThread[numThreads];

        for (int threadCount = 0; threadCount < miners.length; threadCount++) {
            miners[threadCount] = new MinerThread(
                    miningPackage,
                    threadCount,
                    numThreads,
                    miningJob.getExtraNonceStart(),
                    shareRepo);
            miners[threadCount].start();
        }
    }

    public void run() {
        while (shouldContinue) {
            synchronized (lock) {
                UCPClientCommand command;
                while ((command = queue.poll()) != null) {
                    handleCommand(command);

                    for (MinerThread miner : miners) {
                        miner.updateMiningPackage(pendingMiningPackage);
                    }
                    this.miningPackage = pendingMiningPackage;
                }
            }
            Utility.sleep(25);
        }

        for (int i = 0; i < miners.length; i++) {
            miners[i].shutdown();
        }
    }

    public void shutdown() {
        this.shouldContinue = false;
    }

    double getHashrate() {
        double totalHashrate = 0.0d;
        for (MinerThread miner : miners) {
            totalHashrate += miner.getSpeed();
        }

        return totalHashrate;
    }

    void update(UCPClientCommand command) {
        synchronized (lock) {
            queue.add(command);
        }
    }

    private void handleCommand(UCPClientCommand command) {
        if (command instanceof MiningJob) {
            update((MiningJob)command);
        } else if (command instanceof MiningMempoolUpdate) {
            update((MiningMempoolUpdate) command);
        }
    }

    private void update(MiningJob newMiningJob) {
        this.pendingMiningPackage = new MiningPackage(newMiningJob);
    }

    private void update(MiningMempoolUpdate mempoolUpdate) {
        this.pendingMiningPackage = new MiningPackage(
                miningPackage.getBlockHeight(),
                miningPackage.getVersion(),
                miningPackage.getPreviousBlockHash(),
                miningPackage.getSecondPreviousBlockHash(),
                miningPackage.getThirdPreviousBlockHash(),
                mempoolUpdate.getNewMerkleRoot(),
                miningPackage.getLowestTimestamp(),
                miningPackage.getBlockchainDifficulty(),
                miningPackage.getPoolTarget(),
                mempoolUpdate.getJobId());
    }
}
