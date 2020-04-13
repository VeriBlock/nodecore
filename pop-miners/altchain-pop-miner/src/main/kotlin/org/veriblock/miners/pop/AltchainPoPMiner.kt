// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

@file:JvmName("AltchainPoPMiner")

package org.veriblock.miners.pop

import org.koin.core.context.startKoin
import org.veriblock.core.utilities.createLogger
import org.veriblock.miners.pop.api.ApiServer
import org.veriblock.miners.pop.api.webApiModule
import org.veriblock.miners.pop.securityinheriting.SecurityInheritingService
import org.veriblock.miners.pop.service.MinerService
import org.veriblock.sdk.alt.plugin.PluginService
import org.veriblock.shell.Shell
import java.util.concurrent.CountDownLatch
import kotlin.system.exitProcess

private val logger = createLogger {}

private val shutdownSignal = CountDownLatch(1)

private fun run(): Int {
    Runtime.getRuntime().addShutdownHook(Thread {
        shutdownSignal.countDown()
    })

    logger.info { "Starting dependency injection" }
    val koin = startKoin {
        modules(listOf(minerModule, webApiModule))
    }.koin

    val miner: MinerService = koin.get()
    val pluginService: PluginService = koin.get()
    val securityInheritingService: SecurityInheritingService = koin.get()
    val apiServer: ApiServer = koin.get()
    val shell: Shell = koin.get()

    try {
        shell.initialize()
        pluginService.loadPlugins()
        miner.initialize()
        miner.start()
        securityInheritingService.start(miner)
        apiServer.start()
        shell.run()
    } catch (e: Exception) {
        logger.warn(e) { "Fatal error: ${e.message}" }
    } finally {
        shutdownSignal.countDown()
    }

    try {
        shutdownSignal.await()
        miner.setIsShuttingDown(true)
        apiServer.shutdown()
        securityInheritingService.stop()
        miner.shutdown()

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

fun main() {
    exitProcess(run())
}
