// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

@file:JvmName("VeriBlockSPV")

package org.veriblock.spv.standalone

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.veriblock.core.params.MainNetParameters
import org.veriblock.core.params.NetworkConfig
import org.veriblock.core.params.NetworkParameters
import org.veriblock.core.utilities.Configuration
import org.veriblock.core.utilities.createLogger
import org.veriblock.shell.CommandFactory
import org.veriblock.shell.Shell
import org.veriblock.spv.standalone.commands.spvCommands
import org.veriblock.spv.standalone.commands.standardCommands
import veriblock.SpvContext
import veriblock.model.DownloadStatus
import veriblock.net.BootstrapPeerDiscovery
import java.util.concurrent.CountDownLatch
import kotlin.system.exitProcess

private val logger = createLogger {}

private val shutdownSignal = CountDownLatch(1)

private val config = Configuration()

private val networkParameters = NetworkParameters(
    NetworkConfig(
        network = config.getString("network") ?: MainNetParameters.NETWORK
    )
)

private fun run(): Int {
    Runtime.getRuntime().addShutdownHook(Thread {
        shutdownSignal.countDown()
    })

    val spvContext = SpvContext()
    val shell = Shell(CommandFactory().apply {
        standardCommands()
        spvCommands(spvContext)
    })

    logger.info { "Initializing SPV Context..." }
    try {
        spvContext.init(networkParameters, BootstrapPeerDiscovery(networkParameters), false)
        GlobalScope.launch {
            spvContext.peerTable.start()
            do {
                val status = spvContext.peerTable.getDownloadStatus()
                when (status.downloadStatus) {
                    DownloadStatus.DISCOVERING ->
                        logger.info { "Waiting for peers response." }
                    DownloadStatus.DOWNLOADING ->
                        logger.info { "Blockchain is downloading. ${status.currentHeight} / ${status.bestHeight}" }
                    DownloadStatus.READY ->
                        logger.info { "Blockchain is ready. Current height ${status.currentHeight}" }
                }
                delay(5000L)
            } while (status.downloadStatus != DownloadStatus.READY)
        }
        shell.run()
    } catch (e: Exception) {
        logger.warn(e) { "Fatal error: ${e.message}" }
    } finally {
        if (!shell.running) {
            shutdownSignal.countDown()
        }
    }

    try {
        shutdownSignal.await()
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
