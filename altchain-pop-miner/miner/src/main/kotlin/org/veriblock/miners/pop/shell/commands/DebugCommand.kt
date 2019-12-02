// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.shell.commands

import org.veriblock.lite.NodeCoreLiteKit
import org.veriblock.miners.pop.service.PluginService
import org.veriblock.miners.pop.tasks.logger
import org.veriblock.sdk.AltPublication
import org.veriblock.sdk.util.Utils
import org.veriblock.shell.CommandParameter
import org.veriblock.shell.CommandParameterType
import org.veriblock.shell.Shell
import org.veriblock.shell.command
import org.veriblock.shell.core.failure
import org.veriblock.shell.core.success

fun Shell.miningCommands(nodeCoreLiteKit: NodeCoreLiteKit, pluginService: PluginService) {

    command(
        name = "Debug",
        form = "debug",
        description = "Utility to test the vtb and atv submission for an altchain",
        parameters = listOf(
            CommandParameter("chain", CommandParameterType.STRING)
        )
    ) {
        val chain: String = getParameter("chain")

        if (!nodeCoreLiteKit.network.isHealthy()) {
            return@command failure {
                addMessage("V010", "Unable to debug", "Cannot connect to NodeCore", true)
            }
        }

        val securityInheritingChain = pluginService[chain]
            ?: return@command failure {
                addMessage("V010", "Unable to debug", "Unable to load plugin $chain", true)
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

        val proofOfProof = AltPublication(
            state.transaction,
            state.merklePath,
            state.blockOfProof,
            emptyList()
        )

        val siTxId = securityInheritingChain.submit(proofOfProof, debugVeriBlockPublications)
        printInfo(siTxId)
        success()
    }
}
