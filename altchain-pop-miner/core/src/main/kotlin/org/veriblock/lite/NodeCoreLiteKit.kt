// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.lite

import org.veriblock.core.contracts.AddressManager
import org.veriblock.core.wallet.DefaultAddressManager
import org.veriblock.lite.core.Balance
import org.veriblock.lite.core.BlockChain
import org.veriblock.lite.core.Context
import org.veriblock.lite.core.Event
import org.veriblock.lite.net.NodeCoreGateway
import org.veriblock.lite.net.NodeCoreNetwork
import org.veriblock.lite.store.VeriBlockBlockStore
import org.veriblock.lite.util.Threading
import org.veriblock.lite.wallet.TM_FILE_EXTENSION
import org.veriblock.lite.wallet.TransactionMonitor
import org.veriblock.lite.wallet.WALLET_FILE_EXTENSION
import org.veriblock.lite.wallet.loadTransactionMonitor
import org.veriblock.sdk.Address
import org.veriblock.sdk.BlockStoreException
import org.veriblock.sdk.createLogger
import java.io.File
import java.io.IOException

val logger = createLogger {}

class NodeCoreLiteKit(
    private val context: Context
) {
    val blockStore: VeriBlockBlockStore
    val blockChain: BlockChain
    val addressManager: AddressManager
    val transactionMonitor: TransactionMonitor
    val network: NodeCoreNetwork

    var beforeNetworkStart: () -> Unit = {}
    val balanceChangedEvent = Event<Balance>()

    init {
        if (!context.directory.exists() && !context.directory.mkdirs()) {
            throw IOException("Unable to create directory")
        }

        try {
            this.blockStore = createBlockStore()
        } catch (e: BlockStoreException) {
            throw IOException("Unable to initialize VBK block store", e)
        }

        addressManager = loadAddressManager()
        transactionMonitor = createOrLoadTransactionMonitor()
        blockChain = BlockChain(context.networkParameters, blockStore)


        network = NodeCoreNetwork(
            NodeCoreGateway(context.networkParameters),
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
                balanceChangedEvent.trigger(network.getBalance())
            }
        }
        blockChain.blockChainReorganizedEvent.register(transactionMonitor) {
            transactionMonitor.onBlockChainReorganized(it.oldBlocks, it.newBlocks)
        }

        logger.info { "Send funds to the VBK wallet ${addressManager.defaultAddress.hash}" }
        logger.info { "Connecting to NodeCore at ${context.networkParameters.adminHost}:${context.networkParameters.adminPort}..." }
        beforeNetworkStart()
        network.startAsync().addListener(Runnable {
            logger.info { "Connected to NodeCore!" }
            balanceChangedEvent.trigger(network.getBalance())
        }, Threading.LISTENER_THREAD)
    }

    fun shutdown() {
        network.shutdown()
    }

    fun getAddress(): String = addressManager.defaultAddress.hash

    @Throws(BlockStoreException::class)
    private fun createBlockStore(): VeriBlockBlockStore {
        val chainFile = File(context.directory, context.filePrefix + ".spvchain")
        return VeriBlockBlockStore(chainFile)
    }

    private fun createOrLoadTransactionMonitor(): TransactionMonitor {
        val file = File(context.directory, context.filePrefix + TM_FILE_EXTENSION)
        return if (file.exists()) {
            file.loadTransactionMonitor()
        } else {
            val address = Address(addressManager.defaultAddress.hash)
            TransactionMonitor(address)
        }
    }

    private fun loadAddressManager(): AddressManager {
        val addressManager = DefaultAddressManager()
        val file = File(context.directory, context.filePrefix + WALLET_FILE_EXTENSION)
        addressManager.load(file)
        if (!file.exists()) {
            addressManager.save()
        }
        return addressManager
    }
}
