// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.securityinheriting

import org.koin.core.KoinComponent
import org.veriblock.core.utilities.Configuration
import org.veriblock.core.utilities.createLogger
import org.veriblock.miners.pop.core.ApmContext
import org.veriblock.miners.pop.service.AltchainPopMinerService
import org.veriblock.sdk.alt.plugin.PluginService

private val logger = createLogger {}

class SecurityInheritingService(
    context: ApmContext,
    configuration: Configuration,
    pluginService: PluginService
) : KoinComponent {
    private val monitors by lazy {
        pluginService.getPlugins().entries.associate { (chainId, chain) ->
            chainId to SecurityInheritingMonitor(context, configuration, chainId, chain)
        }
    }

    fun start(miner: AltchainPopMinerService) {
        for ((chainId, monitor) in monitors) {
            logger.debug { "Starting $chainId monitor..." }
            monitor.start(miner)
        }
    }

    fun stop() {
        monitors.values.forEach {
            it.stop()
        }
    }

    fun getMonitor(chainId: String) = monitors[chainId]
}
