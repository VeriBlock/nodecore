// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.shell.commands

import org.veriblock.core.utilities.Configuration
import org.veriblock.shell.CommandFactory
import org.veriblock.shell.CommandParameter
import org.veriblock.shell.CommandParameterMappers
import org.veriblock.shell.command
import org.veriblock.shell.core.success

fun CommandFactory.configCommands(
    configuration: Configuration
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
        description = "Sets a new value for a config property",
        parameters = listOf(
            CommandParameter(name = "key", mapper = CommandParameterMappers.STRING, required = true),
            CommandParameter(name = "value", mapper = CommandParameterMappers.STRING, required = true)
        )
    ) {
        val key: String = getParameter("key");
        val value: String = getParameter("value");

        configuration.setProperty(key, value);

        success()
    }
}
