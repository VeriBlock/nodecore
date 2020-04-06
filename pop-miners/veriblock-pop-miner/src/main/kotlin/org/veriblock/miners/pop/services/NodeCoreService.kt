// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.miners.pop.services

import com.google.common.util.concurrent.ThreadFactoryBuilder
import org.veriblock.core.utilities.createLogger
import org.veriblock.miners.pop.EventBus
import org.veriblock.miners.pop.NewVeriBlockFoundEventDto
import org.veriblock.miners.pop.model.BlockStore
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

private val logger = createLogger {}

class NodeCoreService(
    private val nodeCoreGateway: NodeCoreGateway,
    private val blockStore: BlockStore
) {
    private val healthy = AtomicBoolean(false)
    private val synchronized = AtomicBoolean(false)

    private val scheduledExecutorService: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor(
        ThreadFactoryBuilder().setNameFormat("nc-poll").build()
    )

    init {
        scheduledExecutorService.scheduleWithFixedDelay({ poll() }, 5, 1, TimeUnit.SECONDS)
    }

    private fun isHealthy(): Boolean =
        healthy.get()

    private fun isSynchronized(): Boolean =
        synchronized.get()

    fun shutdown() {
        scheduledExecutorService.shutdown()
        nodeCoreGateway.shutdown()
    }

    private fun poll() {
        try {
            if (isHealthy() && isSynchronized()) {
                if (!nodeCoreGateway.isNodeCoreSynchronized()) {
                    synchronized.set(false)
                    logger.info("The connected node is not synchronized")
                    EventBus.nodeCoreDesynchronizedEvent.trigger()
                    return
                }
                val latestBlock = try {
                    nodeCoreGateway.getLastBlock()
                } catch (e: Exception) {
                    logger.error("Unable to get the last block from NodeCore")
                    healthy.set(false)
                    EventBus.nodeCoreUnhealthyEvent.trigger()
                    return
                }
                val chainHead = blockStore.getChainHead()
                if (latestBlock != chainHead) {
                    blockStore.setChainHead(latestBlock)
                    EventBus.newVeriBlockFoundEvent.trigger(NewVeriBlockFoundEventDto(latestBlock, chainHead))
                }
            } else {
                if (nodeCoreGateway.ping()) {
                    if (!isHealthy()) {
                        logger.info("Connected to NodeCore")
                        EventBus.nodeCoreHealthyEvent.trigger()
                    }
                    healthy.set(true)
                    if (nodeCoreGateway.isNodeCoreSynchronized()) {
                        if (!isSynchronized()) {
                            logger.info("The connected node is synchronized")
                            EventBus.nodeCoreSynchronizedEvent.trigger()
                        }
                        synchronized.set(true)
                    } else {
                        if (isSynchronized()) {
                            logger.info("The connected node is not synchronized")
                            EventBus.nodeCoreDesynchronizedEvent.trigger()
                        }
                        synchronized.set(false)
                    }
                } else {
                    if (isHealthy()) {
                        EventBus.nodeCoreUnhealthyEvent.trigger()
                    }
                    if (isSynchronized()) {
                        logger.info("The connected node is not synchronized")
                        EventBus.nodeCoreDesynchronizedEvent.trigger()
                    }
                    healthy.set(false)
                    synchronized.set(false)
                }
            }
        } catch (e: Exception) {
            logger.error("Error while polling NodeCore", e)
        }
    }
}
