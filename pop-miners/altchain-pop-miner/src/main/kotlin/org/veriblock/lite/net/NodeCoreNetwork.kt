// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.lite.net

import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import org.veriblock.core.contracts.AddressManager
import org.veriblock.core.utilities.createLogger
import org.veriblock.core.utilities.debugError
import org.veriblock.core.utilities.debugWarn
import org.veriblock.lite.core.Balance
import org.veriblock.lite.core.BlockChain
import org.veriblock.lite.core.EmptyEvent
import org.veriblock.lite.core.FullBlock
import org.veriblock.lite.transactionmonitor.TransactionMonitor
import org.veriblock.lite.util.Threading
import org.veriblock.sdk.models.SyncStatus
import org.veriblock.sdk.models.BlockStoreException
import org.veriblock.sdk.models.VBlakeHash
import org.veriblock.sdk.models.VeriBlockBlock
import org.veriblock.sdk.models.VeriBlockPublication
import org.veriblock.sdk.models.VeriBlockTransaction
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
    private val connected = SettableFuture.create<Boolean>()

    val healthyEvent = EmptyEvent()
    val unhealthyEvent = EmptyEvent()
    val healthySyncEvent = EmptyEvent()
    val unhealthySyncEvent = EmptyEvent()

    fun isHealthy(): Boolean =
        healthy.get()

    fun isSynchronized(): Boolean =
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

    fun getBlock(hash: VBlakeHash): FullBlock? {
        return gateway.getBlock(hash.toString())
    }

    fun getNodeCoreSyncStatus() = gateway.getNodeCoreSyncStatus()

    private fun poll() {
        try {
            var nodeCoreSyncStatus: SyncStatus? = null
            // Verify if we can make a connection with the remote NodeCore
            if (gateway.ping()) {
                // At this point the APM<->NodeCore connection is fine
                if (!isHealthy()) {
                    healthy.set(true)
                    healthyEvent.trigger()
                }
                connected.set(true)
                // Verify the remote NodeCore sync status
                nodeCoreSyncStatus = gateway.getNodeCoreSyncStatus()
                if (nodeCoreSyncStatus.isSynchronized) {
                    if (!isSynchronized()) {
                        synchronized.set(true)
                        healthySyncEvent.trigger()
                    }
                } else {
                    if (isSynchronized()) {
                        synchronized.set(false)
                        unhealthySyncEvent.trigger()
                        logger.info { "The connected NodeCore is not synchronized, Local Block: ${nodeCoreSyncStatus.localBlockchainHeight}, Network Block: ${nodeCoreSyncStatus.networkHeight}, Block Difference: ${nodeCoreSyncStatus.blockDifference}, waiting until it synchronizes..." }
                    }
                }
            } else {
                // At this point the APM<->NodeCore can't be established
                if (isHealthy()) {
                    healthy.set(false)
                    unhealthyEvent.trigger()
                }
                if (isSynchronized()) {
                    synchronized.set(false)
                    unhealthySyncEvent.trigger()
                }
            }
            if (isHealthy() && isSynchronized()) {
                // At this point the APM<->NodeCore connection is fine and the remote NodeCore is synchronized so
                // APM can continue with its work
                val lastBlock: VeriBlockBlock = try {
                    gateway.getLastBlock()
                } catch (e: Exception) {
                    logger.debugWarn(e) { "Unable to get the last block from NodeCore" }
                    if (isHealthy()) {
                        healthy.set(false)
                        unhealthyEvent.trigger()
                    }
                    return
                }
                try {
                    val currentChainHead = blockChain.getChainHead()
                    if (currentChainHead == null || currentChainHead != lastBlock) {
                        logger.debug { "New chain head detected!" }
                        reconcileBlockChain(currentChainHead, lastBlock)
                    }
                } catch (e: BlockStoreException) {
                    logger.error(e) { "VeriBlockBlock store exception" }
                }
            } else {
                if (!isHealthy()) {
                    logger.debug { "Cannot proceed: waiting for connection with NodeCore..." }
                } else {
                    if (!isSynchronized()) {
                        logger.debug { "Cannot proceed because NodeCore is not synchronized" }

                        nodeCoreSyncStatus?.let {
                            if (nodeCoreSyncStatus.networkHeight != 0) {
                                logger.debug { "Local Block: ${nodeCoreSyncStatus.localBlockchainHeight}, Network Block: ${nodeCoreSyncStatus.networkHeight}, Block Difference: ${nodeCoreSyncStatus.blockDifference}" }
                            } else {
                                logger.debug { "Still not connected to the network" }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            logger.debugError(e) { "Error when polling NodeCore" }
        }
    }

    // FIXME This implementation not good enough. Use channels.
    suspend fun getVeriBlockPublications(
        operationId: String,
        keystoneHash: String,
        contextHash: String,
        btcContextHash: String
    ): List<VeriBlockPublication> {
        val newBlockChannel = blockChain.newBestBlockChannel.openSubscription()
        logger.info {
            """[$operationId] Successfully subscribed to VTB retrieval event!
                |   - Keystone Hash: $keystoneHash
                |   - VBK Context Hash: $contextHash
                |   - BTC Context Hash: $btcContextHash""".trimMargin()
        }
        logger.info { "[$operationId] Waiting for this operation's VTBs..." }
        // Loop through each new block until we get a not-empty publication list
        for (newBlock in newBlockChannel) {
            // Retrieve VTBs from NodeCore
            val veriBlockPublications = gateway.getVeriBlockPublications(
                keystoneHash, contextHash, btcContextHash
            )
            // If the list is not empty, return it
            if (veriBlockPublications.isNotEmpty()) {
                return veriBlockPublications
            }
        }
        // The new block channel never ends, so this will never happen
        error("Unable to retrieve veriblock publications: the subscription to new blocks has been interrupted")
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
            logger.warn("NodeCore Error", e)
        }
    }

    fun getBalance(): Balance =
        gateway.getBalance(addressManager.defaultAddress.hash)

    fun sendCoins(destinationAddress: String, atomicAmount: Long): List<String> =
        gateway.sendCoins(destinationAddress, atomicAmount)

    fun getDebugVeriBlockPublications(vbkContextHash: String, btcContextHash: String) =
        gateway.getDebugVeriBlockPublications(vbkContextHash, btcContextHash)
}

class BlockDownloadException(message: String) : Exception(message)
