// VeriBlock PoP Miner
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class ScheduledPoPJob implements Job {
    private Runnable runnable;

    public ScheduledPoPJob(Runnable runnable) {
        this.runnable = runnable;
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        runnable.run();
    }
}
