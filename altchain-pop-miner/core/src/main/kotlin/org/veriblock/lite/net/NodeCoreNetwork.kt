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
import org.veriblock.lite.core.Balance
import org.veriblock.lite.core.BlockChain
import org.veriblock.lite.core.EmptyEvent
import org.veriblock.lite.core.FullBlock
import org.veriblock.lite.core.PublicationSubscription
import org.veriblock.lite.util.Threading
import org.veriblock.lite.wallet.TransactionMonitor
import org.veriblock.sdk.BlockStoreException
import org.veriblock.sdk.VBlakeHash
import org.veriblock.sdk.VeriBlockBlock
import org.veriblock.sdk.VeriBlockTransaction
import org.veriblock.sdk.createLogger
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

private val logger = createLogger {}

class NodeCoreNetwork(
    private val gateway: NodeCoreGateway,
    private val blockChain: BlockChain,
    private val transactionMonitor: TransactionMonitor,
    private val addressManager: AddressManager
) {
    private val healthy = AtomicBoolean(false)
    private val synchronized = AtomicBoolean(false)
    private val publicationSubscriptions = ConcurrentHashMap<String, PublicationSubscription>()
    private val connected = SettableFuture.create<Boolean>()
    private var firstPollAttempt = true

    val healthyEvent = EmptyEvent()
    val unhealthyEvent = EmptyEvent()
    val healthySyncEvent = EmptyEvent()
    val unhealthySyncEvent = EmptyEvent()

    fun isHealthy(): Boolean =
        healthy.get()

    private fun isSynchronized(): Boolean =
        synchronized.get()

    fun startAsync(): ListenableFuture<Boolean> {
        Threading.NODECORE_POLL_THREAD.scheduleWithFixedDelay({
            this.poll()
        }, 1L, 1L, TimeUnit.SECONDS)

        return connected
    }

    fun shutdown() {
        gateway.shutdown()
    }

    fun submitEndorsement(publicationData: ByteArray, feePerByte: Long, maxFee: Long): VeriBlockTransaction {
        val transaction = gateway.submitEndorsementTransaction(
            publicationData, addressManager, feePerByte, maxFee
        )
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
            if (isHealthy() && isSynchronized()) {
                if (!gateway.isNodeCoreSynchronized()) {
                    unhealthySyncEvent.trigger()
                    synchronized.set(false)
                    return
                }

                val lastBlock: VeriBlockBlock = try {
                    gateway.getLastBlock()
                } catch (e: Exception) {
                    logger.error("NodeCore Error", e)
                    unhealthyEvent.trigger()
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
                    if (!isHealthy()) {
                        healthyEvent.trigger()
                    }
                    healthy.set(true)
                    connected.set(true)

                    if (gateway.isNodeCoreSynchronized()) {
                        if (!isSynchronized()) {
                            healthySyncEvent.trigger()
                        }
                        synchronized.set(true)
                    } else {
                        if (isSynchronized()) {
                            unhealthySyncEvent.trigger()
                        }
                        synchronized.set(false)
                    }
                } else {
                    if (isHealthy() || firstPollAttempt) {
                        unhealthyEvent.trigger()
                    }
                    if (isSynchronized()) {
                        unhealthySyncEvent.trigger()
                    }
                    healthy.set(false)
                    synchronized.set(false)
                }
            }
            firstPollAttempt = false
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
        logger.debug { "Reconciling VBK blockchain..." }
        try {
            val tooFarBehind = previousHead != null && latestBlock.height - previousHead.height > 500
            if (tooFarBehind) {
                logger.warn { "Attempting to reconcile VBK blockchain with a too long block gap. All blocks will be skipped." }
                blockChain.reset()
            }
            if (previousHead == null || latestBlock.previousBlock == previousHead.hash.trimToPreviousBlockSize() || tooFarBehind) {
                val downloaded = getBlock(latestBlock.hash)
                if (downloaded != null) {
                    blockChain.handleNewBestChain(emptyList(), listOf(downloaded))
                }
                return
            }

            val blockChainDelta = gateway.listChangesSince(previousHead.hash.toString())

            val added = ArrayList<FullBlock>(blockChainDelta.added.size)
            for (block in blockChainDelta.added) {
                val downloaded = gateway.getBlock(block.hash.toString())
                    ?: throw BlockDownloadException("Unable to download VBK block " + block.hash.toString())

                added.add(downloaded)
            }

            blockChain.handleNewBestChain(blockChainDelta.removed, added)
        } catch (e: Exception) {
            logger.error("NodeCore Error", e)
        }
    }

    fun getBalance(): Balance =
        gateway.getBalance(addressManager.defaultAddress.hash)

    fun getDebugVeriBlockPublications(vbkContextHash: String, btcContextHash: String) =
        gateway.getDebugVeriBlockPublications(vbkContextHash, btcContextHash)
}

class BlockDownloadException(message: String) : Exception(message)
