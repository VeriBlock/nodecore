// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.shell

import org.veriblock.core.utilities.Configuration
import org.veriblock.lite.core.Context
import org.veriblock.miners.pop.service.MinerService
import org.veriblock.miners.pop.shell.commands.configCommands
import org.veriblock.miners.pop.shell.commands.miningCommands
import org.veriblock.miners.pop.shell.commands.standardCommands
import org.veriblock.miners.pop.shell.commands.walletCommands
import org.veriblock.shell.CommandFactory

fun CommandFactory.configure(
    configuration: Configuration,
    context: Context,
    miner: MinerService
) {
    standardCommands()
    configCommands(configuration)
    miningCommands(miner)
    walletCommands(context, miner)
}
