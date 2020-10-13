// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.shell.commands

import org.veriblock.core.CommandException
import org.veriblock.core.utilities.Configuration
import org.veriblock.miners.pop.MinerConfig
import org.veriblock.sdk.alt.plugin.PluginService
import org.veriblock.shell.CommandFactory
import org.veriblock.shell.CommandParameter
import org.veriblock.shell.CommandParameterMappers
import org.veriblock.shell.command
import org.veriblock.shell.core.failure
import org.veriblock.shell.core.success

fun CommandFactory.configCommands(
    configuration: Configuration,
    minerConfig: MinerConfig,
    pluginService: PluginService
) {
    command(
        name = "List Config",
        form = "listconfig",
        description = "Lists the current configuration properties and values"
    ) {
        printInfo("Configuration Properties:")

        val properties = configuration.list()
        for ((prop, value) in properties) {
            printInfo("\t$prop=$value")
        }

        success()
    }
    command(
        name = "Set Config",
        form = "setconfig",
        description = "Sets a new value for a config property (needs restart)",
        parameters = listOf(
            CommandParameter(name = "key", mapper = CommandParameterMappers.STRING, required = true),
            CommandParameter(name = "value", mapper = CommandParameterMappers.STRING, required = true)
        )
    ) {
        val key: String = getParameter("key")
        val value: String = getParameter("value")

        val configValues = configuration.list()
        if (configValues.containsKey(key)) {
            configuration.setProperty(key, value)
            configuration.saveOverriddenProperties()
            printInfo("NOTE: In order for the changes to apply, please restart the miner.")
            success()
        } else {
            failure {
                addMessage("V400", "Failed to set config", "'${key}' is not part of the configurable properties")
            }
        }
    }

    command(
        name = "Get Automine Config",
        form = "getautomine",
        description = "Gets the current automine config",
        parameters = listOf(
            CommandParameter(name = "chain", mapper = CommandParameterMappers.STRING)
        )
    ) {
        val chainKey: String = getParameter("chain")
        val chain = pluginService[chainKey]
            ?: throw CommandException("There is no plugin with the chain key $chainKey")
        chain.config.availableRoundIndices.forEach {
            printInfo("Round $it: ${chain.config.autoMineRounds.contains(it)}")
        }
        success()
    }

    command(
        name = "Set Automine Config",
        form = "setautomine",
        description = "Sets the current automine config",
        parameters = listOf(
            CommandParameter(name = "chain", mapper = CommandParameterMappers.STRING),
            CommandParameter(name = "round", mapper = CommandParameterMappers.INTEGER),
            CommandParameter(name = "value", mapper = CommandParameterMappers.BOOLEAN)
        )
    ) {
        val chainKey: String = getParameter("chain")
        val chain = pluginService[chainKey]
            ?: throw CommandException("There is no SI chain with the key $chainKey")
        val round: Int = getParameter("round")
        val value: Boolean = getParameter("value")
        if (!chain.config.availableRoundIndices.contains(round)) {
            throw CommandException("Round $round is not defined in the chain's block round indices")
        }
        if (value) {
            chain.config.autoMineRounds.add(round)
        } else {
            chain.config.autoMineRounds.remove(round)
        }
        success()
    }

    command(
        name = "Get VBK Max Fee Config",
        form = "getmaxfee",
        description = "Gets the current VBK max fee config"
    ) {
        printInfo("VBK Max Fee: ${minerConfig.maxFee}")
        success()
    }

    command(
        name = "Set VBK Fee/KB Config",
        form = "setmaxfee",
        description = "Sets the current VBK max fee config",
        parameters = listOf(
            CommandParameter(name = "value", mapper = CommandParameterMappers.LONG)
        )
    ) {
        val value: Long = getParameter("value")
        minerConfig.maxFee = value
        success()
    }

    command(
        name = "Get VBK Fee/Byte Config",
        form = "getfeeperbyte",
        description = "Gets the current VBK fee/Byte config"
    ) {
        printInfo("VBK Fee/KB: ${minerConfig.feePerByte}")
        success()
    }

    command(
        name = "Set VBK Fee/Byte Config",
        form = "setfeeperbyte",
        description = "Sets the current VBK fee/Byte config",
        parameters = listOf(
            CommandParameter(name = "value", mapper = CommandParameterMappers.LONG)
        )
    ) {
        val value: Long = getParameter("value")
        minerConfig.feePerByte = value
        success()
    }
}
