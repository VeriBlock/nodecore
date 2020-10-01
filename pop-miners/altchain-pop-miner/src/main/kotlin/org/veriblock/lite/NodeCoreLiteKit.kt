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
import kotlinx.coroutines.launch
import org.veriblock.core.params.NetworkParameters
import org.veriblock.core.utilities.createLogger
import org.veriblock.lite.core.Context
import org.veriblock.lite.net.NodeCoreGateway
import org.veriblock.lite.net.NodeCoreNetwork
import org.veriblock.lite.transactionmonitor.TM_FILE_EXTENSION
import org.veriblock.lite.transactionmonitor.TransactionMonitor
import org.veriblock.lite.transactionmonitor.loadTransactionMonitor
import org.veriblock.miners.pop.service.MinerConfig
import org.veriblock.sdk.models.Address
import org.veriblock.sdk.models.BlockStoreException
import veriblock.SpvContext
import veriblock.model.DownloadStatusResponse
import veriblock.net.LocalhostDiscovery
import veriblock.util.SpvEventBus
import java.io.File
import java.io.IOException

val logger = createLogger {}

class NodeCoreLiteKit(
    private val config: MinerConfig,
    private val context: Context
) {
    lateinit var spvContext: SpvContext
    lateinit var transactionMonitor: TransactionMonitor
    lateinit var gateway: NodeCoreGateway
    lateinit var network: NodeCoreNetwork

    var beforeNetworkStart: () -> Unit = {}

    fun initialize() {
        if (!context.directory.exists() && !context.directory.mkdirs()) {
            throw IOException("Unable to create directory")
        }

        spvContext = initSpvContext(context.networkParameters)
        gateway = NodeCoreGateway(context.networkParameters, spvContext.spvService)
        transactionMonitor = createOrLoadTransactionMonitor()

        network = NodeCoreNetwork(
            config,
            context,
            gateway,
            transactionMonitor,
            spvContext.addressManager
        )

        transactionMonitor.start()
    }

    fun start() {
        logger.info { "VeriBlock Network: ${context.networkParameters.name}" }

        logger.info { "Send funds to the ${context.vbkTokenName} wallet ${spvContext.addressManager.defaultAddress.hash}" }
        logger.info { "Connecting to NodeCore at ${context.networkParameters.rpcHost}:${context.networkParameters.rpcPort}..." }
        beforeNetworkStart()
        network.startAsync()
    }

    fun shutdown() {
        if (this::network.isInitialized) {
            network.shutdown()
        }
    }

    fun getAddress(): String = spvContext.addressManager.defaultAddress.hash

    private fun createOrLoadTransactionMonitor(): TransactionMonitor {
        val file = File(context.directory, context.filePrefix + TM_FILE_EXTENSION)
        return if (file.exists()) {
            try {
                file.loadTransactionMonitor(context, gateway)
            } catch (e: Exception) {
                throw IOException("Unable to load the transaction monitoring data", e)
            }
        } else {
            val address = Address(spvContext.addressManager.defaultAddress.hash)
            TransactionMonitor(context, gateway, address)
        }
    }

    private fun initSpvContext(networkParameters: NetworkParameters): SpvContext {
        logger.info { "Initializing SPV..." }
        val spvContext = SpvContext()
        spvContext.init(
            networkParameters,
            LocalhostDiscovery(networkParameters)
        )
        spvContext.peerTable.start()
        GlobalScope.launch {
            while (true) {
                val status: DownloadStatusResponse = spvContext.peerTable.getDownloadStatus()
                if (status.downloadStatus.isDiscovering()) {
                    logger.info { "SPV: Waiting for peers response." }
                } else if (status.downloadStatus.isDownloading()) {
                    logger.info { "SPV: Blockchain is downloading. " + status.currentHeight + " / " + status.bestHeight }
                } else {
                    logger.info { "SPV: Blockchain is ready. Current height " + status.currentHeight }
                    break
                }
                delay(5000L)
            }
        }

        return spvContext
    }
}
