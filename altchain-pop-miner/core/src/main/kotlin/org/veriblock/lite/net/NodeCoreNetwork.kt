// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.lite.net

import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import org.veriblock.core.contracts.AddressManager
import org.veriblock.lite.core.BlockChain
import org.veriblock.lite.core.Context
import org.veriblock.lite.core.FullBlock
import org.veriblock.lite.core.PublicationSubscription
import org.veriblock.lite.util.Threading
import org.veriblock.lite.wallet.TransactionMonitor
import org.veriblock.sdk.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

private val logger = createLogger {}

class NodeCoreNetwork(
    private val context: Context,
    private val gateway: NodeCoreGateway,
    private val blockChain: BlockChain,
    private val transactionMonitor: TransactionMonitor,
    private val addressManager: AddressManager
) {
    private val healthy = AtomicBoolean(false)
    private val publicationSubscriptions = ConcurrentHashMap<String, PublicationSubscription>()
    private val connected = SettableFuture.create<Boolean>()

    fun isHealthy(): Boolean =
        healthy.get()

    fun startAsync(): ListenableFuture<Boolean> {
        Threading.NODECORE_POLL_THREAD.scheduleWithFixedDelay({
            this.poll()
        }, 1L, 1L, TimeUnit.SECONDS)

        return connected
    }

    fun shutdown() {
        gateway.shutdown()
    }

    fun submitEndorsement(publicationData: ByteArray): VeriBlockTransaction {
        val transaction = gateway.submitEndorsementTransaction(publicationData, addressManager)
        transactionMonitor.commitTransaction(transaction)
        return transaction
    }

    fun addVeriBlockPublicationSubscription(operationId: String, subscription: PublicationSubscription) {
        publicationSubscriptions[operationId] = subscription
    }

    fun removeVeriBlockPublicationSubscription(operationId: String) {
        publicationSubscriptions.remove(operationId)
    }

    fun getBlock(hash: VBlakeHash): FullBlock? {
        return gateway.getBlock(hash.toString())
    }

    private fun poll() {
        try {
            if (isHealthy()) {
                val lastBlock: VeriBlockBlock = try {
                    gateway.getLastBlock()
                } catch (e: Exception) {
                    logger.error("NodeCore Error", e)
                    healthy.set(false)
                    return
                }

                try {
                    val currentChainHead = blockChain.getChainHead()
                    //logger.trace { "Checking chain head... Last Block: ${lastBlock.hash}; Known last block: ${currentChainHead?.hash}" }
                    if (currentChainHead == null || currentChainHead != lastBlock) {
                        logger.debug { "New chain head detected!" }
                        reconcileBlockChain(currentChainHead, lastBlock)
                        pollForVeriBlockPublications()
                    }
                } catch (e: BlockStoreException) {
                    logger.error("VeriBlockBlock store exception", e)
                }

            } else {
                if (gateway.ping()) {
                    healthy.set(true)
                    connected.set(true)
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "Error when polling NodeCore" }
        }
    }

    private fun pollForVeriBlockPublications() {
        logger.debug { "Polling for VeriBlock publications..." }
        for (subscription in publicationSubscriptions.values) {
            val veriBlockPublications = gateway.getVeriBlockPublications(
                subscription.keystoneHash, subscription.contextHash, subscription.btcContextHash
            )
            subscription.trySetResults(veriBlockPublications)
        }
    }

    private fun reconcileBlockChain(previousHead: VeriBlockBlock?, latestBlock: VeriBlockBlock) {
        logger.debug { "Reconciling blockchain..." }
        try {
            if (previousHead == null || latestBlock.previousBlock == previousHead.hash.trimToPreviousBlockSize()) {
                val downloaded = getBlock(latestBlock.hash)
                if (downloaded != null) {
                    blockChain.handleNewBestChain(emptyList(), listOf(downloaded))
                }
                return
            }

            val blockChainDelta = gateway.listChangesSince(previousHead.hash.toString())

            val added = ArrayList<FullBlock>(blockChainDelta.getAdded().size)
            for (block in blockChainDelta.getAdded()) {
                val downloaded = gateway.getBlock(block.hash.toString())
                    ?: throw BlockDownloadException("Unable to download block " + block.hash.toString())

                added.add(downloaded)
            }

            blockChain.handleNewBestChain(blockChainDelta.getRemoved(), added)
        } catch (e: Exception) {
            logger.error("NodeCore Error", e)
        }
    }
}

class BlockDownloadException(message: String) : Exception(message)
