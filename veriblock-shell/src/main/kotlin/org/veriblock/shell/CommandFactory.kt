// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.shell

import org.slf4j.LoggerFactory
import org.veriblock.shell.core.ResultMessage
import kotlin.collections.set

private val logger = LoggerFactory.getLogger(CommandFactory::class.java)

const val FILENAME_SELECTOR = "-o"
const val FORMAT_SELECTOR = "-f"

class CommandResult(
    val command: Command,
    val parameters: Map<String, Any>
)

class ShellException(
    vararg val messages: ResultMessage = emptyArray()
) : RuntimeException()

class CommandFactory {
    private val commands: MutableMap<String, Command> = LinkedHashMap()

    fun getInstance(request: String?): CommandResult {
        logger.debug("User Command: {}", request)

        if (request.isNullOrEmpty()) {
            throw malformedCommandError()
        }

        val parts = request.split(" ").dropLastWhile { it.isEmpty() }
        if (parts.isEmpty()) {
            throw malformedCommandError()
        }

        val form = parts[0]
        val command = commands[form]
            ?: throw unknownCommandError(form)

        // Handle selectors in a generic way
        val params = ArrayList<String>()
        val selectors = HashMap<String, String>()
        var i = 1
        while (i < parts.size) {
            val param = parts[i]
            if (param.startsWith("-")) {
                if (i + 1 >= parts.size) {
                    throw selectorError(command, param)
                }
                selectors[param] = parts[i + 1]
                i += 2
                continue
            }
            params += param
            i++
        }

        val parameters = extractParameters(command, params)

        return CommandResult(command, parameters + selectors)
    }

    private fun extractParameters(command: Command, suppliedParams: List<String>): Map<String, Any> {
        if (suppliedParams.size > command.parameters.size) {
            throw syntaxError(command, "too many parameters provided")
        }

        return command.parameters.asSequence().mapIndexedNotNull { index, param ->
            if (index < suppliedParams.size) {
                val suppliedParam = suppliedParams[index]
                param.name to extractTypedParam(param, suppliedParam, command)
            } else {
                if (param.required) {
                    throw syntaxError(
                        command,
                        "parameter '${param.name}' is required"
                    )
                }
                null
            }
        }.associate {
            it
        }
    }

    private fun extractTypedParam(param: CommandParameter, suppliedParam: String, command: Command): Any {
        return with(param) {
            CommandWithParam(command, param).mapper(suppliedParam)
        }
    }

    fun getCommands(): Map<String, Command> = commands

    fun registerCommand(command: Command) {
        val aliases = command.form.split("|")
        for (alias in aliases) {
            commands[alias] = command
        }
    }
}

fun String.asPositiveLong() = toLongOrNull()?.let { if (it >= 0L) it else null }
fun String.asPositiveInteger() = toIntOrNull()?.let { if (it >= 0) it else null }

fun malformedCommandError(): ShellException = ShellException(
    ResultMessage(
        "V005",
        "Malformed command string",
        "The passed command string cannot be parsed.",
        true
    )
)

fun unknownCommandError(form: String) = ShellException(
    ResultMessage(
        "V004",
        "Unknown protocol command",
        "The command '$form' is not supported. Type 'help' to view all commands.",
        true
    )
)

fun syntaxError(command: Command, message: String) = ShellException(
    ResultMessage(
        "V009",
        "Syntax error",
        "Usage: ${command.form}${command.parameters.joinToString("") {
            if (it.required) " <${it.name}>" else " [${it.name}]"
        }}\nERROR: $message",
        true)
)

fun selectorError(command: Command, selector: String) = ShellException(
    ResultMessage(
        "V010",
        "Selector $selector's parameter missing",
        "Usage: ${command.form}${command.parameters.joinToString("") {
            if (it.required) " <${it.name}>" else " [${it.name}]"
        }} $selector [parameter]\nERROR: Selector $selector's parameter missing",
        true)
)
