// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.securityinheriting

import org.veriblock.core.utilities.Configuration
import org.veriblock.core.utilities.createLogger
import org.veriblock.miners.pop.Miner
import org.veriblock.sdk.alt.plugin.PluginService

private val logger = createLogger {}

class SecurityInheritingService(
    private val configuration: Configuration,
    private val pluginFactory: PluginService
) {
    private val monitors = HashMap<String, SecurityInheritingMonitor>()

    fun start(miner: Miner) {
        for ((chainId, chain) in pluginFactory.getPlugins()) {
            logger.debug { "Starting $chainId monitor..." }
            val autoMiner = SecurityInheritingMonitor(configuration, chainId, chain)
            autoMiner.start(miner)
            monitors[chainId] = autoMiner
        }
    }

    fun stop() {
        monitors.values.forEach {
            it.stop()
        }
    }

    fun getMonitor(chainId: String) = monitors[chainId]
}
