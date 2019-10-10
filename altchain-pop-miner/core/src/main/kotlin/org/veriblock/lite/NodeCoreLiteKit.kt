// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.lite

import org.veriblock.lite.core.BlockChain
import org.veriblock.lite.core.Context
import org.veriblock.lite.net.NodeCoreGateway
import org.veriblock.lite.net.NodeCoreNetwork
import org.veriblock.lite.params.NetworkParameters
import org.veriblock.lite.store.BlockStore
import org.veriblock.lite.store.StoredVeriBlockBlock
import org.veriblock.lite.store.VeriBlockBlockStoreImpl
import org.veriblock.lite.util.Threading
import org.veriblock.lite.wallet.WALLET_FILE_EXTENSION
import org.veriblock.lite.wallet.Wallet
import org.veriblock.lite.wallet.loadWallet
import org.veriblock.sdk.BlockStoreException
import org.veriblock.sdk.VBlakeHash
import java.io.File
import java.io.IOException

class NodeCoreLiteKit(
        private val context: Context
) {
    private val params: NetworkParameters = context.networkParameters

    lateinit var blockStore: BlockStore<VBlakeHash, StoredVeriBlockBlock>
        private set

    lateinit var blockChain: BlockChain
        private set

    lateinit var wallet: Wallet
        private set

    lateinit var network: NodeCoreNetwork
        private set

    var beforeNetworkStart: Runnable? = null
    var afterNetworkStart: Runnable? = null

    @Throws(IOException::class)
    fun start() {
        if (!context.directory.exists() && !context.directory.mkdirs()) {
            throw IOException("Unable to create directory")
        }

        try {
            this.blockStore = createBlockStore()
        } catch (e: BlockStoreException) {
            throw IOException("Unable to initialize block store", e)
        }

        wallet = createOrLoadWallet()
        blockChain = BlockChain(context.networkParameters, blockStore).apply {
            newBestBlockEvent.register(wallet) {
                wallet.onNewBestBlock(it)
            }
            blockChainReorganizedEvent.register(wallet) {
                wallet.onBlockChainReorganized(it.oldBlocks, it.newBlocks)
            }
        }

        beforeNetworkStart?.run()

        network = NodeCoreNetwork(context, NodeCoreGateway(context.networkParameters), blockChain, wallet).apply {
            val connected = startAsync()
            connected.addListener(Runnable{
                afterNetworkStart?.run()
            }, Threading.LISTENER_THREAD)
        }
    }

    fun shutdown() {

    }

    @Throws(BlockStoreException::class)
    private fun createBlockStore(): BlockStore<VBlakeHash, StoredVeriBlockBlock> {
        val chainFile = File(context.directory, context.filePrefix + ".spvchain")
        return VeriBlockBlockStoreImpl(chainFile)
    }

    private fun createOrLoadWallet(): Wallet {
        val walletFile = File(context.directory, context.filePrefix + WALLET_FILE_EXTENSION)
        return if (walletFile.exists()) {
            loadWallet(walletFile)
        } else {
            Wallet()
        }
    }

    private fun loadWallet(walletFile: File): Wallet = walletFile.loadWallet()
}
