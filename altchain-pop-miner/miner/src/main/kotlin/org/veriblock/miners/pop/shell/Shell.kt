// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.shell

import org.veriblock.lite.NodeCoreLiteKit
import org.veriblock.miners.pop.Miner
import org.veriblock.miners.pop.service.PluginService
import org.veriblock.miners.pop.shell.commands.altchainCommands
import org.veriblock.miners.pop.shell.commands.configCommands
import org.veriblock.miners.pop.shell.commands.miningCommands
import org.veriblock.miners.pop.shell.commands.standardCommands
import org.veriblock.miners.pop.shell.commands.walletCommands
import org.veriblock.shell.Shell

fun Shell.configure(
    miner: Miner,
    nodeCoreLiteKit: NodeCoreLiteKit,
    pluginService: PluginService
) {
    standardCommands()
    configCommands()
    miningCommands(miner)
    walletCommands(miner)
    altchainCommands(nodeCoreLiteKit, pluginService)
}
