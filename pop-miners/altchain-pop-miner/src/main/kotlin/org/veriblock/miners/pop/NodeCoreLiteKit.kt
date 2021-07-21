// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop

import org.veriblock.core.wallet.AddressManager
import org.veriblock.miners.pop.storage.VeriBlockBlockStore
import org.veriblock.sdk.models.Address
import org.veriblock.sdk.models.BlockStoreException
import java.io.File
import java.io.IOException
import org.veriblock.core.utilities.createLogger
import org.veriblock.miners.pop.core.ApmContext
import org.veriblock.miners.pop.core.BlockChain
import org.veriblock.miners.pop.net.NodeCoreGateway
import org.veriblock.miners.pop.net.NodeCoreNetwork
import org.veriblock.miners.pop.transactionmonitor.TM_FILE_EXTENSION
import org.veriblock.miners.pop.transactionmonitor.TransactionMonitor
import org.veriblock.miners.pop.transactionmonitor.loadTransactionMonitor

private val logger = createLogger {}

private const val WALLET_FILE_EXTENSION = ".wallet"

class NodeCoreLiteKit(
    private val config: MinerConfig,
    private val context: ApmContext
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

        blockStore = createBlockStore()
        addressManager = loadAddressManager()
        transactionMonitor = createOrLoadTransactionMonitor()
        blockChain = BlockChain(context.networkParameters, blockStore)

        network = NodeCoreNetwork(
            config,
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
            transactionMonitor.onNewBestBlock(it)
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
        network.startAsync()
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
        throw IOException("Unable to initialize VBK block store: ${e.cause?.message}", e)
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
        val addressManager = AddressManager()
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
