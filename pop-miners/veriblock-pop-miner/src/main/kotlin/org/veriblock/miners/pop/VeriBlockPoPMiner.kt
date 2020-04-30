// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

@file:JvmName("VeriBlockPoPMiner")

package org.veriblock.miners.pop

import mu.KotlinLogging
import org.bitcoinj.core.Context
import org.bitcoinj.utils.Threading
import org.koin.core.context.startKoin
import org.veriblock.core.SharedConstants
import org.veriblock.miners.pop.api.ApiServer
import org.veriblock.miners.pop.api.webApiModule
import org.veriblock.miners.pop.automine.AutoMineEngine
import org.veriblock.miners.pop.schedule.PoPMiningScheduler
import org.veriblock.miners.pop.service.MinerService
import org.veriblock.miners.pop.shell.PopShell
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import kotlin.system.exitProcess

private val logger = KotlinLogging.logger {}

private val eventRegistrar = Any()

private val shutdownSignal: CountDownLatch = CountDownLatch(1)
private lateinit var shell: PopShell
private lateinit var popMinerService: MinerService
var externalQuit = false

fun run(args: Array<String>): Int {
    EventBus.shellCompletedEvent.register(eventRegistrar, ::onShellCompleted)
    EventBus.programQuitEvent.register(eventRegistrar, ::onProgramQuit)

    print(SharedConstants.LICENSE)
    Runtime.getRuntime().addShutdownHook(Thread(Runnable { shutdownSignal.countDown() }))
    val startupInjector = startKoin {
        modules(
            listOf(
                configModule(args),
                bootstrapModule,
                webApiModule
            )
        )
    }.koin
    val config: VpmConfig = startupInjector.get()
    Threading.ignoreLockCycles()
    Threading.USER_THREAD = Executor { command: Runnable ->
        Context.propagate(config.bitcoin.context)
        try {
            command.run()
        } catch (e: Exception) {
            logger.error("Exception running listener", e)
        }
    }
    popMinerService = startupInjector.get()
    val scheduler: PoPMiningScheduler = startupInjector.get()
    val autoMineEngine: AutoMineEngine = startupInjector.get()
    val apiServer: ApiServer = startupInjector.get()
    shell = startupInjector.get()
    shell.initialize()
    try {
        popMinerService.run()
        shell.runOnce()
        scheduler.run()
        autoMineEngine.run()
        apiServer.start()
        shell.run()
    } catch (e: Exception) {
        shell.renderFromThrowable(e)
    } finally {
        shutdownSignal.countDown()
    }
    try {
        shutdownSignal.await()

        EventBus.shellCompletedEvent.unregister(eventRegistrar)
        EventBus.programQuitEvent.unregister(eventRegistrar)

        apiServer.shutdown()
        autoMineEngine.shutdown()
        scheduler.shutdown()
        popMinerService.shutdown()
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
    if (quitReason == 1) {
        externalQuit = true
    }
    popMinerService.setIsShuttingDown(true)
    shell.interrupt()
}

fun main(args: Array<String>) {
    val programExitResult = run(args)
    if (externalQuit) {
        exitProcess(2)
    } else {
        exitProcess(programExitResult)
    }
}
