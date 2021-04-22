// VeriBlock PoP Miner
// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.miners.pop.automine

import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.StringUtils
import org.veriblock.core.utilities.createLogger
import org.veriblock.core.utilities.debugWarn
import org.veriblock.miners.pop.EventBus
import org.veriblock.miners.pop.NewVeriBlockFoundEventDto
import org.veriblock.miners.pop.VpmConfig
import org.veriblock.miners.pop.service.BitcoinService
import org.veriblock.miners.pop.service.MinerService
import org.veriblock.miners.pop.service.NodeCoreService
import java.util.concurrent.atomic.AtomicBoolean

private val logger = createLogger {}

class AutoMineEngine(
    configuration: VpmConfig,
    private val minerService: MinerService,
    private val nodeCoreService: NodeCoreService,
    private val bitcoinService: BitcoinService
) {
    private val running = AtomicBoolean(false)

    var config = configuration.autoMine

    val conditions = listOf(
        keystoneBlockCondition,
        round1Condition,
        round2Condition,
        round3Condition
    )

    fun run() {
        EventBus.newVeriBlockFoundEvent.register(this, ::onNewVeriBlockFound)
        running.set(true)
        logger.info { "AutoMine engine is now running, waiting for the miner to be ready" }
    }

    fun shutdown() {
        EventBus.newVeriBlockFoundEvent.unregister(this)
    }

    private fun onNewVeriBlockFound(event: NewVeriBlockFoundEventDto) {
        if (!nodeCoreService.isReady() || !bitcoinService.isServiceReady() || !bitcoinService.isBlockchainDownloaded()
            || !bitcoinService.isSufficientlyFunded()) {
            logger.debug { "The miner is not ready, skipping the auto mine for the block height ${event.block.getHeight()}" }
            return
        }
        try {
            val previousHead = event.previousHead
            val latestBlock = event.block
            val end = latestBlock.getHeight()
            var start = end
            if (previousHead != null) {
                start = previousHead.getHeight() + 1
            }
            for (height in start..end) {
                for (condition in conditions) {
                    if (condition(config, height)) {
                        mine(height)
                    }
                }
            }
        } catch (e: Exception) {
            logger.debugWarn(e) { "Error handling new block" }
        }
    }

    private fun mine(height: Int) {
        val result = runBlocking {
            minerService.mine(height)
        }
        if (result.didFail()) {
            val errorMessage = StringBuilder()
            for (message in result.messages) {
                errorMessage.append(System.lineSeparator())
                    .append(message.message)
                    .append(": ")
                    .append(StringUtils.join(message.details, "; "))
            }

            logger.warn { "Mine Action Failed: $errorMessage" }
        }
    }
}
