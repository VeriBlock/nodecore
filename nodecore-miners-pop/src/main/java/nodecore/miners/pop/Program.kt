// VeriBlock PoP Miner
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop

import com.google.common.eventbus.Subscribe
import com.google.inject.Guice
import com.google.inject.Injector
import nodecore.miners.pop.api.ApiServer
import nodecore.miners.pop.contracts.*
import nodecore.miners.pop.events.ShellCompletedEvent
import nodecore.miners.pop.rules.RulesModule
import nodecore.miners.pop.shell.PopShell
import nodecore.miners.pop.storage.RepositoriesModule
import org.bitcoinj.core.Context
import org.bitcoinj.utils.Threading
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.veriblock.core.SharedConstants
import org.veriblock.shell.Shell
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import kotlin.system.exitProcess

class Program {
    private val shutdownSignal: CountDownLatch = CountDownLatch(1)

    init {
        InternalEventBus.getInstance().register(this)
    }

    fun run(args: Array<String>): Int {
        print(SharedConstants.LICENSE)
        Runtime.getRuntime().addShutdownHook(Thread(Runnable { shutdownSignal.countDown() }))
        val startupInjector: Injector = Guice.createInjector(
            BootstrapModule(),
            RepositoriesModule(),
            RulesModule())
        val options: ProgramOptions = startupInjector.getInstance(ProgramOptions::class.java)
        options.parse(args)
        val configuration: Configuration = startupInjector.getInstance(Configuration::class.java)
        configuration.load()
        configuration.save()
        val messageService: MessageService = startupInjector.getInstance(MessageService::class.java)
        val context: Context = startupInjector.getInstance(Context::class.java)
        Threading.ignoreLockCycles()
        Threading.USER_THREAD = Executor { command: Runnable ->
            Context.propagate(context)
            try {
                command.run()
            } catch (e: Exception) {
                logger.error("Exception running listener", e)
            }
        }
        val popMiner: PoPMiner = startupInjector.getInstance(PoPMiner::class.java)
        val scheduler: PoPMiningScheduler = startupInjector.getInstance(PoPMiningScheduler::class.java)
        val eventEngine: PoPEventEngine = startupInjector.getInstance(PoPEventEngine::class.java)
        val apiServer: ApiServer = startupInjector.getInstance(ApiServer::class.java)
        apiServer.address = configuration.httpApiAddress
        apiServer.port = configuration.httpApiPort
        val shell: PopShell = startupInjector.getInstance(PopShell::class.java)
        shell.initialize()
        try {
            popMiner.run()
            scheduler.run()
            eventEngine.run()
            apiServer.start()
            shell.run()
        } catch (e: Exception) {
            shell.renderFromThrowable(e)
            shutdownSignal.countDown()
        }
        try {
            shutdownSignal.await()
            nodecore.miners.pop.Threading.shutdown()
            apiServer.shutdown()
            eventEngine.shutdown()
            scheduler.shutdown()
            popMiner.shutdown()
            messageService.shutdown()
            configuration.save()
            logger.info("Application exit")
        } catch (e: InterruptedException) {
            logger.error("Shutdown signal was interrupted", e)
            return 1
        } catch (e: Exception) {
            logger.error("Could not shut down services cleanly", e)
            return 1
        }
        return 0
    }

    @Subscribe
    fun onShellCompleted(event: ShellCompletedEvent?) {
        try {
            shutdownSignal.countDown()
        } catch (e: Exception) {
            logger.error(e.message, e)
        }
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(Program::class.java)
    }
}

fun main(args: Array<String>) {
    val main = Program()
    exitProcess(main.run(args))
}
