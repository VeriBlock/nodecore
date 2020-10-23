// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.shell

import org.veriblock.core.utilities.Configuration
import org.veriblock.miners.pop.core.ApmContext
import org.veriblock.miners.pop.service.AltchainPopMinerService
import org.veriblock.miners.pop.service.ApmOperationExplainer
import org.veriblock.miners.pop.service.DiagnosticService
import org.veriblock.miners.pop.MinerConfig
import org.veriblock.miners.pop.shell.commands.configCommands
import org.veriblock.miners.pop.shell.commands.diagnosticCommands
import org.veriblock.miners.pop.shell.commands.miningCommands
import org.veriblock.miners.pop.shell.commands.standardCommands
import org.veriblock.miners.pop.shell.commands.walletCommands
import org.veriblock.sdk.alt.plugin.PluginService
import org.veriblock.shell.CommandFactory

fun CommandFactory.configure(
    configuration: Configuration,
    minerConfig: MinerConfig,
    context: ApmContext,
    minerService: AltchainPopMinerService,
    pluginService: PluginService,
    diagnosticService: DiagnosticService,
    apmOperationExplainer: ApmOperationExplainer
) {
    standardCommands()
    diagnosticCommands(diagnosticService)
    configCommands(configuration, minerConfig, pluginService)
    miningCommands(minerService, pluginService, apmOperationExplainer)
    walletCommands(context, minerService)
}
