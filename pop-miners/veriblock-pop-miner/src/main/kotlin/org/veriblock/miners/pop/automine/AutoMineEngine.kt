// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.miners.pop.automine

import org.apache.commons.lang3.StringUtils
import org.veriblock.core.utilities.createLogger
import org.veriblock.miners.pop.EventBus
import org.veriblock.miners.pop.NewVeriBlockFoundEventDto
import org.veriblock.miners.pop.VpmConfig
import org.veriblock.miners.pop.service.MinerService
import java.util.concurrent.atomic.AtomicBoolean

private val logger = createLogger {}

class AutoMineEngine(
    configuration: VpmConfig,
    private val minerService: MinerService
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
        logger.info { "AutoMine engine is now running" }
    }

    fun shutdown() {
        EventBus.newVeriBlockFoundEvent.unregister(this)
    }

    private fun onNewVeriBlockFound(event: NewVeriBlockFoundEventDto) {
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
            logger.warn("Error handling new block", e)
        }
    }

    private fun mine(height: Int) {
        val result = minerService.mine(height)
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
