// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop;

import nodecore.miners.pop.events.InfoMessageEvent;
import org.apache.commons.lang3.StringUtils;
import org.bitcoinj.core.Context;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.ScheduleBuilder;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;

public class PoPMiningScheduler {
    private static final Logger logger = LoggerFactory.getLogger(PoPMiningScheduler.class);

    private final Configuration configuration;
    private final PoPMiner popMiner;
    private final Context context;
    private final Scheduler scheduler;

    private boolean runnable = true;
    private ScheduleBuilder<CronTrigger> scheduleBuilder;

    public PoPMiningScheduler(Configuration configuration, PoPMiner popMiner, Context context) {
        this.configuration = configuration;
        this.popMiner = popMiner;
        this.context = context;

        try {
            String schedule = configuration.getCronSchedule();
            if (StringUtils.isNotBlank(schedule)) {
                this.scheduleBuilder = CronScheduleBuilder.cronSchedule(schedule);
            } else {
                runnable = false;
            }
        } catch (RuntimeException e) {
            logger.info("Invalid cron expression, scheduler will not start");
            runnable = false;
        }

        SchedulerFactory factory = new StdSchedulerFactory();
        Scheduler scheduler = null;
        try {
            scheduler = factory.getScheduler();
            Runnable executeSchedule = this::executeSchedule;
            scheduler.setJobFactory(((bundle, scheduler1) -> new ScheduledPoPJob(executeSchedule)));
        } catch (SchedulerException e) {
            logger.error(e.getMessage(), e);
            runnable = false;
        }

        this.scheduler = scheduler;
    }

    public void shutdown() throws InterruptedException {
        try {
            if (scheduler != null && !scheduler.isShutdown()) {
                scheduler.shutdown();
            }
        } catch (SchedulerException e) {
            logger.error(e.getMessage(), e);
        }
    }

    public void run() {
        if (runnable && scheduler != null) {
            try {
                scheduler.start();

                JobDetail job = JobBuilder.newJob(ScheduledPoPJob.class).withIdentity("scheduledPoP").build();

                Trigger trigger = TriggerBuilder.newTrigger().withIdentity("propertiesSchedule").withSchedule(scheduleBuilder).build();

                scheduler.scheduleJob(job, trigger);

                SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String info = String.format("Found cron schedule '" + configuration.getCronSchedule() + "', first trigger at %s",
                        dateFormatter.format(trigger.getNextFireTime()));
                InternalEventBus.getInstance().post(new InfoMessageEvent(info));
            } catch (SchedulerException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    private void executeSchedule() {
        Context.propagate(context);

        if (popMiner.isReady()) {
            logger.info("Starting mining operation as scheduled");
            popMiner.mine(null);
        } else {
            logger.info("PoP miner is not in ready state, skipping scheduled mining operation");
        }
    }
}
