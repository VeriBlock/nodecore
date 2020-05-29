// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.shell

import org.veriblock.core.utilities.Configuration
import org.veriblock.lite.NodeCoreLiteKit
import org.veriblock.lite.core.Context
import org.veriblock.miners.pop.service.MinerConfig
import org.veriblock.miners.pop.service.MinerService
import org.veriblock.miners.pop.service.MiningOperationMapperService
import org.veriblock.miners.pop.shell.commands.configCommands
import org.veriblock.miners.pop.shell.commands.debugCommands
import org.veriblock.miners.pop.shell.commands.miningCommands
import org.veriblock.miners.pop.shell.commands.standardCommands
import org.veriblock.miners.pop.shell.commands.walletCommands
import org.veriblock.sdk.alt.plugin.PluginService
import org.veriblock.shell.CommandFactory

fun CommandFactory.configure(
    configuration: Configuration,
    minerConfig: MinerConfig,
    context: Context,
    miner: MinerService,
    pluginService: PluginService,
    nodeCoreLiteKit: NodeCoreLiteKit,
    miningOperationMapperService: MiningOperationMapperService
) {
    standardCommands()
    debugCommands(minerConfig, context, miner, pluginService, nodeCoreLiteKit)
    configCommands(configuration)
    miningCommands(miner, miningOperationMapperService)
    walletCommands(context, miner)
}
