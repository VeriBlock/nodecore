// VeriBlock PoP Miner
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.miners.pop.schedule

import nodecore.miners.pop.Configuration
import nodecore.miners.pop.PoPMiner
import org.apache.commons.lang3.StringUtils
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
import java.text.SimpleDateFormat

private val logger = createLogger {}

class PoPMiningScheduler(
    private val configuration: Configuration,
    private val popMiner: PoPMiner,
    private val context: Context
) {
    private val scheduler: Scheduler?
    private var runnable = true
    private var scheduleBuilder: ScheduleBuilder<CronTrigger>? = null

    init {
        try {
            val schedule = configuration.cronSchedule
            if (StringUtils.isNotBlank(schedule)) {
                scheduleBuilder = CronScheduleBuilder.cronSchedule(schedule)
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
                    "Found cron schedule '" + configuration.cronSchedule + "', first trigger at %s",
                    dateFormatter.format(trigger.nextFireTime)
                )
                logger.info(info)
            } catch (e: SchedulerException) {
                logger.error(e.message, e)
            }
        }
    }

    private fun executeSchedule() {
        Context.propagate(context)
        if (popMiner.isReady()) {
            logger.info("Starting mining operation as scheduled")
            popMiner.mine(null)
        } else {
            logger.info("PoP miner is not in ready state, skipping scheduled mining operation")
        }
    }
}
