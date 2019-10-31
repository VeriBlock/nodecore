// VeriBlock PoP Miner
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop

import com.google.common.eventbus.Subscribe
import nodecore.miners.pop.api.ApiServer
import nodecore.miners.pop.api.webApiModule
import nodecore.miners.pop.contracts.*
import nodecore.miners.pop.events.ShellCompletedEvent
import nodecore.miners.pop.rules.rulesModule
import nodecore.miners.pop.shell.PopShell
import nodecore.miners.pop.storage.repositoryModule
import org.bitcoinj.core.Context
import org.bitcoinj.utils.Threading
import org.koin.core.context.startKoin
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.veriblock.core.SharedConstants
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
        val startupInjector = startKoin {
            modules(listOf(
                bootstrapModule,
                repositoryModule,
                rulesModule,
                webApiModule
            ))
        }.koin
        val options: ProgramOptions = startupInjector.get()
        options.parse(args)
        val configuration: Configuration = startupInjector.get()
        configuration.load()
        configuration.save()
        val messageService: MessageService = startupInjector.get()
        val context: Context = startupInjector.get()
        Threading.ignoreLockCycles()
        Threading.USER_THREAD = Executor { command: Runnable ->
            Context.propagate(context)
            try {
                command.run()
            } catch (e: Exception) {
                logger.error("Exception running listener", e)
            }
        }
        val popMiner: PoPMiner = startupInjector.get()
        val scheduler: PoPMiningScheduler = startupInjector.get()
        val eventEngine: PoPEventEngine = startupInjector.get()
        val apiServer: ApiServer = startupInjector.get()
        apiServer.address = configuration.httpApiAddress
        apiServer.port = configuration.httpApiPort
        val shell: PopShell = startupInjector.get()
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
