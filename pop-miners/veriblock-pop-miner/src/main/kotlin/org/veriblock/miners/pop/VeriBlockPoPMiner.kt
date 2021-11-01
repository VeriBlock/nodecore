// VeriBlock PoP Miner
// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

@file:JvmName("VeriBlockPoPMiner")

package org.veriblock.miners.pop

import java.net.BindException
import mu.KotlinLogging
import org.bitcoinj.core.Context
import org.bitcoinj.utils.Threading
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.koin.core.context.startKoin
import org.veriblock.core.SharedConstants
import org.veriblock.core.params.*
import org.veriblock.core.tuweni.progpow.ProgPoWCache
import org.veriblock.miners.pop.api.ApiServer
import org.veriblock.miners.pop.api.webApiModule
import org.veriblock.miners.pop.automine.AutoMineEngine
import org.veriblock.miners.pop.schedule.PopMiningScheduler
import org.veriblock.miners.pop.service.MinerService
import org.veriblock.miners.pop.shell.PopShell
import java.security.Security
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import org.veriblock.core.utilities.checkJvmVersion
import org.veriblock.core.utilities.debugError
import org.veriblock.core.utilities.extensions.checkPortViability
import kotlin.system.exitProcess

private val logger = KotlinLogging.logger {}

private val eventRegistrar = Any()

private val shutdownSignal: CountDownLatch = CountDownLatch(1)
private lateinit var shell: PopShell
private lateinit var popMinerService: MinerService
var externalQuit = false

fun run(args: Array<String>): Int {
    Security.addProvider(BouncyCastleProvider())
    EventBus.shellCompletedEvent.register(eventRegistrar, ::onShellCompleted)
    EventBus.programQuitEvent.register(eventRegistrar, ::onProgramQuit)

    print(SharedConstants.LICENSE)
    println(SharedConstants.VERIBLOCK_APPLICATION_NAME.replace("$1", ApplicationMeta.FULL_APPLICATION_NAME_VERSION))
    println("\t\t${SharedConstants.VERIBLOCK_WEBSITE}")
    println("\t\t${SharedConstants.VERIBLOCK_EXPLORER}\n")
    println("${SharedConstants.VERIBLOCK_PRODUCT_WIKI_URL.replace("$1", "https://wiki.veriblock.org/index.php/HowTo_run_PoP_Miner_0.4.9")}\n")
    println("${SharedConstants.TYPE_HELP}\n")

    val jvmVersionResult = checkJvmVersion()
    if (!jvmVersionResult.wasSuccessful()) {
        logger.error("JVM version is not correct!")
        logger.error(jvmVersionResult.error)
        return -1
    }

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
    if (!config.api.port.checkPortViability(config.api.host)) {
        logger.error { "The port ${config.api.port} is not available at the host ${config.api.host}, please set a different miner API port!" }
        return -1
    }

    if (config.bitcoin.network.toString() == "MainNet") {
        org.veriblock.core.Context.create(defaultMainNetParameters);
    } else {
        org.veriblock.core.Context.create(defaultTestNetParameters);
    }

    ProgPoWCache.setMaxCachedPairs(2); // Fewer cached pairs for PoP miner

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
    val scheduler: PopMiningScheduler = startupInjector.get()
    val autoMineEngine: AutoMineEngine = startupInjector.get()
    val apiServer: ApiServer = startupInjector.get()
    shell = startupInjector.get()
    shell.initialize()

    var errored = false
    try {
        popMinerService.run()
        shell.runOnce()
        scheduler.run()
        autoMineEngine.run()
        apiServer.start()
        shell.run()
    } catch (exception: BindException) {
        errored = true
        logger.debugError(exception) { "The port ${config.api.port} is not available at the host ${config.api.host}, please set a different miner API port!" }
    } catch (exception: Exception) {
        errored = true
        shell.renderFromThrowable(exception)
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
    return if (!errored) 0 else 1
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
