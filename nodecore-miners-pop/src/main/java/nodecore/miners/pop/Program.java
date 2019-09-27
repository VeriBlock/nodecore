// VeriBlock PoP Miner
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Guice;
import com.google.inject.Injector;
import nodecore.miners.pop.contracts.*;
import nodecore.miners.pop.events.ProgramQuitEvent;
import nodecore.miners.pop.events.ShellCompletedEvent;
import nodecore.miners.pop.api.ApiServer;
import nodecore.miners.pop.api.WebApiModule;
import nodecore.miners.pop.rules.RulesModule;
import nodecore.miners.pop.shell.CommandFactoryModule;
import nodecore.miners.pop.shell.DefaultShell;
import nodecore.miners.pop.storage.RepositoriesModule;
import org.bitcoinj.core.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.veriblock.core.SharedConstants;

import java.util.concurrent.*;

public class Program {
    private static final Logger logger = LoggerFactory.getLogger(Program.class);
    private final CountDownLatch shutdownSignal;
    private DefaultShell shell;
    private static boolean externalQuit = false;

    private Program() {
        this.shutdownSignal = new CountDownLatch(1);

        InternalEventBus.getInstance().register(this);
    }

    private int run(String[] args) {
        System.out.print(SharedConstants.LICENSE);
        externalQuit = false;

        Runtime.getRuntime().addShutdownHook(new Thread(shutdownSignal::countDown));

        Injector startupInjector = Guice.createInjector(
                new BootstrapModule(),
                new CommandFactoryModule(),
                new RepositoriesModule(),
                new RulesModule(),
                new WebApiModule());

        ProgramOptions options = startupInjector.getInstance(ProgramOptions.class);
        options.parse(args);

        Configuration configuration = startupInjector.getInstance(Configuration.class);
        configuration.load();
        configuration.save();

        MessageService messageService = startupInjector.getInstance(MessageService.class);

        Context context = startupInjector.getInstance(Context.class);
        org.bitcoinj.utils.Threading.ignoreLockCycles();
        org.bitcoinj.utils.Threading.USER_THREAD = command -> {
            Context.propagate(context);
            try {
                command.run();
            } catch (Exception e) {
                logger.error("Exception running listener", e);
            }
        };

        PoPMiner popMiner = startupInjector.getInstance(PoPMiner.class);
        PoPMiningScheduler scheduler = startupInjector.getInstance(PoPMiningScheduler.class);
        PoPEventEngine eventEngine = startupInjector.getInstance(PoPEventEngine.class);

        ApiServer apiServer = startupInjector.getInstance(ApiServer.class);
        apiServer.setAddress(configuration.getHttpApiAddress());
        apiServer.setPort(configuration.getHttpApiPort());

        shell = startupInjector.getInstance(DefaultShell.class);
        shell.initialize();
        try {
            popMiner.run();
            scheduler.run();
            eventEngine.run();
            apiServer.start();
            shell.run();
        } catch (Exception e) {
            shell.renderFromThrowable(e);
            shutdownSignal.countDown();
        }

        try {
            shutdownSignal.await();

            Threading.shutdown();
            apiServer.shutdown();
            eventEngine.shutdown();
            scheduler.shutdown();
            popMiner.shutdown();
            messageService.shutdown();
            configuration.save();

            logger.info("Application exit");
        } catch (InterruptedException e) {
            logger.error("Shutdown signal was interrupted", e);
            return 1;
        } catch (Exception e) {
            logger.error("Could not shut down services cleanly", e);
            return 1;
        }

        return 0;
    }

    @Subscribe public void onShellCompleted(ShellCompletedEvent event) {
        try {
            shutdownSignal.countDown();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
    
    @Subscribe public void onProgramQuit(ProgramQuitEvent event) {
    	///HACK: imitate an "exit" command in the console
    	if(event.reason == 1) {
    	    externalQuit = true;
    	}
    	shell.quitExternally();
    }

    public static void main(String[] args) {
    	Program main = new Program();
    	int programExitResult = main.run(args);
    	if(externalQuit) {
    		System.exit(2);
    		return;
    	}
        System.exit(programExitResult);
    }
}
