package org.veriblock.miners.pop.service

import org.veriblock.core.utilities.DiagnosticUtility
import org.veriblock.miners.pop.VpmConfig
import org.veriblock.miners.pop.common.formatBTCFriendlyString
import org.veriblock.sdk.models.DiagnosticInformation
import org.veriblock.sdk.models.getSynchronizedMessage

class DiagnosticService(
    private val config: VpmConfig,
    private val bitcoinService: BitcoinService,
    private val nodeCoreService: NodeCoreService
) {

    fun collectDiagnosticInformation(): DiagnosticInformation {
        val information = ArrayList<String>()

        // Check the NodeCore status
        if (nodeCoreService.isAccessible()) {
            information.add("SUCCESS - NodeCore connection: Connected to ${config.nodeCoreRpc.host}:${config.nodeCoreRpc.port}")
            // Configured network
            if (nodeCoreService.isOnSameNetwork()) {
                information.add("SUCCESS - NodeCore configured network: NodeCore & VPM are running on the same configured network (${config.bitcoin.network.name})")
            } else {
                information.add("FAIL - NodeCore configured network: NodeCore (${nodeCoreService.latestNodeCoreStateInfo.networkVersion}) & VPM (${config.bitcoin.network.name}) are not running on the same configured network")
            }
            // Synchronization
            if (nodeCoreService.isSynchronized()) {
                information.add("SUCCESS - NodeCore synchronization status: Synchronized ${nodeCoreService.latestNodeCoreStateInfo.getSynchronizedMessage()}")
            } else {
                information.add("FAIL - NodeCore synchronization status: Not synchronized. ${nodeCoreService.latestNodeCoreStateInfo.getSynchronizedMessage()}")
            }
        } else {
            information.add("FAIL - NodeCore connection: Not connected to ${config.nodeCoreRpc.host}:${config.nodeCoreRpc.port}")
            information.add("FAIL - NodeCore unable to determine the synchronization status")
            information.add("FAIL - NodeCore unable to determine the configured network")
        }

        // Check the Bitcoin status
        if (bitcoinService.isServiceReady()) {
            information.add("SUCCESS - Bitcoin service is ready")
        } else {
            information.add("FAIL - Bitcoin service is not ready")
        }
        if (bitcoinService.isBlockchainDownloaded()) {
            information.add("SUCCESS - Blockchain download status: Downloaded ${bitcoinService.blockchainDownloadPercent}%, ${bitcoinService.blockchainDownloadBlocksToGo} blocks to go")
        } else {
            information.add("FAIL - Blockchain download status: Downloading ${bitcoinService.blockchainDownloadPercent}%, ${bitcoinService.blockchainDownloadBlocksToGo} blocks to go")
        }

        // Check the balance
        val balance = bitcoinService.getBalance()
        if (bitcoinService.isSufficientlyFunded()) {
            information.add("SUCCESS - The BTC PoP wallet contains sufficient funds, current balance: ${balance.formatBTCFriendlyString()}")
        } else {
            val maximumTransactionFee = bitcoinService.getMaximumTransactionFee()
            information.add("FAIL - The BTC PoP wallet does not contain sufficient funds: current balance: ${balance.formatBTCFriendlyString()}, minimum required: ${maximumTransactionFee.formatBTCFriendlyString()}, need ${maximumTransactionFee.subtract(balance).formatBTCFriendlyString()} more.")
        }

        // Add the diagnostic information
        val diagnosticInfo = DiagnosticUtility.getDiagnosticInfo()
        return DiagnosticInformation(information, diagnosticInfo)
    }
}
