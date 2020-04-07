// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.lite

import org.veriblock.core.contracts.AddressManager
import org.veriblock.core.utilities.createLogger
import org.veriblock.core.wallet.DefaultAddressManager
import org.veriblock.lite.core.Balance
import org.veriblock.lite.core.BlockChain
import org.veriblock.lite.core.Context
import org.veriblock.lite.core.Event
import org.veriblock.lite.net.NodeCoreGatewayImpl
import org.veriblock.lite.net.NodeCoreNetwork
import org.veriblock.lite.store.VeriBlockBlockStore
import org.veriblock.lite.transactionmonitor.TM_FILE_EXTENSION
import org.veriblock.lite.transactionmonitor.TransactionMonitor
import org.veriblock.lite.transactionmonitor.loadTransactionMonitor
import org.veriblock.lite.util.Threading
import org.veriblock.sdk.models.Address
import org.veriblock.sdk.models.BlockStoreException
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
    val balanceChangedEvent = Event<Balance>()

    fun initialize() {
        if (!context.directory.exists() && !context.directory.mkdirs()) {
            throw IOException("Unable to create directory")
        }

        this.blockStore = createBlockStore()
        addressManager = loadAddressManager()
        transactionMonitor = createOrLoadTransactionMonitor()
        blockChain = BlockChain(context.networkParameters, blockStore)

        network = NodeCoreNetwork(
            NodeCoreGatewayImpl(context.networkParameters),
            blockChain,
            transactionMonitor,
            addressManager
        )
    }

    fun start() {
        logger.info { "VeriBlock Network: ${context.networkParameters.network}" }

        blockChain.newBestBlockEvent.register(transactionMonitor) {
            val balanceChanged = transactionMonitor.onNewBestBlock(it)
            if (balanceChanged) {
                if (network.isHealthy()) {
                    balanceChangedEvent.trigger(network.getBalance())
                }
            }
        }
        blockChain.blockChainReorganizedEvent.register(transactionMonitor) {
            transactionMonitor.onBlockChainReorganized(it.oldBlocks, it.newBlocks)
        }
        blockChain.blockChainReorganizedEvent.register(this) {
            for (newBlock in it.newBlocks) {
                blockChain.newBestBlockEvent.trigger(newBlock)
                blockChain.newBestBlockChannel.offer(newBlock)
            }
        }

        logger.info { "Send funds to the ${context.vbkTokenName} wallet ${addressManager.defaultAddress.hash}" }
        logger.info { "Connecting to NodeCore at ${context.networkParameters.adminHost}:${context.networkParameters.adminPort}..." }
        beforeNetworkStart()
        network.startAsync().addListener(Runnable {
            if (network.isHealthy()) {
                balanceChangedEvent.trigger(network.getBalance())
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
}
