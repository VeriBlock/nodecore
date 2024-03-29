// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2021 Xenios SEZC
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
import org.veriblock.core.params.getDefaultNetworkParameters
import org.veriblock.core.tuweni.progpow.ProgPowCache
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
import org.veriblock.spv.util.SpvEventBus
import java.security.Security
import java.util.concurrent.CountDownLatch
import org.veriblock.core.utilities.checkJvmVersion
import kotlin.system.exitProcess

private val logger = createLogger {}

private val shutdownSignal = CountDownLatch(1)

private val config = Configuration()

private val spvConfig: SpvConfig = SpvConfig(
    networkParameters = getDefaultNetworkParameters(config.getString("network") ?: "mainnet"),
    dataDir = config.getString("dataDir") ?: config.getDataDirectory(),
    connectDirectlyTo = config.getOrNull("connectDirectlyTo") {
        getStringList(it)
    }?: emptyList(),
    trustPeerHashes = config.getBoolean("trustPeerHashes") ?: false
)

private fun run(): Int {
    Security.addProvider(BouncyCastleProvider())
    Runtime.getRuntime().addShutdownHook(Thread {
        shutdownSignal.countDown()
    })

    ProgPowCache.setMaxCachedPairs(2) // Fewer cached pairs for SPV

    print(SharedConstants.LICENSE)
    println(SharedConstants.VERIBLOCK_APPLICATION_NAME.replace("$1", ApplicationMeta.FULL_APPLICATION_NAME_VERSION.replace("VeriBlock ", "")))
    println("\t\t${SharedConstants.VERIBLOCK_WEBSITE}")
    println("\t\t${SharedConstants.VERIBLOCK_EXPLORER}\n")
    println("${SharedConstants.VERIBLOCK_PRODUCT_WIKI_URL.replace("$1", "https://wiki.veriblock.org/index.php/HowTo_run_SPV")}\n")
    println("${SharedConstants.TYPE_HELP}\n")

    val jvmVersionResult = checkJvmVersion()
    if (!jvmVersionResult.wasSuccessful()) {
        logger.error("JVM version is not correct!")
        logger.error(jvmVersionResult.error)
        return -1
    }

    val commandFactory = CommandFactory()
    // Create shell before any logging
    val shell = Shell(commandFactory)

    logger.info { "Initializing SPV Context (${spvConfig.networkParameters.name})..." }
    val spvContext = SpvContext(spvConfig)

    // Initialize the commands
    commandFactory.apply {
        standardCommands()
        spvCommands(spvContext)
    }
    shell.initialize()

    var errored = false
    try {
        logger.info { "Looking for peers..." }
        spvContext.start()
        runBlocking {
            var status = spvContext.spvService.getDownloadStatus()
            while (status.downloadStatus == DownloadStatus.DISCOVERING) {
                delay(1000L)
                status = spvContext.spvService.getDownloadStatus()
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
                            status = spvContext.spvService.getDownloadStatus()
                        }
                        progressBar.stepTo(progPowHeight - initialHeight)
                    }
                }
                if (!spvConfig.trustPeerHashes) {
                    logger.info { "Proceeding to download vProgPoW blocks. This operation is highly CPU-intensive and will take some time." }
                }
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
                        status = spvContext.spvService.getDownloadStatus()
                    }
                    progressBar.maxHint(status.bestHeight.toLong() - progPowInitialHeight)
                    progressBar.stepTo(status.bestHeight.toLong() - progPowInitialHeight)
                }
            }
        }

        SpvEventBus.addressStateUpdatedEvent.register(logger) {
            logger.info { it.toString() }
        }

        logger.info { "SPV is ready. Current blockchain height: ${spvContext.spvService.getDownloadStatus().currentHeight}" }
        logger.info { "To get started:" }
        logger.info { "Type 'getnewaddress' to create a new address" }
        logger.info { "Type 'importwallet <sourceLocation>' to import an existing wallet" }
        logger.info { "Type 'help' to display a list of available commands" }

        if (!spvContext.addressManager.isLocked) {
            try {
                spvContext.spvService.importWallet(spvContext.addressManager.walletPath())
                logger.info { "Successfully imported the wallet file: ${spvContext.addressManager.walletPath()}" }
                logger.info { "Type 'getbalance' to see the balances of all of your addresses" }
            } catch (exception: Exception) {
                logger.info { "Failed to import the wallet file: ${exception.message}" }
            }
        }

        shell.run()
    } catch (e: Exception) {
        errored = true
        logger.debugError(e) { "Fatal error" }
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
