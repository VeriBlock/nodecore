package org.veriblock.miners.pop.service

import org.veriblock.core.utilities.DiagnosticUtility
import org.veriblock.miners.pop.common.formatBTCFriendlyString
import org.veriblock.sdk.models.DiagnosticInformation

class DiagnosticService(
    private val bitcoinService: BitcoinService,
    private val nodeCoreService: NodeCoreService,
    private val nodeCoreGateway: NodeCoreGateway
) {

    fun collectDiagnosticInformation(): DiagnosticInformation {
        val information = ArrayList<String>()

        // Check the NodeCore status
        if (nodeCoreService.isHealthy()) {
            information.add("SUCCESS - NodeCore connection: Connected")
            val nodeCoreStateInfo = nodeCoreGateway.getNodeCoreStateInfo()
            if (nodeCoreService.isSynchronized()) {
                information.add("SUCCESS - NodeCore synchronization status: Synchronized (LocalHeight=${nodeCoreStateInfo.localBlockchainHeight} NetworkHeight=${nodeCoreStateInfo.networkHeight})")
            } else {
                information.add("FAIL - NodeCore synchronization status: Not synchronized. ${nodeCoreStateInfo.blockDifference} blocks left (LocalHeight=${nodeCoreStateInfo.localBlockchainHeight} NetworkHeight=${nodeCoreStateInfo.networkHeight})")
            }
        } else {
            information.add("FAIL - NodeCore connection: Not connected")
            information.add("FAIL - Unable to determine the NodeCore synchronization status")
        }

        // Check the bitcoinJ status
        if (bitcoinService.blockchainDownloaded()) {
            information.add("SUCCESS - Blockchain download status: Downloaded")
        } else {
            information.add("FAIL - Blockchain download status: Downloading ${bitcoinService.blockchainDownloadPercent}%, ${bitcoinService.blockchainDownloadBlocksToGo} blocks to go")
        }

        // Check the balance
        val maximumTransactionFee = bitcoinService.getMaximumTransactionFee()
        val balance = bitcoinService.getBalance()
        if (!bitcoinService.getBalance().isLessThan(maximumTransactionFee)) {
            information.add("SUCCESS - The BTC PoP wallet contains sufficient funds, current balance: ${balance.formatBTCFriendlyString()}")
        } else {
            information.add("FAIL - The BTC PoP wallet does not contain sufficient funds: current balance: ${balance.formatBTCFriendlyString()}, minimum required: ${maximumTransactionFee.formatBTCFriendlyString()}, need ${maximumTransactionFee.subtract(balance).formatBTCFriendlyString()} more.")
        }

        // Add the diagnostic information
        val diagnosticInfo = DiagnosticUtility.getDiagnosticInfo()
        return DiagnosticInformation(information, diagnosticInfo)
    }
}
