// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
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
    val suggestedCommands: List<String> = emptyList(),
    val extraData: String? = null,
    val action: (CommandContext) -> Result
)

class CommandParameter(
    val name: String,
    val type: CommandParameterType,
    val required: Boolean = true
)

enum class CommandParameterType {
    STRING,
    INTEGER,
    LONG,
    AMOUNT,
    PEER,
    STANDARD_ADDRESS,
    COMMA_SEPARATED_STANDARD_ADDRESSES,
    STANDARD_OR_MULTISIG_ADDRESS,
    HASH
}

fun Shell.command(
    name: String,
    form: String,
    description: String,
    parameters: List<CommandParameter> = emptyList(),
    suggestedCommands: List<String> = emptyList(),
    action: CommandContext.() -> Result
) {
    val command = Command(name, form, description, parameters, suggestedCommands, action)
    registerCommand(command)
}
