// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.miners.pop.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.bitcoinj.utils.ContextPropagatingThreadFactory
import org.veriblock.core.utilities.createLogger
import org.veriblock.miners.pop.EventBus
import org.veriblock.miners.pop.NewVeriBlockFoundEventDto
import org.veriblock.miners.pop.VpmConfig
import org.veriblock.miners.pop.common.isOnSameNetwork
import org.veriblock.miners.pop.model.BlockStore
import org.veriblock.sdk.models.StateInfo
import org.veriblock.sdk.models.getSynchronizedMessage
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.atomic.AtomicBoolean

private val logger = createLogger {}

class NodeCoreService(
    private val config: VpmConfig,
    private val nodeCoreGateway: NodeCoreGateway,
    private val blockStore: BlockStore,
    private var bitcoinService: BitcoinService
) {
    private var firstPoll: Boolean = true

    private val ready = AtomicBoolean(false)
    private val accessible = AtomicBoolean(false)
    private val synchronized = AtomicBoolean(!config.nodeCoreRpc.performNetworkChecks)
    private val sameNetwork = AtomicBoolean(!config.nodeCoreRpc.performNetworkChecks)

    var latestNodeCoreStateInfo: StateInfo = StateInfo()

    private val scheduledExecutorService: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor(
        ContextPropagatingThreadFactory("nc-poll")
    )

    private val coroutineScope = CoroutineScope(scheduledExecutorService.asCoroutineDispatcher())

    fun initialize() {
        logger.info { "Connecting to NodeCore at ${config.nodeCoreRpc.host}:${config.nodeCoreRpc.port}..." }

        // Launching from the bitcoin context in order to propagate it
        bitcoinService.contextCoroutineScope.launch {
            coroutineScope.launch {
                delay(5000)
                while (true) {
                    poll()
                    delay(1000)
                }
            }
        }
    }

    /**
     * This variable should be true if accessible, sameNetwork and synchronized variables are true as well
     */
    fun isReady(): Boolean =
        ready.get()

    fun isAccessible(): Boolean =
        accessible.get()

    fun isSynchronized(): Boolean =
        synchronized.get()

    fun isOnSameNetwork(): Boolean =
        sameNetwork.get()

    fun shutdown() {
        scheduledExecutorService.shutdown()
        nodeCoreGateway.shutdown()
    }

    private fun poll() {
        try {
            var nodeCoreStateInfo: StateInfo?
            // Verify if we can make a connection with NodeCore
            if (nodeCoreGateway.ping()) {
                // At this point the VPM<->NodeCore connection is fine
                nodeCoreStateInfo = nodeCoreGateway.getNodeCoreStateInfo()
                latestNodeCoreStateInfo = nodeCoreStateInfo

                if (!isAccessible()) {
                    accessible.set(true)
                    EventBus.nodeCoreAccessibleEvent.trigger()
                }

                if (config.nodeCoreRpc.performNetworkChecks) {
                    // Verify the NodeCore configured Network
                    if (nodeCoreStateInfo.networkVersion.isOnSameNetwork(config.bitcoin.network.name)) {
                        if (!isOnSameNetwork()) {
                            sameNetwork.set(true)
                            EventBus.nodeCoreSameNetworkEvent.trigger()
                        }
                    } else {
                        if (isOnSameNetwork() || firstPoll) {
                            sameNetwork.set(false)
                            EventBus.nodeCoreNotSameNetworkEvent.trigger()
                            logger.warn { "The connected NodeCore (${nodeCoreStateInfo.networkVersion}) & VPM (${config.bitcoin.network.name}) are not running on the same configured network" }
                        }
                    }

                    // Verify the NodeCore synchronization status
                    if (nodeCoreStateInfo.isSynchronized) {
                        if (!isSynchronized()) {
                            synchronized.set(true)
                            EventBus.nodeCoreSynchronizedEvent.trigger()
                            logger.info { "The connected NodeCore is synchronized: ${nodeCoreStateInfo.getSynchronizedMessage()}" }
                        }
                    } else {
                        if (isSynchronized() || firstPoll) {
                            synchronized.set(false)
                            EventBus.nodeCoreNotSynchronizedEvent.trigger()
                            logger.info { "The connected NodeCore is not synchronized: ${nodeCoreStateInfo.getSynchronizedMessage()}" }
                        }
                    }
                }
            } else {
                // At this point the VPM<->NodeCore connection can't be established
                latestNodeCoreStateInfo = StateInfo()
                if (isAccessible()) {
                    accessible.set(false)
                    EventBus.nodeCoreNotAccessibleEvent.trigger()
                }
                if (config.nodeCoreRpc.performNetworkChecks) {
                    if (isSynchronized()) {
                        synchronized.set(false)
                        EventBus.nodeCoreNotSynchronizedEvent.trigger()
                    }
                    if (isOnSameNetwork()) {
                        sameNetwork.set(false)
                        EventBus.nodeCoreNotSameNetworkEvent.trigger()
                    }
                }
            }

            if (isAccessible() && isOnSameNetwork() && isSynchronized()) {
                if (!isReady()) {
                    ready.set(true)
                    EventBus.nodeCoreReadyEvent.trigger()
                }

                // At this point the VPM<->NodeCore connection is fine and the NodeCore is synchronized so
                // VPM can continue with its work
                val latestBlock = try {
                    nodeCoreGateway.getLastBlock()
                } catch (e: Exception) {
                    logger.error("Unable to get the last block from NodeCore")
                    accessible.set(false)
                    EventBus.nodeCoreNotAccessibleEvent.trigger()
                    return
                }
                val chainHead = blockStore.getChainHead()
                if (latestBlock != chainHead) {
                    blockStore.setChainHead(latestBlock)
                    EventBus.newVeriBlockFoundEvent.trigger(NewVeriBlockFoundEventDto(latestBlock, chainHead))
                }
            } else {
                if (isReady()) {
                    ready.set(false)
                    EventBus.nodeCoreNotReadyEvent.trigger()
                }
            }
        } catch (e: Exception) {
            logger.error("Error while polling NodeCore", e)
        }
        firstPoll = false
    }
}
