// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.shell.commands

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.veriblock.core.utilities.createLogger
import org.veriblock.core.utilities.debugError
import org.veriblock.core.utilities.extensions.toHex
import org.veriblock.lite.NodeCoreLiteKit
import org.veriblock.miners.pop.securityinheriting.SecurityInheritingService
import org.veriblock.sdk.alt.plugin.PluginService
import org.veriblock.sdk.models.VBlakeHash
import org.veriblock.sdk.util.Utils
import org.veriblock.shell.CommandFactory
import org.veriblock.shell.CommandParameter
import org.veriblock.shell.CommandParameterMappers
import org.veriblock.shell.command
import org.veriblock.shell.core.failure
import org.veriblock.shell.core.success

private val logger = createLogger {}

fun CommandFactory.altchainCommands(
    nodeCoreLiteKit: NodeCoreLiteKit,
    pluginService: PluginService,
    securityInheritingService: SecurityInheritingService
) {
    command(
        name = "Update Context",
        form = "updatecontext",
        description = "Utility to test the vtb and atv submission for an altchain",
        parameters = listOf(
            CommandParameter("chain", CommandParameterMappers.STRING)
        )
    ) {
        val chain: String = getParameter("chain")

        if (!nodeCoreLiteKit.network.isHealthy()) {
            return@command failure {
                addMessage("V010", "Unable to run command", "Cannot connect to NodeCore", true)
            }
        }

        val securityInheritingChain = pluginService[chain]
            ?: return@command failure {
                addMessage("V010", "Unable to run command", "Unable to load plugin $chain", true)
            }

        val chainName = securityInheritingChain.name

        val securityInheritingMonitor = securityInheritingService.getMonitor(chain)
            ?: return@command failure {
                addMessage("V010", "Unable to run command", "Unable to find monitor service for $chainName", true)
            }

        printInfo("Launching updateContext coroutine...")
        GlobalScope.launch {
            val newBlockHeightChannel = securityInheritingMonitor.newBlockHeightBroadcastChannel.openSubscription()
            try {
                do {
                    // Wait for a new block
                    val chainTip = newBlockHeightChannel.receive()

                    printInfo("Current $chainName chain tip: $chainTip")
                    val publicationData = try {
                        securityInheritingChain.getMiningInstruction(null)
                    } catch (e: Exception) {
                        throw IllegalStateException("Failed to get PoP publication data from $chainName", e)
                    }

                    val vbkContextBlockHash = publicationData.context[0]
                    val vbkContextBlock = nodeCoreLiteKit.network.getBlock(VBlakeHash.wrap(vbkContextBlockHash))
                        ?: error("Unable to find the mining instruction's VBK context block ${vbkContextBlockHash.toHex()}")
                    val vbkChainHead = nodeCoreLiteKit.blockChain.getChainHead()
                        ?: error("Unable to get VBK's chain head!")
                    val contextAge = vbkChainHead.height - vbkContextBlock.height

                    printInfo("$chainName's current VBK context block: ${vbkContextBlock.hash} @ ${vbkContextBlock.height}")
                    printInfo("$chainName's VBK context age: $contextAge blocks old")

                    printInfo("Retrieving VTBs...")
                    val debugVeriBlockPublications = nodeCoreLiteKit.network.getDebugVeriBlockPublications(
                        Utils.encodeHex(publicationData.context[0]),
                        Utils.encodeHex(publicationData.btcContext[0])
                    )
                    if (debugVeriBlockPublications.isEmpty()) {
                        error("Invalid VBK publications from NodeCore: an empty list has been received!")
                    }
                    debugVeriBlockPublications.forEach {
                        if (it.firstBlock == null || it.firstBitcoinBlock == null) {
                            error(
                                "Invalid VBK publications from NodeCore: at least one of the received publications has no VBK blocks or no BTC blocks"
                            )
                        }
                    }

                    printInfo("${debugVeriBlockPublications.size} VTBs retrieved. Updating context...")
                    val siTxId = securityInheritingChain.updateContext(debugVeriBlockPublications)
                    printInfo("$chainName's context updated! Transaction id: $siTxId")
                } while (contextAge > 20)
            } catch (t: Throwable) {
                logger.debugError(t) { t.message ?: "Error" }
            } finally {
                newBlockHeightChannel.cancel()
            }
        }
        success()
    }
}
