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
import me.tongfei.progressbar.ProgressBarBuilder
import me.tongfei.progressbar.ProgressBarStyle
import org.veriblock.core.SharedConstants
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
import veriblock.model.DownloadStatusResponse
import veriblock.net.BootstrapPeerDiscovery
import java.lang.Thread.sleep
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
    shell.initialize()

    print(SharedConstants.LICENSE)
    println(SharedConstants.VERIBLOCK_APPLICATION_NAME.replace("$1", ApplicationMeta.FULL_APPLICATION_NAME_VERSION))
    println("\t\t${SharedConstants.VERIBLOCK_WEBSITE}")
    println("\t\t${SharedConstants.VERIBLOCK_EXPLORER}\n")
    println("${SharedConstants.VERIBLOCK_PRODUCT_WIKI_URL.replace("$1", "https://wiki.veriblock.org/index.php/NodeCore-SPV")}\n")
    println("${SharedConstants.TYPE_HELP}\n")

    var errored = false
    logger.info { "Initializing SPV Context..." }
    try {
        spvContext.init(networkParameters, BootstrapPeerDiscovery(networkParameters))
        spvContext.peerTable.start()
        ProgressBarBuilder().apply {
            setTaskName("Loading SPV")
            setInitialMax(1)
            setStyle(ProgressBarStyle.COLORFUL_UNICODE_BLOCK)
        }.build().use { progressBar ->
            var status = spvContext.peerTable.getDownloadStatus()
            progressBar.extraMessage = "Looking for peers..."
            while (status.downloadStatus != DownloadStatus.READY) {
                if (status.downloadStatus == DownloadStatus.DOWNLOADING) {
                    progressBar.extraMessage = "Downloading blocks..."
                    progressBar.maxHint(status.bestHeight.toLong())
                    progressBar.stepTo(status.currentHeight.toLong())
                }
                sleep(1000L)
                status = spvContext.peerTable.getDownloadStatus()
            }
            progressBar.maxHint(status.bestHeight.toLong())
            progressBar.stepTo(status.bestHeight.toLong())
        }
        logger.info { "SPV is ready. Current blockchain height: ${spvContext.peerTable.getDownloadStatus().currentHeight}" }
        shell.run()
    } catch (e: Exception) {
        errored = true
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

    return if (!errored) 0 else 1
}

fun main() {
    exitProcess(run())
}
