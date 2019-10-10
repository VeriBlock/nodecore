// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.shell.commands

import org.veriblock.sdk.Configuration
import org.veriblock.shell.CommandParameter
import org.veriblock.shell.CommandParameterType
import org.veriblock.shell.Shell
import org.veriblock.shell.command
import org.veriblock.shell.core.success

fun Shell.configCommands() {
    command(
        name = "List Config",
        form = "listconfig",
        description = "Lists the current configuration properties and values"
    ) {
        printInfo("Configuration Properties:")

        val properties = Configuration.list()
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
            CommandParameter(name = "key", type = CommandParameterType.STRING, required = true),
            CommandParameter(name = "value", type = CommandParameterType.STRING, required = true)
        )
    ) {
        val key: String = getParameter("key");
        val value: String = getParameter("value");

        Configuration.setProperty(key, value);

        success()
    }
}
