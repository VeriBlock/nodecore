// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.shell.commands

import org.veriblock.lite.NodeCoreLiteKit
import org.veriblock.sdk.alt.plugin.PluginService
import org.veriblock.sdk.util.Utils
import org.veriblock.shell.CommandFactory
import org.veriblock.shell.CommandParameter
import org.veriblock.shell.CommandParameterMappers
import org.veriblock.shell.command
import org.veriblock.shell.core.failure
import org.veriblock.shell.core.success

fun CommandFactory.altchainCommands(nodeCoreLiteKit: NodeCoreLiteKit, pluginService: PluginService) {

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

        val publicationData = try {
            securityInheritingChain.getPublicationData(null)
        } catch (e: Exception) {
            return@command failure {
                addMessage("V010", "SI failure", "Failed to get PoP publication data from SI chain $chain: ${e.message}", true)
            }
        }

        val debugVeriBlockPublications = nodeCoreLiteKit.network.getDebugVeriBlockPublications(
            Utils.encodeHex(publicationData.context[0]),
            Utils.encodeHex(publicationData.btcContext[0])
        )
        if (debugVeriBlockPublications.isEmpty()) {
            return@command failure {
                addMessage("V010", "NodeCore failure", "Invalid VBK publications from NodeCore: an empty list has been received!", true)
            }
        }
        debugVeriBlockPublications.forEach {
            if (it.firstBlock == null || it.firstBitcoinBlock == null) {
                return@command failure {
                    addMessage(
                        "V010", "NodeCore failure",
                        "Invalid VBK publications from NodeCore: at least one of the received publications has no VBK blocks or no BTC blocks", true
                    )
                }
            }
        }

        val siTxId = securityInheritingChain.updateContext(debugVeriBlockPublications)
        printInfo(siTxId)
        success()
    }
}
