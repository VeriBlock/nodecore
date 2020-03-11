// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop

import mu.KotlinLogging
import nodecore.miners.pop.api.ApiServer
import nodecore.miners.pop.api.webApiModule
import nodecore.miners.pop.events.EventBus
import nodecore.miners.pop.rules.rulesModule
import nodecore.miners.pop.shell.PopShell
import nodecore.miners.pop.storage.repositoriesModule
import org.bitcoinj.core.Context
import org.bitcoinj.utils.Threading
import org.koin.core.context.startKoin
import org.veriblock.core.SharedConstants
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import kotlin.system.exitProcess

private val logger = KotlinLogging.logger {}

class Program {
    private val shutdownSignal: CountDownLatch = CountDownLatch(1)
    private lateinit var shell: PopShell
    private lateinit var popMiner: PoPMiner
    var externalQuit = false

    init {
        EventBus.shellCompletedEvent.register(this, ::onShellCompleted)
        EventBus.programQuitEvent.register(this, ::onProgramQuit)
    }

    fun run(args: Array<String>): Int {
        print(SharedConstants.LICENSE)
        Runtime.getRuntime().addShutdownHook(Thread(Runnable { shutdownSignal.countDown() }))
        val startupInjector = startKoin {
            modules(listOf(
                bootstrapModule,
                repositoriesModule,
                rulesModule,
                webApiModule
            ))
        }.koin
        val options: ProgramOptions = startupInjector.get()
        options.parse(args)
        val configuration: Configuration = startupInjector.get()
        configuration.load()
        configuration.save()
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
        popMiner = startupInjector.get()
        val scheduler: PoPMiningScheduler = startupInjector.get()
        val eventEngine: PoPEventEngine = startupInjector.get()
        val apiServer: ApiServer = startupInjector.get()
        apiServer.port = configuration.httpApiPort
        shell = startupInjector.get()
        shell.initialize()
        try {
            popMiner.run()
            scheduler.run()
            eventEngine.run()
            apiServer.start()
            shell.run()
        } catch (e: Exception) {
            shell.renderFromThrowable(e)
        } finally {
            shutdownSignal.countDown()
        }
        try {
            shutdownSignal.await()
            apiServer.shutdown()
            eventEngine.shutdown()
            scheduler.shutdown()
            popMiner.shutdown()
            nodecore.miners.pop.Threading.shutdown()
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

    private fun onShellCompleted() {
        try {
            shutdownSignal.countDown()
        } catch (e: Exception) {
            logger.error(e.message, e)
        }
    }

    private fun onProgramQuit(quitReason: Int) {
        ///HACK: imitate an "exit" command in the console
        if (quitReason == 1) {
            externalQuit = true
            popMiner.setIsShuttingDown(true)
        }
        shell.interrupt()
    }
}

fun main(args: Array<String>) {
    val main = Program()
    val programExitResult = main.run(args)
    if (main.externalQuit) {
        exitProcess(2)
    } else {
        exitProcess(programExitResult)
    }
}
