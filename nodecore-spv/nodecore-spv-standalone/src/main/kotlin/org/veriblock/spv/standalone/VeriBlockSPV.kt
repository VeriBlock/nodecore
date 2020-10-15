// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

@file:JvmName("VeriBlockSPV")

package org.veriblock.spv.standalone

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import me.tongfei.progressbar.ProgressBarBuilder
import me.tongfei.progressbar.ProgressBarStyle
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.veriblock.core.Context
import org.veriblock.core.SharedConstants
import org.veriblock.core.tuweni.progpow.ProgPoWCache
import org.veriblock.core.utilities.Configuration
import org.veriblock.core.utilities.createLogger
import org.veriblock.core.utilities.debugError
import org.veriblock.shell.CommandFactory
import org.veriblock.shell.Shell
import org.veriblock.spv.standalone.commands.spvCommands
import org.veriblock.spv.standalone.commands.standardCommands
import org.veriblock.spv.SpvConfig
import org.veriblock.spv.SpvContext
import org.veriblock.spv.model.DownloadStatus
import java.security.Security
import java.util.concurrent.CountDownLatch
import kotlin.system.exitProcess

private val logger = createLogger {}

private val shutdownSignal = CountDownLatch(1)

private val config = Configuration()
private val spvConfig: SpvConfig = config.extract("spv") ?: SpvConfig()

private fun run(): Int {
    Security.addProvider(BouncyCastleProvider())
    Runtime.getRuntime().addShutdownHook(Thread {
        shutdownSignal.countDown()
    })

    ProgPoWCache.setMaxCachedPairs(2); // Fewer cached pairs for SPV
    val spvContext = SpvContext()
    val shell = Shell(CommandFactory().apply {
        standardCommands()
        spvCommands(spvContext)
    })
    shell.initialize()

    print(SharedConstants.LICENSE)
    println(SharedConstants.VERIBLOCK_APPLICATION_NAME.replace("$1", ApplicationMeta.FULL_APPLICATION_NAME_VERSION.replace("VeriBlock ", "")))
    println("\t\t${SharedConstants.VERIBLOCK_WEBSITE}")
    println("\t\t${SharedConstants.VERIBLOCK_EXPLORER}\n")
    println("${SharedConstants.VERIBLOCK_PRODUCT_WIKI_URL.replace("$1", "https://wiki.veriblock.org/index.php/NodeCore-SPV")}\n")
    println("${SharedConstants.TYPE_HELP}\n")

    logger.info { "Initializing SPV Context (${spvConfig.network})..." }
    var errored = false
    try {
        spvContext.init(spvConfig)
        logger.info { "Looking for peers..." }
        spvContext.peerTable.start()
        runBlocking {
            var status = spvContext.peerTable.getDownloadStatus()
            while (status.downloadStatus == DownloadStatus.DISCOVERING) {
                delay(1000L)
                status = spvContext.peerTable.getDownloadStatus()
            }
            if (status.downloadStatus == DownloadStatus.DOWNLOADING) {
                val progPowHeight = Context.get().networkParameters.progPowForkHeight.toLong()
                val initialHeight = status.currentHeight
                if (status.currentHeight < progPowHeight) {
                    ProgressBarBuilder().apply {
                        setTaskName("Downloading vBlake Blocks")
                        setInitialMax(progPowHeight - initialHeight)
                        setStyle(ProgressBarStyle.COLORFUL_UNICODE_BLOCK)
                    }.build().use { progressBar ->
                        while (status.downloadStatus == DownloadStatus.DOWNLOADING && status.currentHeight < progPowHeight) {
                            progressBar.stepTo(status.currentHeight.toLong() - initialHeight)
                            delay(1000L)
                            status = spvContext.peerTable.getDownloadStatus()
                        }
                        progressBar.stepTo(progPowHeight - initialHeight)
                    }
                }
                logger.info { "Proceeding to download vProgPoW blocks. This operation is highly CPU-intensive and will take some time." }
                val progPowInitialHeight = initialHeight.toLong().coerceAtLeast(progPowHeight)
                ProgressBarBuilder().apply {
                    setTaskName("Downloading vProgPow Blocks")
                    setInitialMax((status.bestHeight - progPowInitialHeight).coerceAtLeast(1))
                    setStyle(ProgressBarStyle.COLORFUL_UNICODE_BLOCK)
                }.build().use { progressBar ->
                    while (status.downloadStatus == DownloadStatus.DOWNLOADING) {
                        progressBar.maxHint(status.bestHeight.toLong() - progPowInitialHeight)
                        progressBar.stepTo(status.currentHeight.toLong() - progPowInitialHeight)
                        delay(1000L)
                        status = spvContext.peerTable.getDownloadStatus()
                    }
                    progressBar.maxHint(status.bestHeight.toLong() - progPowInitialHeight)
                    progressBar.stepTo(status.bestHeight.toLong() - progPowInitialHeight)
                }
            }
        }
        logger.info { "SPV is ready. Current blockchain height: ${spvContext.peerTable.getDownloadStatus().currentHeight}" }
        logger.info { """Type "help" to display a list of available commands""" }
        shell.run()
    } catch (e: Exception) {
        errored = true
        logger.debugError(e) { "Fatal error" }
        e.printStackTrace()
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
