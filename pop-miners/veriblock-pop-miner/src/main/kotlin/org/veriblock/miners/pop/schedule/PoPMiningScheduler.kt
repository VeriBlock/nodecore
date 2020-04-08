// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.miners.pop.schedule

import org.bitcoinj.core.Context
import org.quartz.CronScheduleBuilder
import org.quartz.CronTrigger
import org.quartz.JobBuilder
import org.quartz.ScheduleBuilder
import org.quartz.Scheduler
import org.quartz.SchedulerException
import org.quartz.SchedulerFactory
import org.quartz.Trigger
import org.quartz.TriggerBuilder
import org.quartz.impl.StdSchedulerFactory
import org.quartz.spi.JobFactory
import org.veriblock.core.utilities.createLogger
import org.veriblock.miners.pop.VpmConfig
import org.veriblock.miners.pop.service.MinerService
import java.text.SimpleDateFormat

private val logger = createLogger {}

class PoPMiningScheduler(
    private val config: VpmConfig,
    private val popMinerService: MinerService
) {
    private val scheduler: Scheduler?
    private var runnable = true
    private var scheduleBuilder: ScheduleBuilder<CronTrigger>? = null

    init {
        try {
            if (!config.cronSchedule.isBlank()) {
                scheduleBuilder = CronScheduleBuilder.cronSchedule(config.cronSchedule)
            } else {
                runnable = false
            }
        } catch (e: RuntimeException) {
            logger.info("Invalid cron expression, scheduler will not start")
            runnable = false
        }
        val factory: SchedulerFactory = StdSchedulerFactory()
        var scheduler: Scheduler? = null
        try {
            scheduler = factory.scheduler
            val executeSchedule = Runnable { executeSchedule() }
            scheduler.setJobFactory(JobFactory { _, _ -> ScheduledPoPJob(executeSchedule) })
        } catch (e: SchedulerException) {
            logger.error(e.message, e)
            runnable = false
        }
        this.scheduler = scheduler
    }

    @Throws(InterruptedException::class)
    fun shutdown() {
        try {
            if (scheduler != null && !scheduler.isShutdown) {
                scheduler.shutdown()
            }
        } catch (e: SchedulerException) {
            logger.error(e.message, e)
        }
    }

    fun run() {
        if (runnable && scheduler != null) {
            try {
                scheduler.start()
                val job = JobBuilder.newJob(ScheduledPoPJob::class.java).withIdentity("scheduledPoP").build()
                val trigger: Trigger = TriggerBuilder.newTrigger().withIdentity("propertiesSchedule").withSchedule(
                    scheduleBuilder
                ).build()
                scheduler.scheduleJob(job, trigger)
                val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                val info = String.format(
                    "Found cron schedule '${config.cronSchedule}', first trigger at %s",
                    dateFormatter.format(trigger.nextFireTime)
                )
                logger.info(info)
            } catch (e: SchedulerException) {
                logger.error(e.message, e)
            }
        }
    }

    private fun executeSchedule() {
        Context.propagate(config.bitcoin.context)
        if (popMinerService.isReady()) {
            logger.info("Starting mining operation as scheduled")
            popMinerService.mine(null)
        } else {
            logger.info("PoP miner is not in ready state, skipping scheduled mining operation")
        }
    }
}
