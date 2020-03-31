// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.miners.pop.schedule

import org.quartz.Job
import org.quartz.JobExecutionContext

class ScheduledPoPJob(
    private val runnable: Runnable
) : Job {
    override fun execute(context: JobExecutionContext) {
        runnable.run()
    }
}
