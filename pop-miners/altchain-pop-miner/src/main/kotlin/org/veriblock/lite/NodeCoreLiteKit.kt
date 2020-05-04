// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.lite

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.veriblock.core.contracts.AddressManager
import org.veriblock.core.utilities.createLogger
import org.veriblock.core.wallet.DefaultAddressManager
import org.veriblock.lite.core.Balance
import org.veriblock.lite.core.Context
import org.veriblock.lite.core.Event
import org.veriblock.lite.net.GatewayStrategy
import org.veriblock.lite.net.NodeCoreGateway
import org.veriblock.lite.net.NodeCoreNetwork
import org.veriblock.lite.net.createFullNode
import org.veriblock.lite.net.createSpv
import org.veriblock.lite.params.NetworkParameters
import org.veriblock.lite.transactionmonitor.TM_FILE_EXTENSION
import org.veriblock.lite.transactionmonitor.TransactionMonitor
import org.veriblock.lite.transactionmonitor.loadTransactionMonitor
import org.veriblock.miners.pop.service.sec
import org.veriblock.sdk.models.Address
import veriblock.SpvContext
import veriblock.model.DownloadStatusResponse
import veriblock.net.LocalhostDiscovery
import java.io.File
import java.io.IOException

val logger = createLogger {}

private const val WALLET_FILE_EXTENSION = ".wallet"

class NodeCoreLiteKit(
    private val context: Context
) {
    lateinit var addressManager: AddressManager
    lateinit var transactionMonitor: TransactionMonitor
    lateinit var network: NodeCoreNetwork
    lateinit var gateway: NodeCoreGateway
    lateinit var gatewayStrategy: GatewayStrategy
    lateinit var spvContext: SpvContext

    var beforeNetworkStart: () -> Unit = {}
    val balanceChangedEvent = Event<Balance>()
    private var lastKnownBalance: Balance? = null

    fun initialize() {
        if (!context.directory.exists() && !context.directory.mkdirs()) {
            throw IOException("Unable to create directory")
        }

        addressManager = loadAddressManager()

        if (context.networkParameters.isSpv) {
            spvContext = initSpvContest(context.networkParameters, addressManager.all)
            gatewayStrategy = createSpv(spvContext)
        } else {
            gatewayStrategy = createFullNode(context.networkParameters)
        }

        gateway = NodeCoreGateway(context.networkParameters, gatewayStrategy)
        transactionMonitor = createOrLoadTransactionMonitor()

        network = NodeCoreNetwork(
            gateway,
            transactionMonitor,
            addressManager
        )

        transactionMonitor.start()
    }

    fun start() {
        logger.info { "VeriBlock Network: ${context.networkParameters.network}" }
        logger.info { "Send funds to the ${context.vbkTokenName} wallet ${addressManager.defaultAddress.hash}" }
        logger.info { "Connecting to NodeCore at ${context.networkParameters.adminHost}:${context.networkParameters.adminPort}..." }
        beforeNetworkStart()

        network.startAsync()
        balanceUpdater()
    }

    fun shutdown() {
        gatewayStrategy.shutdown()
    }

    fun balanceUpdater() {
        GlobalScope.launch {
            while (isActive) {
                try {
                    updateBalance()
                } catch (e: Exception) {
                    logger.error(e) { e.message }
                }
                delay(60.sec.toMillis())
            }
        }
    }

    fun updateBalance() {
        val balance = gateway.getBalance(addressManager.defaultAddress.hash)
        if (lastKnownBalance == null || lastKnownBalance != balance) {
            balanceChangedEvent.trigger(balance)
            lastKnownBalance = balance
        }
    }

    fun getAddress(): String = addressManager.defaultAddress.hash

    private fun createOrLoadTransactionMonitor(): TransactionMonitor {
        val file = File(context.directory, context.filePrefix + TM_FILE_EXTENSION)
        return if (file.exists()) {
            try {
                file.loadTransactionMonitor(context, gateway)
            } catch (e: Exception) {
                throw IOException("Unable to load the transaction monitoring data", e)
            }
        } else {
            val address = Address(addressManager.defaultAddress.hash)
            TransactionMonitor(context, address, gateway)
        }
    }

    private fun loadAddressManager(): AddressManager = try {
        val addressManager = DefaultAddressManager()
        val file = File(context.directory, context.filePrefix + WALLET_FILE_EXTENSION)
        addressManager.load(file)
        if (!file.exists()) {
            addressManager.save()
        }
        addressManager
    } catch (e: Exception) {
        throw IOException("Unable to load the address manager", e)
    }

    private fun initSpvContest(networkParameters: NetworkParameters, addresses: Collection<org.veriblock.core.wallet.Address>): SpvContext {
        val spvContext = SpvContext()
        spvContext.init(
            networkParameters.spvNetworkParameters,
            LocalhostDiscovery(networkParameters.spvNetworkParameters), false
        )
        spvContext.peerTable.start()

        for (address in addresses) {
            spvContext.addressManager.monitor(address)
        }

        logger.info { "Initialize SPV: " }
        while (true) {
            val status: DownloadStatusResponse = spvContext.peerTable.downloadStatus
            if (status.downloadStatus.isDiscovering) {
                logger.info { "Waiting for peers response." }
            } else if (status.downloadStatus.isDownloading) {
                logger.info { "Blockchain is downloading. " + status.currentHeight + " / " + status.bestHeight }
            } else {
                logger.info { "Blockchain is ready. Current height " + status.currentHeight }
                break
            }
            Thread.sleep(5000L)
        }
        return spvContext
    }
}
