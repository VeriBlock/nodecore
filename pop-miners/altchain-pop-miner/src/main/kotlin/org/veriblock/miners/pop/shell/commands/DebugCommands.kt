// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.shell.commands

import io.grpc.StatusRuntimeException
import kotlinx.coroutines.runBlocking
import org.veriblock.lite.NodeCoreLiteKit
import org.veriblock.lite.core.Context
import org.veriblock.miners.pop.service.MinerConfig
import org.veriblock.miners.pop.service.MinerService
import org.veriblock.miners.pop.util.formatCoinAmount
import org.veriblock.sdk.alt.plugin.PluginService
import org.veriblock.sdk.models.Coin
import org.veriblock.shell.CommandFactory
import org.veriblock.shell.CommandParameter
import org.veriblock.shell.CommandParameterMappers
import org.veriblock.shell.command
import org.veriblock.shell.core.success

fun CommandFactory.debugCommands(
    config: MinerConfig,
    context: Context,
    minerService: MinerService,
    pluginService: PluginService,
    nodeCoreLiteKit: NodeCoreLiteKit
) {
    command(
        name = "Get Debug Information",
        form = "getdebuginfo",
        description = "Collect information about the application for troubleshooting",
        parameters = listOf(
            CommandParameter(name = "chain", mapper = CommandParameterMappers.STRING, required = true)
        )
    ) {
        val chainId = getParameter<String>("chain")

        if (nodeCoreLiteKit.network.isHealthy()) {
            printInfo("NodeCore connection: Connected")
            if (nodeCoreLiteKit.network.isSynchronized()) {
                printInfo("NodeCore synchronization status: Synchronized")
            } else {
                printInfo("NodeCore synchronization status: Not synchronized")
            }
        } else {
            printInfo("NodeCore connection: Not connected")
            printInfo("Unable to determine the NodeCore synchronization status")
        }

        val chain = pluginService[chainId]
        if (chain == null) {
            printInfo("Unable to load plugin with the id $chainId")
        } else {
            runBlocking {
                if (chain.isConnected()) {
                    printInfo("${chain.name} connection: Connected")
                    if (chain.isSynchronized()) {
                        printInfo("${chain.name} synchronization status: Synchronized")
                    } else {
                        printInfo("${chain.name} synchronization status: Not synchronized")
                    }
                } else {
                    printInfo("${chain.name} connection: Not connected")
                    printInfo("Unable to determine the ${chain.name} synchronization status")
                }
            }
        }

        try {
            val currentBalance = minerService.getBalance()?.confirmedBalance ?: Coin.ZERO
            if (nodeCoreLiteKit.network.isHealthy()) {
                if (currentBalance.atomicUnits < config.maxFee) {
                    printInfo("The PoP wallet does not contain sufficient funds")
                    printInfo("Current balance: ${currentBalance.atomicUnits.formatCoinAmount()} ${context.vbkTokenName}")
                    printInfo("Minimum required: ${config.maxFee.formatCoinAmount()}, need ${(config.maxFee - currentBalance.atomicUnits).formatCoinAmount()} more")
                } else {
                    printInfo("The PoP wallet contains sufficient funds")
                }
            } else {
                printInfo("Unable to determine the PoP balance.")
            }
        } catch(e: StatusRuntimeException) {
            printInfo("Unable to determine the PoP balance.")
        }

        success()
    }
}
