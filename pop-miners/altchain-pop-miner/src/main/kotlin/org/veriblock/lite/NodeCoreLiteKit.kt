// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.lite

import org.veriblock.core.contracts.AddressManager
import org.veriblock.core.params.NetworkParameters
import org.veriblock.core.utilities.createLogger
import org.veriblock.core.wallet.DefaultAddressManager
import org.veriblock.lite.core.BlockChain
import org.veriblock.lite.core.Context
import org.veriblock.lite.net.NodeCoreGateway
import org.veriblock.lite.net.NodeCoreNetwork
import org.veriblock.lite.store.VeriBlockBlockStore
import org.veriblock.lite.transactionmonitor.TM_FILE_EXTENSION
import org.veriblock.lite.transactionmonitor.TransactionMonitor
import org.veriblock.lite.transactionmonitor.loadTransactionMonitor
import org.veriblock.lite.util.Threading
import org.veriblock.miners.pop.EventBus
import org.veriblock.sdk.models.Address
import org.veriblock.sdk.models.BlockStoreException
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
    lateinit var blockStore: VeriBlockBlockStore
    lateinit var blockChain: BlockChain
    lateinit var addressManager: AddressManager
    lateinit var transactionMonitor: TransactionMonitor
    lateinit var network: NodeCoreNetwork

    var beforeNetworkStart: () -> Unit = {}

    fun initialize() {
        if (!context.directory.exists() && !context.directory.mkdirs()) {
            throw IOException("Unable to create directory")
        }

        this.blockStore = createBlockStore()
        addressManager = loadAddressManager()
        transactionMonitor = createOrLoadTransactionMonitor()
        blockChain = BlockChain(context.networkParameters, blockStore)

        network = NodeCoreNetwork(
            context,
            NodeCoreGateway(context.networkParameters),
            blockChain,
            transactionMonitor,
            addressManager
        )
    }

    fun start() {
        logger.info { "VeriBlock Network: ${context.networkParameters.name}" }

        EventBus.newBestBlockEvent.register(transactionMonitor) {
            val balanceChanged = transactionMonitor.onNewBestBlock(it)
            if (balanceChanged) {
                if (network.isHealthy()) {
                    EventBus.balanceChangedEvent.trigger(network.getBalance())
                }
            }
        }
        EventBus.blockChainReorganizedEvent.register(transactionMonitor) {
            transactionMonitor.onBlockChainReorganized(it.oldBlocks, it.newBlocks)
        }
        EventBus.blockChainReorganizedEvent.register(this) {
            for (newBlock in it.newBlocks) {
                EventBus.newBestBlockEvent.trigger(newBlock)
                EventBus.newBestBlockChannel.offer(newBlock)
            }
        }

        logger.info { "Send funds to the ${context.vbkTokenName} wallet ${addressManager.defaultAddress.hash}" }
        logger.info { "Connecting to NodeCore at ${context.networkParameters.rpcHost}:${context.networkParameters.rpcPort}..." }
        beforeNetworkStart()
        network.startAsync().addListener(Runnable {
            if (network.isHealthy()) {
                val balance = try {
                    network.getBalance()
                } catch (ignored: Exception) {
                    return@Runnable
                }
                EventBus.balanceChangedEvent.trigger(balance)
            }
        }, Threading.LISTENER_THREAD)
    }

    fun shutdown() {
        if (this::network.isInitialized) {
            network.shutdown()
        }
    }

    fun getAddress(): String = addressManager.defaultAddress.hash

    private fun createBlockStore(): VeriBlockBlockStore = try {
        val chainFile = File(context.directory, context.filePrefix + ".spvchain")
        VeriBlockBlockStore(chainFile)
    } catch (e: BlockStoreException) {
        throw IOException("Unable to initialize VBK block store", e)
    }

    private fun createOrLoadTransactionMonitor(): TransactionMonitor {
        val file = File(context.directory, context.filePrefix + TM_FILE_EXTENSION)
        return if (file.exists()) {
            try {
                file.loadTransactionMonitor(context)
            } catch (e: Exception) {
                throw IOException("Unable to load the transaction monitoring data", e)
            }
        } else {
            val address = Address(addressManager.defaultAddress.hash)
            TransactionMonitor(context, address)
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

    private fun initSpvContext(networkParameters: NetworkParameters, addresses: Collection<org.veriblock.core.wallet.Address>): SpvContext {
        val spvContext = SpvContext()
        spvContext.init(
            networkParameters,
            LocalhostDiscovery(networkParameters)
        )
        spvContext.peerTable.start()

        for (address in addresses) {
            spvContext.addressManager.monitor(address)
        }

        logger.info { "Initialize SPV: " }
        while (true) {
            val status: DownloadStatusResponse = spvContext.peerTable.getDownloadStatus()
            if (status.downloadStatus.isDiscovering()) {
                logger.info { "Waiting for peers response." }
            } else if (status.downloadStatus.isDownloading()) {
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
