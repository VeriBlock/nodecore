// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.shell.commands

import org.veriblock.core.utilities.Configuration
import org.veriblock.miners.pop.automine.AutoMineEngine
import org.veriblock.miners.pop.service.BitcoinService
import org.veriblock.shell.CommandFactory
import org.veriblock.shell.CommandParameter
import org.veriblock.shell.CommandParameterMappers
import org.veriblock.shell.command
import org.veriblock.shell.core.failure
import org.veriblock.shell.core.success

fun CommandFactory.configCommands(
    configuration: Configuration,
    autoMineEngine: AutoMineEngine,
    bitcoinService: BitcoinService
) {
    command(
        name = "List Config",
        form = "listconfig",
        description = "Lists the configuration properties and values"
    ) {
        printInfo("Configuration Properties:")

        val properties = configuration.list()
        for (property in properties) {
            printInfo("    $property")
        }

        success()
    }

    command(
        name = "Set Config",
        form = "setconfig",
        description = "Sets a new value for a config property (needs restart)",
        parameters = listOf(
            CommandParameter(name = "key", mapper = CommandParameterMappers.STRING),
            CommandParameter(name = "value", mapper = CommandParameterMappers.STRING)
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
        description = "Gets the current automine config"
    ) {
        printInfo("Round 1: ${autoMineEngine.config.round1}")
        printInfo("Round 2: ${autoMineEngine.config.round2}")
        printInfo("Round 3: ${autoMineEngine.config.round3}")
        printInfo("Round 4: ${autoMineEngine.config.round4}")
        success()
    }

    command(
        name = "Set Automine Config",
        form = "setautomine",
        description = "Sets the current automine config",
        parameters = listOf(
            CommandParameter(name = "round", mapper = CommandParameterMappers.INTEGER),
            CommandParameter(name = "value", mapper = CommandParameterMappers.BOOLEAN)
        )
    ) {
        val round: Int = getParameter("round")
        val value: Boolean = getParameter("value")
        when (round) {
            1 -> autoMineEngine.config = autoMineEngine.config.copy(round1 = value)
            2 -> autoMineEngine.config = autoMineEngine.config.copy(round2 = value)
            3 -> autoMineEngine.config = autoMineEngine.config.copy(round3 = value)
            4 -> autoMineEngine.config = autoMineEngine.config.copy(round4 = value)
            else -> error("There's no round #$round")
        }

        success()
    }

    command(
        name = "Get BTC Max Fee Config",
        form = "getmaxfee",
        description = "Gets the current BTC max fee config"
    ) {
        printInfo("BTC Max Fee: ${bitcoinService.maxFee}")
        success()
    }

    command(
        name = "Set BTC Fee/KB Config",
        form = "setmaxfee",
        description = "Sets the current BTC max fee config",
        parameters = listOf(
            CommandParameter(name = "value", mapper = CommandParameterMappers.LONG)
        )
    ) {
        val value: Long = getParameter("value")
        bitcoinService.maxFee = value
        success()
    }

    command(
        name = "Get BTC Fee/KB Config",
        form = "getfeeperkb",
        description = "Gets the current BTC fee/KB config"
    ) {
        printInfo("BTC Fee/KB: ${bitcoinService.feePerKb}")
        success()
    }

    command(
        name = "Set BTC Fee/KB Config",
        form = "setfeeperkb",
        description = "Sets the current BTC fee/KB config",
        parameters = listOf(
            CommandParameter(name = "value", mapper = CommandParameterMappers.LONG)
        )
    ) {
        val value: Long = getParameter("value")
        bitcoinService.feePerKb = value
        success()
    }
}
