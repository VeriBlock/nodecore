package org.veriblock.miners.pop.service

import io.grpc.StatusRuntimeException
import kotlinx.coroutines.runBlocking
import org.veriblock.core.utilities.DiagnosticUtility
import org.veriblock.miners.pop.core.ApmContext
import org.veriblock.miners.pop.MinerConfig
import org.veriblock.miners.pop.securityinheriting.SecurityInheritingService
import org.veriblock.miners.pop.util.formatCoinAmount
import org.veriblock.sdk.alt.plugin.PluginService
import org.veriblock.sdk.models.DiagnosticInformation
import org.veriblock.sdk.models.getSynchronizedMessage

class DiagnosticService(
    private val context: ApmContext,
    private val minerConfig: MinerConfig,
    private val minerService: AltchainPopMinerService,
    private val pluginService: PluginService,
    private val securityInheritingService: SecurityInheritingService
) {
    fun collectDiagnosticInformation(): DiagnosticInformation {
        val information = ArrayList<String>()

        // Check the NodeCore status
        if (minerService.network.isAccessible()) {
            information.add("SUCCESS - SPV connection: Connected")
            // Synchronization
            if (minerService.network.isSynchronized()) {
                information.add("SUCCESS - SPV synchronization status: Synchronized ${minerService.network.latestSpvStateInfo.getSynchronizedMessage()}")
            } else {
                information.add("FAIL - SPV synchronization status: Not synchronized. ${minerService.network.latestSpvStateInfo.getSynchronizedMessage()}")
            }
        } else {
            information.add("FAIL - SPV connection: Not connected")
            information.add("FAIL - SPV unable to determine the synchronization status")
            information.add("FAIL - SPV unable to determine the configured network")
        }

        // Check the altchains status
        val plugins = pluginService.getPlugins().filter { it.key != "test" }
        if (plugins.isNotEmpty()) {
            runBlocking {
                plugins.forEach {
                    val chainMonitor = securityInheritingService.getMonitor(it.key)
                        ?: error("Unable to load altchain monitor ${it.key}") // Shouldn't happen
                    if (chainMonitor.isAccessible()) {
                        information.add("SUCCESS - ${it.value.name} connection: Connected to ${it.value.config.host}")
                        // Configured network
                        if (chainMonitor.isOnSameNetwork()) {
                            information.add("SUCCESS - ${it.value.name} configured network: ${it.value.name} & APM are running on the same configured network (${context.networkParameters.name})")
                        } else {
                            information.add("FAIL - ${it.value.name} configured network: ${it.value.name} (${chainMonitor.latestBlockChainInfo.networkVersion}) & APM (${context.networkParameters.name}) are not running on the same configured network")
                        }
                        // Synchronization
                        if (chainMonitor.isSynchronized()) {
                            information.add("SUCCESS - ${it.value.name} synchronization status: Synchronized ${chainMonitor.latestBlockChainInfo.getSynchronizedMessage()}")
                        } else {
                            information.add("FAIL- ${it.value.name} synchronization status: Not synchronized, ${chainMonitor.latestBlockChainInfo.getSynchronizedMessage()}")
                        }
                    } else {
                        information.add("FAIL - ${it.value.name} connection: Not connected to ${it.value.config.host}")
                        information.add("FAIL - ${it.value.name} Unable to determine the synchronization status")
                        information.add("FAIL - ${it.value.name} unable to determine the configured network")
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
                val configuredPayoutAddress = it.value.payoutAddress
                if (configuredPayoutAddress == null || configuredPayoutAddress == "INSERT PAYOUT ADDRESS" || configuredPayoutAddress.isEmpty()) {
                    information.add("FAIL - ${it.value.name} payoutAddress: '$configuredPayoutAddress' is not configured")
                } else {
                    information.add("SUCCESS - ${it.value.name} payoutAddress: '$configuredPayoutAddress' is configured")
                }

                // Check the auto mine rounds
                val configuredAutoMineRounds = it.value.autoMineRounds
                val min = configuredAutoMineRounds.minOrNull() ?: 0
                val max = configuredAutoMineRounds.minOrNull() ?: 0
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
            val currentBalance = minerService.getBalance().confirmedBalance
            if (minerService.network.isAccessible()) {
                if (currentBalance.atomicUnits > minerConfig.feePerByte) {
                    information.add("SUCCESS - The ${context.vbkTokenName} PoP wallet contains sufficient funds, current balance: ${currentBalance.atomicUnits.formatCoinAmount()} ${context.vbkTokenName}")
                } else {
                    information.add("FAIL - The ${context.vbkTokenName} PoP wallet does not contain sufficient funds: current balance: ${currentBalance.atomicUnits.formatCoinAmount()} ${context.vbkTokenName}, minimum required: ${minerConfig.maxFee.formatCoinAmount()}, need ${(minerConfig.maxFee - currentBalance.atomicUnits).formatCoinAmount()} more. See: https://wiki.veriblock.org/index.php/Get_VBK")
                }
            } else {
                information.add("FAIL - Unable to determine the ${context.vbkTokenName} PoP balance.")
            }
        } catch(e: StatusRuntimeException) {
            information.add("FAIL - Unable to determine the ${context.vbkTokenName} PoP balance.")
        }

        // Add the diagnostic information
        val diagnosticInfo = DiagnosticUtility.getDiagnosticInfo()
        return DiagnosticInformation(information, diagnosticInfo)
    }
}
