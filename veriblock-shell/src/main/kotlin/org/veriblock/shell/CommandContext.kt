// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.shell

import com.google.gson.GsonBuilder
import org.jline.utils.AttributedStyle

class CommandContext(
    val shell: Shell,
    val command: Command,
    private val parameters: Map<String, Any>
) {
    var quit = false
        private set
    var clear = false
        private set
    val outputFile: String? = getOptionalParameter(FILENAME_SELECTOR)
    val extraData = mutableMapOf<String, Any>()

    @Suppress("UNCHECKED_CAST")
    fun <T> getParameter(name: String) = parameters.getValue(name) as T

    @Suppress("UNCHECKED_CAST")
    fun <T> getOptionalParameter(name: String) = parameters[name] as T?

    fun quit() {
        quit = true
    }

    fun clear() {
        clear = true
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> getExtraData(key: String) = extraData[key] as? T

    fun printInfo(message: String) {
        shell.printInfo(message)
    }

    fun suggestCommands() {
        val suggestedCommands = command.suggestedCommands()
        if (suggestedCommands.isEmpty()) {
            return
        }
        val commandSummaries = suggestedCommands.associateWith {
            shell.getCommand(it).description
        }
        val maxLength = commandSummaries.keys.map {
            it.length
        }.max() ?:0

        shell.printStyled(
            "You may also find the following command(s) useful ('help' for syntax):",
            AttributedStyle.BOLD.foreground(AttributedStyle.MAGENTA)
        )

        val formatPattern = String.format("  %%1$-%ds", maxLength + 1);
        for (key in commandSummaries.keys) {
            shell.printStyled(
                String.format(formatPattern, key),
                AttributedStyle.BOLD.foreground(AttributedStyle.WHITE),
                false
            )
            shell.printStyled(
                "(${commandSummaries[key]})",
                AttributedStyle.BOLD.foreground(AttributedStyle.CYAN)
            )
        }
    }

    fun outputObject(obj: Any) {
        shell.printStyled(
            GsonBuilder().setPrettyPrinting().create().toJson(obj) + "\n",
            AttributedStyle.BOLD.foreground(AttributedStyle.GREEN)
        )

        if (outputFile != null) {
            val outputFormat: String? = getOptionalParameter(FORMAT_SELECTOR)
            FileOutputWriter().outputObject(obj, outputFile, outputFormat)
        }
    }
}
