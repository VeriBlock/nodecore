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
import org.veriblock.core.utilities.DiagnosticUtility
import org.veriblock.lite.NodeCoreLiteKit
import org.veriblock.lite.core.Context
import org.veriblock.miners.pop.service.MinerConfig
import org.veriblock.miners.pop.service.MinerService
import org.veriblock.miners.pop.util.formatCoinAmount
import org.veriblock.sdk.alt.plugin.PluginService
import org.veriblock.sdk.models.Coin
import org.veriblock.shell.CommandFactory
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
        description = "Collect information about the application for troubleshooting"
    ) {
        printInfo("Running several checks, this may take a few moments...")
        val diagnosticInfo = DiagnosticUtility.getDiagnosticInfo()
        printInfo("Run Diagnostic Info: ${GsonBuilder().setPrettyPrinting().create().toJson(diagnosticInfo)}")
        if (nodeCoreLiteKit.network.isHealthy()) {
            printInfo("SUCCESS - NodeCore connection: Connected")
            val nodeCoreSyncStatus = nodeCoreLiteKit.network.getNodeCoreSyncStatus()
            if (nodeCoreLiteKit.network.isSynchronized()) {
                printInfo("SUCCESS - NodeCore synchronization status: Synchronized (LocalHeight=${nodeCoreSyncStatus.localBlockchainHeight} NetworkHeight=${nodeCoreSyncStatus.networkHeight})")
            } else {
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
                        val chainSyncStatus = it.value.getSynchronizedStatus()
                        if (chainSyncStatus.isSynchronized) {
                            printInfo("SUCCESS - ${it.value.name} synchronization status: Synchronized (LocalHeight=${chainSyncStatus.localBlockchainHeight} NetworkHeight=${chainSyncStatus.networkHeight})")
                        } else {
                            printInfo("FAIL- ${it.value.name} synchronization status: Not synchronized, ${chainSyncStatus.blockDifference} blocks left (LocalHeight=${chainSyncStatus.localBlockchainHeight} NetworkHeight=${chainSyncStatus.networkHeight})")
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
                    printInfo("SUCCESS - The PoP wallet contains sufficient funds, current balance: ${currentBalance.atomicUnits.formatCoinAmount()} ${context.vbkTokenName}")
                } else {
                    printInfo("FAIL - The VBK PoP wallet does not contain sufficient funds: current balance: ${currentBalance.atomicUnits.formatCoinAmount()} ${context.vbkTokenName}, minimum required: ${config.maxFee.formatCoinAmount()}, need ${(config.maxFee - currentBalance.atomicUnits).formatCoinAmount()} more. See: https://wiki.veriblock.org/index.php/Get_VBK")
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
