// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

@file:JvmName("AltchainPoPMiner")

package org.veriblock.miners.pop

import java.net.BindException
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.time.delay
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.koin.core.context.startKoin
import org.veriblock.core.SharedConstants
import org.veriblock.core.utilities.createLogger
import org.veriblock.core.utilities.debugError
import org.veriblock.miners.pop.api.ApiServer
import org.veriblock.miners.pop.api.webApiModule
import org.veriblock.miners.pop.securityinheriting.SecurityInheritingService
import org.veriblock.miners.pop.service.AltchainPopMinerService
import org.veriblock.sdk.alt.plugin.PluginService
import org.veriblock.sdk.util.checkSystemClock
import org.veriblock.shell.Shell
import java.security.Security
import java.time.Duration
import java.util.concurrent.CountDownLatch
import org.veriblock.core.utilities.Configuration
import org.veriblock.core.utilities.checkJvmVersion
import org.veriblock.core.utilities.extensions.checkPortViability
import kotlin.system.exitProcess

private val logger = createLogger {}

private val shutdownSignal: CountDownLatch = CountDownLatch(1)
private lateinit var shell: Shell
private lateinit var minerService: AltchainPopMinerService
private val eventRegistrar = Any()
var externalQuit = false

private fun run(autoRestart: Boolean): Int {
    Security.addProvider(BouncyCastleProvider())
    EventBus.shellCompletedEvent.register(eventRegistrar, ::onShellCompleted)
    EventBus.programQuitEvent.register(eventRegistrar, ::onProgramQuit)

    print(SharedConstants.LICENSE)
    println(SharedConstants.VERIBLOCK_APPLICATION_NAME.replace("$1", ApplicationMeta.FULL_APPLICATION_NAME_VERSION))
    println("\t\t${SharedConstants.VERIBLOCK_WEBSITE}")
    println("\t\t${SharedConstants.VERIBLOCK_EXPLORER}\n")
    println("${SharedConstants.VERIBLOCK_PRODUCT_WIKI_URL.replace("$1", "https://wiki.veriblock.org/index.php/Altchain_PoP_Miner")}\n")
    println("${SharedConstants.TYPE_HELP}\n")

    val jvmVersionResult = checkJvmVersion()
    if (!jvmVersionResult.wasSuccessful()) {
        logger.error("JVM version is not correct!")
        logger.error(jvmVersionResult.error)
        return -1
    }

    Runtime.getRuntime().addShutdownHook(Thread { shutdownSignal.countDown() })

    // Check the system clock status with NTP
    runBlocking {
        checkSystemClock()
    }

    // Automatic restart code (FIXME remove once APM becomes stable)
    if (autoRestart) {
        logger.warn { "Auto Restart enabled! Restarting in 8 hours." }
        GlobalScope.launch {
            delay(Duration.ofHours(8))
            externalQuit = true
            minerService.setIsShuttingDown(true)
            shell.interrupt()
        }
    }

    val koin = startKoin {
        modules(
            listOf(
                configModule(),
                minerModule,
                webApiModule
            )
        )
    }.koin

    val configuration: Configuration = koin.get()
    val apiPort = configuration.getInt("miner.api.port") ?: 8081
    val apiHost = configuration.getString("miner.api.host") ?: "127.0.0.1"

    if (!apiPort.checkPortViability(apiHost)) {
        logger.error { "The port $apiPort is not available at the host $apiHost, please set a different miner API port!" }
        return -1
    }

    minerService = koin.get()
    val pluginService: PluginService = koin.get()
    val securityInheritingService: SecurityInheritingService = koin.get()
    val apiServer: ApiServer = koin.get()
    shell = koin.get()

    var errored = false
    try {
        shell.initialize()
        pluginService.loadPlugins()
        if (pluginService.getPlugins().isEmpty()) {
            error("In order for APM to run, at least one altchain plugin must be configured properly.")
        }

        pluginService.getPlugins().values.forEach { it.validatePayoutAddress() }

        minerService.initialize()
        minerService.start()
        securityInheritingService.start(minerService)
        apiServer.start(apiHost, apiPort)
        shell.run()
    } catch (exception: BindException) {
        errored = true
        logger.debugError(exception) { "The port $apiPort is not available at the host $apiHost, please set a different miner API port!" }
    } catch (exception: Exception) {
        errored = true
        logger.debugError(exception) { "Fatal error" }
    } finally {
        shutdownSignal.countDown()
    }

    try {
        shutdownSignal.await()

        EventBus.shellCompletedEvent.unregister(eventRegistrar)
        EventBus.programQuitEvent.unregister(eventRegistrar)

        minerService.setIsShuttingDown(true)
        apiServer.shutdown()
        securityInheritingService.stop()
        minerService.shutdown()

        logger.info("Application exit")
    } catch (e: InterruptedException) {
        logger.debugError(e) { "Shutdown signal was interrupted" }
        return 1
    } catch (e: Exception) {
        logger.debugError(e) { "Could not shut down services cleanly" }
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
    minerService.setIsShuttingDown(true)
    shell.interrupt()
}

fun main(args: Array<String>) {
    val autoRestart = args.firstOrNull()?.toLowerCase() == "autorestart"
    val programExitResult = run(autoRestart)
    if (externalQuit) {
        exitProcess(2)
    } else {
        exitProcess(programExitResult)
    }
}
