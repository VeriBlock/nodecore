// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.shell.commands

import com.google.gson.GsonBuilder
import io.grpc.StatusRuntimeException
import kotlinx.coroutines.runBlocking
import org.veriblock.lite.NodeCoreLiteKit
import org.veriblock.lite.core.Context
import org.veriblock.miners.pop.service.MinerConfig
import org.veriblock.miners.pop.service.MinerService
import org.veriblock.miners.pop.service.MiningOperationMapperService
import org.veriblock.miners.pop.service.MiningOperationMapperService.OperationStatus.PENDING
import org.veriblock.miners.pop.util.formatCoinAmount
import org.veriblock.sdk.alt.plugin.PluginService
import org.veriblock.sdk.models.Coin
import org.veriblock.shell.CommandFactory
import org.veriblock.shell.CommandParameter
import org.veriblock.shell.CommandParameterMappers
import org.veriblock.shell.command
import org.veriblock.shell.core.failure
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
        description = "Collect information about the application for troubleshooting"
    ) {
        if (nodeCoreLiteKit.network.isHealthy()) {
            printInfo("SUCCESS - NodeCore connection: Connected")
            if (nodeCoreLiteKit.network.isSynchronized()) {
                printInfo("SUCCESS - NodeCore synchronization status: Synchronized")
            } else {
                val nodeCoreSyncStatus = nodeCoreLiteKit.network.getNodeCoreSyncStatus()
                printInfo("FAIL - NodeCore synchronization status: Not synchronized. ${nodeCoreSyncStatus.blockDifference} blocks left (LocalHeight=${nodeCoreSyncStatus.localBlockchainHeight} NetworkHeight=${nodeCoreSyncStatus.networkHeight})")
            }
        } else {
            printInfo("FAIL - NodeCore connection: Not connected")
            printInfo("FAIL - Unable to determine the NodeCore synchronization status")
        }

        val plugins = pluginService.getPlugins().filter { it.key != "test" }
        if (plugins.isNotEmpty()) {
            runBlocking {
                plugins.forEach {
                    if (it.value.isConnected()) {
                        printInfo("SUCCESS - ${it.value.name} connection: Connected")
                        if (it.value.isSynchronized()) {
                            printInfo("SUCCESS - ${it.value.name} synchronization status: Synchronized")
                        } else {
                            printInfo("FAIL- ${it.value.name} synchronization status: Not synchronized")
                        }
                    } else {
                        printInfo("FAIL - ${it.value.name} connection: Not connected")
                        printInfo("FAIL - Unable to determine the ${it.value.name} synchronization status")
                    }
                }
            }
        } else {
            printInfo("FAIL - There are no plugins loaded")
        }

        try {
            val currentBalance = minerService.getBalance()?.confirmedBalance ?: Coin.ZERO
            if (nodeCoreLiteKit.network.isHealthy()) {
                if (currentBalance.atomicUnits > config.maxFee) {
                    printInfo("SUCCESS - The PoP wallet contains sufficient funds")
                } else {
                    printInfo("FAIL - The PoP wallet does not contain sufficient funds:")
                    printInfo("FAIL - Current balance: ${currentBalance.atomicUnits.formatCoinAmount()} ${context.vbkTokenName}")
                    printInfo("FAIL - Minimum required: ${config.maxFee.formatCoinAmount()}, need ${(config.maxFee - currentBalance.atomicUnits).formatCoinAmount()} more")
                }
            } else {
                printInfo("FAIL - Unable to determine the PoP balance.")
            }
        } catch(e: StatusRuntimeException) {
            printInfo("FAIL - Unable to determine the PoP balance.")
        }

        success()
    }
}
