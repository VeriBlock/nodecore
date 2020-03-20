// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.shell

import org.veriblock.shell.core.Result

class Command(
    val name: String,
    val form: String,
    val description: String,
    val parameters: List<CommandParameter>,
    val suggestedCommands: () -> List<String> = { emptyList() },
    val extraData: String? = null,
    val action: (CommandContext) -> Result
) {
    override fun toString() = form
}

class CommandParameter(
    val name: String,
    val mapper: CommandParameterMapper,
    val required: Boolean = true
) {
    override fun toString() = name
}

fun CommandFactory.command(
    name: String,
    form: String,
    description: String,
    parameters: List<CommandParameter> = emptyList(),
    suggestedCommands: () -> List<String> = { emptyList() },
    extraData: String? = null,
    action: CommandContext.() -> Result
) {
    val command = Command(name, form, description, parameters, suggestedCommands, extraData, action)
    registerCommand(command)
}
