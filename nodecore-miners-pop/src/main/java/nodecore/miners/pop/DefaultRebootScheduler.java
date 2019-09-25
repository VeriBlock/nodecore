// VeriBlock PoP Miner
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop;

import java.text.SimpleDateFormat;

import org.apache.commons.lang3.StringUtils;
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

import com.google.inject.Inject;

import nodecore.miners.pop.contracts.Configuration;
import nodecore.miners.pop.contracts.RebootScheduler;
import nodecore.miners.pop.events.InfoMessageEvent;
import nodecore.miners.pop.events.ProgramQuitEvent;

public class DefaultRebootScheduler implements RebootScheduler {
    private static final Logger logger = LoggerFactory.getLogger(DefaultRebootScheduler.class);

    private final Configuration configuration;
    private final Scheduler scheduler;

    private boolean runnable = true;
    private ScheduleBuilder<CronTrigger> scheduleBuilder;

    @Inject
    public DefaultRebootScheduler(Configuration configuration) {
        this.configuration = configuration;

        try {
            String schedule = configuration.getCronRebootSchedule();
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

    @Override
    public void shutdown() throws InterruptedException {
        try {
            if (scheduler != null && !scheduler.isShutdown()) {
                scheduler.shutdown();
            }
        } catch (SchedulerException e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    public void run() {
        if (runnable && scheduler != null) {
            try {
                scheduler.start();

                JobDetail job = JobBuilder.newJob(ScheduledPoPJob.class)
                        .withIdentity("scheduledReboot")
                        .build();

                Trigger trigger = TriggerBuilder.newTrigger()
                        .withIdentity("propertiesScheduleReboot")
                        .withSchedule(scheduleBuilder)
                        .build();

                scheduler.scheduleJob(job, trigger);

                SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String info = String.format("Found cron schedule '" + configuration.getCronRebootSchedule() + "', first trigger at %s", dateFormatter.format(trigger.getNextFireTime()));
                InternalEventBus.getInstance().post(new InfoMessageEvent(info));
            } catch (SchedulerException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    private void executeSchedule() {
    	logger.info("Rebooting the miner as scheduled");
    	ProgramQuitEvent event = new ProgramQuitEvent(1);
        InternalEventBus.getInstance().post(event);
    }
}
