package org.veriblock.miners.pop.service

import io.grpc.StatusRuntimeException
import kotlinx.coroutines.runBlocking
import org.veriblock.core.utilities.DiagnosticUtility
import org.veriblock.core.utilities.SegwitAddressUtility
import org.veriblock.core.utilities.extensions.isHex
import org.veriblock.lite.NodeCoreLiteKit
import org.veriblock.lite.core.Context
import org.veriblock.miners.pop.util.formatCoinAmount
import org.veriblock.sdk.alt.plugin.PluginService
import org.veriblock.sdk.models.Coin
import org.veriblock.sdk.models.DiagnosticInformation

class DiagnosticService(
    private val config: MinerConfig,
    private val context: Context,
    private val minerService: MinerService,
    private val pluginService: PluginService,
    private val nodeCoreLiteKit: NodeCoreLiteKit
) {

    fun collectDiagnosticInformation(): DiagnosticInformation {
        val information = ArrayList<String>()

        // Check the NodeCore status
        if (nodeCoreLiteKit.network.isHealthy()) {
            information.add("SUCCESS - NodeCore connection: Connected")
            val nodeCoreStateInfo = nodeCoreLiteKit.network.getNodeCoreStateInfo()
            if (nodeCoreLiteKit.network.isSynchronized()) {
                information.add("SUCCESS - NodeCore synchronization status: Synchronized (LocalHeight=${nodeCoreStateInfo.localBlockchainHeight} NetworkHeight=${nodeCoreStateInfo.networkHeight})")
            } else {
                information.add("FAIL - NodeCore synchronization status: Not synchronized. ${nodeCoreStateInfo.blockDifference} blocks left (LocalHeight=${nodeCoreStateInfo.localBlockchainHeight} NetworkHeight=${nodeCoreStateInfo.networkHeight})")
            }
        } else {
            information.add("FAIL - NodeCore connection: Not connected")
            information.add("FAIL - Unable to determine the NodeCore synchronization status")
        }

        // Check the altchains status
        val plugins = pluginService.getPlugins().filter { it.key != "test" }
        if (plugins.isNotEmpty()) {
            runBlocking {
                plugins.forEach {
                    if (it.value.isConnected()) {
                        information.add("SUCCESS - ${it.value.name} connection: Connected")
                        val chainSyncStatus = it.value.getBlockChainInfo()
                        if (chainSyncStatus.isSynchronized) {
                            information.add("SUCCESS - ${it.value.name} synchronization status: Synchronized (LocalHeight=${chainSyncStatus.localBlockchainHeight} NetworkHeight=${chainSyncStatus.networkHeight} InitialBlockDownload=${chainSyncStatus.initialblockdownload})")
                        } else {
                            information.add("FAIL- ${it.value.name} synchronization status: Not synchronized, ${chainSyncStatus.blockDifference} blocks left (LocalHeight=${chainSyncStatus.localBlockchainHeight} NetworkHeight=${chainSyncStatus.networkHeight} InitialBlockDownload=${chainSyncStatus.initialblockdownload})")
                        }
                    } else {
                        information.add("FAIL - ${it.value.name} connection: Not connected")
                        information.add("FAIL - Unable to determine the ${it.value.name} synchronization status")
                    }
                }
            }
        } else {
            information.add("FAIL - There are no SI chains loaded")
        }

        // Check the payoutAddress for the SI chains
        val configuredPlugins = pluginService.getConfiguredPlugins().filter { it.key != "test" }
        if (configuredPlugins.isNotEmpty()) {
            configuredPlugins.forEach {
                val configuredPayoutAddress = it.value.payoutAddress ?: ""
                val isValidAddress = if (configuredPayoutAddress.isHex()) {
                    true
                } else {
                    try {
                        SegwitAddressUtility.generatePayoutScriptFromSegwitAddress(configuredPayoutAddress)
                        true
                    } catch (e: Exception) {
                        false
                    }
                }
                if (configuredPayoutAddress == "INSERT PAYOUT ADDRESS" || !isValidAddress) {
                    information.add("FAIL - ${it.value.name} configured payoutAddress '$configuredPayoutAddress' is not valid")
                } else {
                    information.add("SUCCESS - ${it.value.name} configured payoutAddress '$configuredPayoutAddress' is valid")
                }

                // Check the auto mine rounds
                val configuredAutoMineRounds = it.value.autoMineRounds
                val min = configuredAutoMineRounds.min() ?: 0
                val max = configuredAutoMineRounds.max() ?: 0
                if (configuredAutoMineRounds.isNotEmpty() && (min < 1 || max > 4)) {
                    val invalidMineRounds = configuredAutoMineRounds.filter { mineRound ->
                        mineRound < 1 || mineRound > 4
                    }
                    information.add("FAIL - ${it.value.name} configuration has invalid value for autoMineRounds. '[${invalidMineRounds.joinToString(",")}]' is not valid, the rounds should be between 1 and 4")
                } else {
                    information.add("SUCCESS - ${it.value.name} configuration for autoMineRounds is valid")
                }
            }
        } else {
            information.add("FAIL - There are no SI chains configured")
        }

        // Check the balance
        try {
            val currentBalance = minerService.getBalance()?.confirmedBalance ?: Coin.ZERO
            if (nodeCoreLiteKit.network.isHealthy()) {
                if (currentBalance.atomicUnits > config.maxFee) {
                    information.add("SUCCESS - The ${context.vbkTokenName} PoP wallet contains sufficient funds, current balance: ${currentBalance.atomicUnits.formatCoinAmount()} ${context.vbkTokenName}")
                } else {
                    information.add("FAIL - The ${context.vbkTokenName} PoP wallet does not contain sufficient funds: current balance: ${currentBalance.atomicUnits.formatCoinAmount()} ${context.vbkTokenName}, minimum required: ${config.maxFee.formatCoinAmount()}, need ${(config.maxFee - currentBalance.atomicUnits).formatCoinAmount()} more. See: https://wiki.veriblock.org/index.php/Get_VBK")
                }
            } else {
                information.add("FAIL - Unable to determine the ${context.vbkTokenName} dPoP balance.")
            }
        } catch(e: StatusRuntimeException) {
            information.add("FAIL - Unable to determine the ${context.vbkTokenName} PoP balance.")
        }

        // Add the diagnostic information
        val diagnosticInfo = DiagnosticUtility.getDiagnosticInfo()
        return DiagnosticInformation(information, diagnosticInfo)
    }
}
